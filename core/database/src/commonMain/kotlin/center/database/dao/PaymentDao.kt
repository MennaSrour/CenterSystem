package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.PaymentEntity
import center.database.entity.TeacherDues
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PaymentDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.paymentQueries

    fun observeAll(): Flow<List<PaymentEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { PaymentEntity(it.id, it.studentId, it.classId, it.amountPaid, it.teacherShare, it.centerShare, it.paymentDate) }
            }

    fun observeById(id: String): Flow<PaymentEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row ->
                row?.let { PaymentEntity(it.id, it.studentId, it.classId, it.amountPaid, it.teacherShare, it.centerShare, it.paymentDate) }
            }

    suspend fun save(entity: PaymentEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                if (queries.selectById(entity.id).executeAsOneOrNull() == null) {
                    queries.insertRow(
                        entity.id,
                        entity.studentId,
                        entity.classId,
                        entity.amountPaid,
                        entity.teacherShare,
                        entity.centerShare,
                        entity.paymentDate,
                    )
                } else {
                    queries.updateRow(
                        entity.studentId,
                        entity.classId,
                        entity.amountPaid,
                        entity.teacherShare,
                        entity.centerShare,
                        entity.paymentDate,
                        entity.id,
                    )
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }

    fun getPaymentsForStudent(studentId: String): Flow<List<PaymentEntity>> =
        queries
            .selectForStudent(studentId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { PaymentEntity(it.id, it.studentId, it.classId, it.amountPaid, it.teacherShare, it.centerShare, it.paymentDate) }
            }

    /** Epoch milliseconds, inclusive month start and exclusive next-month start in the caller's timezone. */
    fun getTeacherDuesByMonth(
        teacherId: String,
        monthStartInclusive: Long,
        nextMonthStartExclusive: Long,
    ): Flow<TeacherDues> {
        require(monthStartInclusive < nextMonthStartExclusive)
        return queries
            .teacherDues(teacherId, monthStartInclusive, nextMonthStartExclusive)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.single().let { TeacherDues(teacherId, it.totalPaid, it.teacherShare, it.centerShare) } }
    }
}
