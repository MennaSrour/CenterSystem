package center.database.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import center.database.CenterDatabase
import center.database.Class_group
import center.database.SelectTeachersWithGroups
import center.database.dao.AttendanceDao
import center.database.dao.MessageQueueDao
import center.database.entity.AttendanceEntity
import center.database.entity.MessageQueueEntity
import center.database.entity.PaymentEntity
import center.domain.model.Attendance
import center.domain.model.AttendanceStatus
import center.domain.model.MessageStatus
import center.domain.model.Payment
import center.domain.model.QueuedMessage
import center.domain.model.ReminderChannel
import center.domain.model.Teacher
import center.domain.repository.AttendanceRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.TeacherRepository
import center.domain.usecase.DuplicateAttendanceException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

class SqlTeacherRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : TeacherRepository {
    override fun observeTeachers(): Flow<List<Teacher>> =
        database.operationsQueries.selectTeachersWithGroups().asFlow().mapToList(dispatcher).map { rows ->
            rows.groupBy { it.id }.values.map { groups ->
                val teacher = groups.first()
                teacher.toDomain(groups.mapNotNull { it.assignedGroupId }.toSet())
            }
        }

    override suspend fun getTeacherForClass(classId: String): Teacher? =
        withContext(dispatcher) {
            database.transactionWithResult {
                val group =
                    database.classGroupQueries.selectById(classId).executeAsOneOrNull()
                        ?: return@transactionWithResult null
                val teacher = database.teacherQueries.selectById(group.teacherId).executeAsOne()
                Teacher(
                    teacher.id,
                    teacher.fullName,
                    teacher.subject,
                    teacher.commissionRate,
                    teacher.phone,
                    teacher.notes,
                    database.operationsQueries
                        .selectAssignedGroups(teacher.id)
                        .executeAsList()
                        .toSet(),
                )
            }
        }

    override suspend fun getCommissionForClass(classId: String): Double? =
        withContext(dispatcher) {
            database.transactionWithResult {
                val group =
                    database.classGroupQueries.selectById(classId).executeAsOneOrNull()
                        ?: return@transactionWithResult null
                group.commissionBasisPoints?.div(10_000.0)
                    ?: database.teacherQueries
                        .selectById(group.teacherId)
                        .executeAsOne()
                        .commissionRate
            }
        }

    override suspend fun save(teacher: Teacher): Unit =
        withContext(dispatcher) {
            require(teacher.id.isNotBlank() && teacher.fullName.isNotBlank() && teacher.subject.isNotBlank())
            require(teacher.commissionRate.isFinite() && teacher.commissionRate in 0.0..1.0)
            database.transaction {
                val teachers = database.teacherQueries
                if (teachers.selectById(teacher.id).executeAsOneOrNull() == null) {
                    teachers.insertRow(teacher.id, teacher.fullName, teacher.subject, teacher.commissionRate)
                } else {
                    teachers.updateRow(teacher.fullName, teacher.subject, teacher.commissionRate, teacher.id)
                }
                database.operationsQueries.setTeacherContact(teacher.phone, teacher.notes, teacher.id)
            }
        }
}

private fun SelectTeachersWithGroups.toDomain(groupIds: Set<String>): Teacher =
    Teacher(id, fullName, subject, commissionRate, phone, notes, groupIds)

class SqlPaymentRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PaymentRepository {
    override fun getPaymentsForStudent(studentId: String): Flow<List<Payment>> =
        database.paymentQueries
            .selectForStudent(studentId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getPaymentsForTeacher(
        teacherId: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Payment>> {
        require(startInclusive < endExclusive)
        return database.paymentQueries
            .selectForTeacher(teacherId, startInclusive, endExclusive)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { it.toDomain() }
            }
    }

    override suspend fun saveWithReceipt(
        payment: Payment,
        receipt: QueuedMessage,
    ): Unit =
        withContext(dispatcher) {
            require(payment.billingMonth?.let { it.dayOfMonth == 1 && it.year in 1..9999 } != false) { "Invalid billing month" }
            val entity =
                PaymentEntity(
                    payment.id,
                    payment.studentId,
                    payment.classId,
                    payment.amountPaid,
                    payment.teacherShare,
                    payment.centerShare,
                    payment.paymentDate,
                )
            val message = receipt.toEntity()
            require(message.status == center.database.entity.MessageStatus.PENDING)
            database.transaction {
                val group = database.requireEligibleGroup(payment.classId, payment.studentId)
                validateMonthlyPayment(group, payment)
                database.paymentQueries.insertRow(
                    entity.id,
                    entity.studentId,
                    entity.classId,
                    entity.amountPaid,
                    entity.teacherShare,
                    entity.centerShare,
                    entity.paymentDate,
                )
                payment.billingMonth?.let { database.operationsQueries.setPaymentMonth(it.toString(), payment.id) }
                database.insertMessage(message)
            }
        }

    private fun validateMonthlyPayment(
        group: Class_group,
        payment: Payment,
    ) {
        val price = group.monthlyPriceMinor ?: return
        val month = requireNotNull(payment.billingMonth) { "Billing month is required for a monthly group" }
        require(month.dayOfMonth == 1)
        val (start, end) = month.bounds(TimeZone.of("Africa/Cairo"))
        val paid = database.operationsQueries.paidForMonth(payment.studentId, payment.classId, month.toString(), start, end).executeAsOne()
        val discount = database.operationsQueries.enrollmentDiscount(payment.studentId, payment.classId).executeAsOne()
        val net = (price - (discount * 100).roundToLong()).coerceAtLeast(0)
        val amount = (payment.amountPaid * 100).roundToLong()
        val commission = checkNotNull(group.commissionBasisPoints)
        require(amount in 1..100_000_000_000L && amount <= net - paid) { "Payment exceeds the remaining subscription balance" }
        val teacherMinor = (amount * commission + 5_000) / 10_000
        require((payment.teacherShare * 100).roundToLong() == teacherMinor) { "Group commission changed before payment" }
    }
}

private fun center.database.Payment.toDomain(): Payment =
    Payment(id, studentId, classId, amountPaid, teacherShare, centerShare, paymentDate, billingMonth?.let(LocalDate::parse))

class SqlAttendanceRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : AttendanceRepository {
    private val dao = AttendanceDao(database, dispatcher)

    override fun observePresentCount(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<Long> {
        require(startInclusive < endExclusive)
        return database.operationsQueries
            .presentToday(startInclusive, endExclusive)
            .asFlow()
            .mapToOne(dispatcher)
    }

    override fun getAttendanceForClass(
        classId: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Attendance>> =
        dao.getAttendanceForClass(classId, startInclusive, endExclusive).map { rows ->
            rows.map {
                Attendance(it.id, it.studentId, it.classId, it.timestamp, AttendanceStatus.valueOf(it.status.name))
            }
        }

    override suspend fun saveWithNotification(
        attendance: Attendance,
        notification: QueuedMessage,
    ): Unit =
        withContext(dispatcher) {
            val entity =
                AttendanceEntity(
                    attendance.id,
                    attendance.studentId,
                    attendance.classId,
                    attendance.timestamp,
                    center.database.entity.AttendanceStatus
                        .valueOf(attendance.status.name),
                )
            val message = notification.toEntity()
            require(message.status == center.database.entity.MessageStatus.PENDING)
            database.transaction {
                database.requireEligibleGroup(attendance.classId, attendance.studentId)
                val zone = TimeZone.of("Africa/Cairo")
                val date = Instant.fromEpochMilliseconds(attendance.timestamp).toLocalDateTime(zone).date
                val (start, end) = date.bounds(zone, DatePeriod(days = 1))
                if (database.operationsQueries.countDailyAttendance(attendance.studentId, attendance.classId, start, end).executeAsOne() >
                    0
                ) {
                    throw DuplicateAttendanceException()
                }
                database.attendanceQueries.insertRow(entity.id, entity.studentId, entity.classId, entity.timestamp, entity.status.name)
                database.insertMessage(message)
            }
        }
}

class SqlMessageQueueRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MessageQueueRepository {
    private val dao = MessageQueueDao(database, dispatcher)

    override fun getPendingMessages(): Flow<List<QueuedMessage>> =
        dao.getPendingMessages().map { rows ->
            rows.map {
                QueuedMessage(
                    it.id,
                    it.targetPhone,
                    it.messageBody,
                    MessageStatus.valueOf(it.status.name),
                    it.createdAt,
                    ReminderChannel.valueOf(it.channel.name),
                )
            }
        }

    override suspend fun save(message: QueuedMessage): Unit =
        withContext(dispatcher) {
            require(message.status == MessageStatus.PENDING)
            database.transaction { database.insertMessage(message.toEntity()) }
        }

    override suspend fun markSent(id: String) = dao.markSent(id)
}

internal fun QueuedMessage.toEntity() =
    MessageQueueEntity(
        id,
        targetPhone,
        messageBody,
        center.database.entity.MessageStatus
            .valueOf(status.name),
        createdAt,
        center.database.entity.ReminderChannel
            .valueOf(channel.name),
    )

internal fun CenterDatabase.insertMessage(message: MessageQueueEntity) {
    messageQueueQueries.insertRow(message.id, message.targetPhone, message.messageBody, message.status.name, message.createdAt)
    operationsQueries.setMessageChannel(message.channel.name, message.id)
}

private fun CenterDatabase.requireEligibleGroup(
    classId: String,
    studentId: String,
): Class_group {
    val group = requireNotNull(classGroupQueries.selectById(classId).executeAsOneOrNull()) { "Group not found" }
    require(group.status == "ACTIVE") { "Group is archived" }
    require(studentQueries.selectById(studentId).executeAsOneOrNull()?.isActive == true) { "Student is missing or inactive" }
    if (group.monthlyPriceMinor != null) {
        require(operationsQueries.hasEnrollment(studentId, classId).executeAsOne()) { "Student is not enrolled in the group" }
    }
    return group
}
