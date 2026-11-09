package center.domain.usecase

import center.domain.model.Attendance
import center.domain.model.AttendanceStatus
import center.domain.model.MessageStatus
import center.domain.model.Payment
import center.domain.model.QueuedMessage
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.repository.AttendanceRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UseCasesTest {
    @Test
    fun paymentAllocatesDefaultCommissionAndQueuesArabicReceipt() =
        runTest {
            val fixture = Fixture()
            val payment = fixture.process(" student ", "class", 100.0).getOrThrow()
            assertEquals(70.0, payment.teacherShare)
            assertEquals(30.0, payment.centerShare)
            assertEquals(fixture.now, payment.paymentDate)
            assertEquals(listOf(payment), fixture.payments)
            val receipt = fixture.messages.single()
            assertEquals("01012345678", receipt.targetPhone)
            assertEquals(MessageStatus.PENDING, receipt.status)
            assertEquals(fixture.now, receipt.createdAt)
            assertTrue(receipt.messageBody.contains("100.00"))
            assertTrue(receipt.messageBody.contains("منة"))
            assertTrue(receipt.messageBody.contains(payment.id))
        }

    @Test
    fun customCommissionRoundsToCentsAndKeepsExactRemainder() =
        runTest {
            val fixture = Fixture()
            fixture.teacher = fixture.teacher?.copy(commissionRate = 0.8)
            val payment = fixture.process("student", "class", 100.01).getOrThrow()
            assertEquals(80.01, payment.teacherShare)
            assertEquals(20.0, payment.centerShare)
            fixture.teacher = fixture.teacher?.copy(commissionRate = 0.0)
            assertEquals(0.0, fixture.process("student", "class", 1.0).getOrThrow().teacherShare)
            fixture.teacher = fixture.teacher?.copy(commissionRate = 1.0)
            assertEquals(0.0, fixture.process("student", "class", 1.0).getOrThrow().centerShare)
        }

    @Test
    fun invalidAmountsFailWithoutWrites() =
        runTest {
            val fixture = Fixture()
            listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 0.001, 1_000_000_001.0).forEach { amount ->
                assertTrue(fixture.process("student", "class", amount).isFailure)
            }
            assertTrue(fixture.payments.isEmpty())
            assertTrue(fixture.messages.isEmpty())
        }

    @Test
    fun missingAndInvalidBusinessDataFailWithoutWrites() =
        runTest {
            val fixture = Fixture()
            assertIs<StudentNotFoundException>(fixture.process("unknown", "class", 100.0).exceptionOrNull())
            assertIs<ClassTeacherNotFoundException>(fixture.process("student", "unknown", 100.0).exceptionOrNull())
            fixture.teacher = fixture.teacher?.copy(commissionRate = Double.NaN)
            assertTrue(fixture.process("student", "class", 100.0).isFailure)
            fixture.student = fixture.student.copy(parentPhone = "")
            assertTrue(fixture.record("student", "class", AttendanceStatus.PRESENT).isFailure)
            fixture.student = fixture.student.copy(parentPhone = "01012345678", isActive = false)
            assertTrue(fixture.process("student", "class", 100.0).isFailure)
            assertTrue(fixture.payments.isEmpty())
            assertTrue(fixture.attendance.isEmpty())
            assertTrue(fixture.messages.isEmpty())
        }

    @Test
    fun attendanceQueuesAccurateArabicForBothStatuses() =
        runTest {
            val fixture = Fixture()
            val present = fixture.record("student", "class", AttendanceStatus.PRESENT).getOrThrow()
            assertEquals(AttendanceStatus.PRESENT, present.status)
            assertEquals(fixture.now, present.timestamp)
            assertEquals("تم تسجيل حضور الطالب منة لسنتر Center One", fixture.messages.single().messageBody)
            fixture.record("student", "class", AttendanceStatus.ABSENT).getOrThrow()
            assertEquals("تم تسجيل غياب الطالب منة لسنتر Center One", fixture.messages.last().messageBody)
            assertTrue(fixture.messages.all { it.status == MessageStatus.PENDING })
        }

    @Test
    fun storageFailureIsReturnedWithoutPartialFakeWrites() =
        runTest {
            val fixture = Fixture()
            val failure = IllegalStateException("Storage unavailable")
            fixture.writeFailure = failure
            assertSame(failure, fixture.process("student", "class", 100.0).exceptionOrNull())
            assertSame(failure, fixture.record("student", "class", AttendanceStatus.PRESENT).exceptionOrNull())
            assertTrue(fixture.payments.isEmpty())
            assertTrue(fixture.attendance.isEmpty())
            assertTrue(fixture.messages.isEmpty())
        }

    @Test
    fun monthlyDuesRespectLeapMonthTimezoneAndExclusiveEnd() =
        runTest {
            val fixture = Fixture()
            val zone = TimeZone.of("Africa/Cairo")
            val date = LocalDate(2024, 2, 1)
            val start = date.atStartOfDayIn(zone).toEpochMilliseconds()
            val end = date.plus(DatePeriod(months = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
            listOf(start - 1, start, end - 1, end).forEachIndexed { index, time ->
                fixture.payments.add(Payment("$index", "student", "class", 100.0, 70.0, 30.0, time))
            }
            val dues = GetTeacherMonthlyDuesUseCase(fixture, zone)
            assertEquals(140.0, dues("teacher", 2, 2024).getOrThrow())
            assertEquals(start to end, fixture.requestedRange)
            assertEquals(0.0, dues("other", 2, 2024).getOrThrow())
            assertTrue(dues("teacher", 13, 2024).isFailure)
            assertTrue(dues("teacher", 2, 0).isFailure)
        }

    @Test
    fun decemberDuesUseNextYearBoundary() =
        runTest {
            val fixture = Fixture()
            val zone = TimeZone.UTC
            GetTeacherMonthlyDuesUseCase(fixture, zone)("teacher", 12, 2024).getOrThrow()
            assertEquals(
                LocalDate(2024, 12, 1).atStartOfDayIn(zone).toEpochMilliseconds() to
                    LocalDate(2025, 1, 1).atStartOfDayIn(zone).toEpochMilliseconds(),
                fixture.requestedRange,
            )
        }

    @Test
    fun pendingSnapshotDoesNotSendOrAcknowledgeMessages() =
        runTest {
            val fixture = Fixture()
            val pending = QueuedMessage("pending", "01012345678", "رسالة", createdAt = fixture.now)
            fixture.messages.add(pending)
            fixture.messages.add(pending.copy(id = "sent", status = MessageStatus.SENT))
            assertEquals(listOf(pending), SyncPendingMessagesUseCase(fixture)().getOrThrow())
            assertEquals(0, fixture.sentAcknowledgements)
            assertEquals(2, fixture.messages.size)
        }

    @Test
    fun cancellationIsRethrownByEveryUseCase() =
        runTest {
            val fixture = Fixture()
            fixture.readFailure = CancellationException("Cancelled")
            assertFailsWith<CancellationException> { fixture.process("student", "class", 100.0) }
            assertFailsWith<CancellationException> { fixture.record("student", "class", AttendanceStatus.PRESENT) }
            assertFailsWith<CancellationException> { GetTeacherMonthlyDuesUseCase(fixture)("teacher", 1, 2024) }
            assertFailsWith<CancellationException> { SyncPendingMessagesUseCase(fixture)() }
        }

    private class Fixture :
        StudentRepository,
        TeacherRepository,
        PaymentRepository,
        AttendanceRepository,
        MessageQueueRepository {
        var student = Student("student", "منة", parentPhone = "01012345678")
        var teacher: Teacher? = Teacher("teacher", "Teacher", "Math")
        val payments = mutableListOf<Payment>()
        val attendance = mutableListOf<Attendance>()
        val messages = mutableListOf<QueuedMessage>()
        var writeFailure: Exception? = null
        var readFailure: Exception? = null
        var requestedRange: Pair<Long, Long>? = null
        var sentAcknowledgements = 0
        val now = 1_700_000_000_000L
        private var sequence = 0
        private val clock = EpochClock { now }
        private val ids = IdGenerator { "event-${++sequence}" }
        val process = ProcessPaymentUseCase(this, this, this, clock, ids)
        val record = RecordAttendanceUseCase(this, this, this, clock, ids)

        override fun observeStudents() = flowOf(listOf(student))

        override suspend fun getStudentByCode(code: String): Student? {
            readFailure?.let { throw it }
            return student.takeIf { it.code == code }
        }

        override suspend fun save(student: Student) {
            this.student = student
        }

        override suspend fun students() = listOf(student)

        override fun observeTeachers() = flowOf(listOfNotNull(teacher))

        override suspend fun getTeacherForClass(classId: String) = teacher.takeIf { classId == "class" }

        override suspend fun save(teacher: Teacher) {
            this.teacher = teacher
        }

        override fun getPaymentsForStudent(studentId: String) = flowOf(payments.filter { it.studentId == studentId })

        override fun getPaymentsForTeacher(
            teacherId: String,
            startInclusive: Long,
            endExclusive: Long,
        ): kotlinx.coroutines.flow.Flow<List<Payment>> {
            readFailure?.let { throw it }
            requestedRange = startInclusive to endExclusive
            return flowOf(payments.filter { teacherId == teacher?.id && it.paymentDate >= startInclusive && it.paymentDate < endExclusive })
        }

        override suspend fun saveWithReceipt(
            payment: Payment,
            receipt: QueuedMessage,
        ) {
            writeFailure?.let { throw it }
            payments.add(payment)
            messages.add(receipt)
        }

        override fun getAttendanceForClass(
            classId: String,
            startInclusive: Long,
            endExclusive: Long,
        ) = flowOf(attendance.filter { it.classId == classId && it.timestamp >= startInclusive && it.timestamp < endExclusive })

        override suspend fun saveWithNotification(
            attendance: Attendance,
            notification: QueuedMessage,
        ) {
            writeFailure?.let { throw it }
            this.attendance.add(attendance)
            messages.add(notification)
        }

        override fun getPendingMessages(): kotlinx.coroutines.flow.Flow<List<QueuedMessage>> {
            readFailure?.let { throw it }
            return flowOf(messages.filter { it.status == MessageStatus.PENDING })
        }

        override suspend fun save(message: QueuedMessage) {
            messages.add(message)
        }

        override suspend fun markSent(id: String) {
            sentAcknowledgements++
        }
    }
}
