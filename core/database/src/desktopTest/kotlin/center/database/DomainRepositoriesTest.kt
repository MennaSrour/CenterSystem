package center.database

import center.database.entity.ClassGroupEntity
import center.database.entity.MessageQueueEntity
import center.database.entity.TeacherEntity
import center.database.repository.SqlAttendanceRepository
import center.database.repository.SqlMessageQueueRepository
import center.database.repository.SqlPaymentRepository
import center.database.repository.SqlTeacherRepository
import center.domain.RegisterStudent
import center.domain.model.AttendanceStatus
import center.domain.model.Student
import center.domain.usecase.EpochClock
import center.domain.usecase.GetTeacherMonthlyDuesUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.RecordAttendanceUseCase
import center.domain.usecase.SyncPendingMessagesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.nio.file.Files
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DomainRepositoriesTest {
    @Test
    fun processesPaymentAndAttendanceIntoLocalOutboxAndDomainQueries() =
        runTest {
            withFixture { fixture ->
                var id = 0
                val ids = IdGenerator { "id-${++id}" }
                val clock = EpochClock { 100L }
                val payment =
                    ProcessPaymentUseCase(fixture.students, fixture.teachers, fixture.payments, clock, ids)
                        .invoke("student", "class", 100.01)
                        .getOrThrow()
                assertEquals(70.01, payment.teacherShare)
                assertEquals(30.0, payment.centerShare)
                RecordAttendanceUseCase(fixture.students, fixture.teachers, fixture.attendance, clock, ids)
                    .invoke("student", "class", AttendanceStatus.PRESENT)
                    .getOrThrow()
                val pending = SyncPendingMessagesUseCase(fixture.messages)().getOrThrow()
                assertEquals(2, pending.size)
                assertEquals(
                    1,
                    fixture.attendance
                        .getAttendanceForClass("class", 0L, 101L)
                        .first()
                        .size,
                )
                assertEquals(listOf(payment), fixture.payments.getPaymentsForStudent("student").first())
                assertEquals(70.01, GetTeacherMonthlyDuesUseCase(fixture.payments, TimeZone.UTC)("teacher", 1, 1970).getOrThrow())
                assertEquals(0.0, GetTeacherMonthlyDuesUseCase(fixture.payments, TimeZone.UTC)("other", 1, 1970).getOrThrow())
                fixture.messages.markSent(pending.first().id)
                assertEquals(1, SyncPendingMessagesUseCase(fixture.messages)().getOrThrow().size)
            }
        }

    @Test
    fun queueInsertFailureRollsBackPaymentAndAttendance() =
        runTest {
            withFixture { fixture ->
                fixture.daos.messages.save(MessageQueueEntity("duplicate", "01012345678", "Existing", createdAt = 0L))
                val values = listOf("payment", "duplicate", "attendance", "duplicate").iterator()
                val ids = IdGenerator { values.next() }
                val clock = EpochClock { 100L }
                val payment =
                    ProcessPaymentUseCase(fixture.students, fixture.teachers, fixture.payments, clock, ids)
                        .invoke("student", "class", 100.0)
                assertIs<SQLException>(payment.exceptionOrNull())
                val attendance =
                    RecordAttendanceUseCase(fixture.students, fixture.teachers, fixture.attendance, clock, ids)
                        .invoke("student", "class", AttendanceStatus.PRESENT)
                assertIs<SQLException>(attendance.exceptionOrNull())
                assertTrue(
                    fixture.daos.payments
                        .observeAll()
                        .first()
                        .isEmpty(),
                )
                assertTrue(
                    fixture.daos.attendance
                        .observeAll()
                        .first()
                        .isEmpty(),
                )
                assertEquals(
                    listOf("duplicate"),
                    fixture.daos.messages
                        .getPendingMessages()
                        .first()
                        .map { it.id },
                )
            }
        }

    @Test
    fun duplicatePaymentIdDoesNotOverwriteReceiptOrPayment() =
        runTest {
            withFixture { fixture ->
                val values = listOf("payment", "first-receipt", "payment", "second-receipt").iterator()
                val process =
                    ProcessPaymentUseCase(
                        fixture.students,
                        fixture.teachers,
                        fixture.payments,
                        EpochClock { 100L },
                        IdGenerator { values.next() },
                    )
                val first = process("student", "class", 100.0).getOrThrow()
                assertIs<SQLException>(process("student", "class", 200.0).exceptionOrNull())
                assertEquals(listOf(first), fixture.payments.getPaymentsForStudent("student").first())
                assertEquals(
                    listOf("first-receipt"),
                    fixture.daos.messages
                        .getPendingMessages()
                        .first()
                        .map { it.id },
                )
            }
        }

    @Test
    fun registrationNameUpdatePreservesStudentContactAndGrade() =
        runTest {
            withFixture { fixture ->
                RegisterStudent(fixture.students)("student", "Updated Name")
                val student = requireNotNull(fixture.students.getStudentByCode("student"))
                assertEquals("Updated Name", student.fullName)
                assertEquals("Grade 10", student.gradeLevel)
                assertEquals("01012345678", student.parentPhone)
                assertEquals(student, fixture.students.students().single())
            }
        }

    private class Fixture(
        val database: CenterDatabase,
        scope: TestScope,
    ) {
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val daos = CenterDaos(database, dispatcher)
        val students = SqlStudentRepository(database, dispatcher)
        val teachers = SqlTeacherRepository(database, dispatcher)
        val payments = SqlPaymentRepository(database, dispatcher)
        val attendance = SqlAttendanceRepository(database, dispatcher)
        val messages = SqlMessageQueueRepository(database, dispatcher)
    }

    private suspend fun TestScope.withFixture(block: suspend (Fixture) -> Unit) {
        val directory = Files.createTempDirectory("center-domain")
        try {
            openDatabase(directory).use { driver ->
                val fixture = Fixture(CenterDatabase(driver), this)
                fixture.daos.teachers.save(TeacherEntity("teacher", "Teacher", "Math"))
                fixture.daos.classGroups.save(ClassGroupEntity("class", "Class", "teacher", 100.0))
                fixture.students.save(Student("student", "منة", "Grade 10", parentPhone = "01012345678"))
                block(fixture)
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
