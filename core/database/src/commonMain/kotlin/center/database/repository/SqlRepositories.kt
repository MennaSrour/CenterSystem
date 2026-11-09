package center.database.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import center.database.CenterDatabase
import center.database.dao.AttendanceDao
import center.database.dao.MessageQueueDao
import center.database.dao.PaymentDao
import center.database.dao.TeacherDao
import center.database.entity.AttendanceEntity
import center.database.entity.MessageQueueEntity
import center.database.entity.PaymentEntity
import center.database.entity.TeacherEntity
import center.domain.model.Attendance
import center.domain.model.AttendanceStatus
import center.domain.model.MessageStatus
import center.domain.model.Payment
import center.domain.model.QueuedMessage
import center.domain.model.Teacher
import center.domain.repository.AttendanceRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.TeacherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlTeacherRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : TeacherRepository {
    private val dao = TeacherDao(database, dispatcher)

    override fun observeTeachers(): Flow<List<Teacher>> =
        dao.observeAll().map { rows ->
            rows.map { Teacher(it.id, it.fullName, it.subject, it.commissionRate) }
        }

    override suspend fun getTeacherForClass(classId: String): Teacher? =
        withContext(dispatcher) {
            database.transactionWithResult {
                val group = database.classGroupQueries.selectById(classId).executeAsOneOrNull()
                group
                    ?.let { database.teacherQueries.selectById(it.teacherId).executeAsOneOrNull() }
                    ?.let { Teacher(it.id, it.fullName, it.subject, it.commissionRate) }
            }
        }

    override suspend fun save(teacher: Teacher) =
        dao.save(TeacherEntity(teacher.id, teacher.fullName, teacher.subject, teacher.commissionRate))
}

class SqlPaymentRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PaymentRepository {
    private val dao = PaymentDao(database, dispatcher)

    override fun getPaymentsForStudent(studentId: String): Flow<List<Payment>> =
        dao
            .getPaymentsForStudent(studentId)
            .map { rows ->
                rows.map { Payment(it.id, it.studentId, it.classId, it.amountPaid, it.teacherShare, it.centerShare, it.paymentDate) }
            }

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
                rows.map { Payment(it.id, it.studentId, it.classId, it.amountPaid, it.teacherShare, it.centerShare, it.paymentDate) }
            }
    }

    override suspend fun saveWithReceipt(
        payment: Payment,
        receipt: QueuedMessage,
    ): Unit =
        withContext(dispatcher) {
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
                database.paymentQueries.insertRow(
                    entity.id,
                    entity.studentId,
                    entity.classId,
                    entity.amountPaid,
                    entity.teacherShare,
                    entity.centerShare,
                    entity.paymentDate,
                )
                database.insertMessage(message)
            }
        }
}

class SqlAttendanceRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : AttendanceRepository {
    private val dao = AttendanceDao(database, dispatcher)

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
                database.attendanceQueries.insertRow(entity.id, entity.studentId, entity.classId, entity.timestamp, entity.status.name)
                database.insertMessage(message)
            }
        }
}

class SqlMessageQueueRepository(
    database: CenterDatabase,
    dispatcher: CoroutineDispatcher,
) : MessageQueueRepository {
    private val dao = MessageQueueDao(database, dispatcher)

    override fun getPendingMessages(): Flow<List<QueuedMessage>> =
        dao.getPendingMessages().map { rows ->
            rows.map {
                QueuedMessage(it.id, it.targetPhone, it.messageBody, MessageStatus.valueOf(it.status.name), it.createdAt)
            }
        }

    override suspend fun save(message: QueuedMessage) = dao.save(message.toEntity())

    override suspend fun markSent(id: String) = dao.markSent(id)
}

private fun QueuedMessage.toEntity() =
    MessageQueueEntity(
        id,
        targetPhone,
        messageBody,
        center.database.entity.MessageStatus
            .valueOf(status.name),
        createdAt,
    )

private fun CenterDatabase.insertMessage(message: MessageQueueEntity) {
    messageQueueQueries.insertRow(message.id, message.targetPhone, message.messageBody, message.status.name, message.createdAt)
}
