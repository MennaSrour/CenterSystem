package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.PaymentEntity
import center.database.entity.StudentEntity
import center.database.entity.StudentWithPayments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudentDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.studentQueries

    fun observeAll(): Flow<List<StudentEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { StudentEntity(it.id, it.name, it.gradeLevel, it.studentPhone, it.parentPhone, it.isActive) } }

    fun observeById(id: String): Flow<StudentEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.let { StudentEntity(it.id, it.name, it.gradeLevel, it.studentPhone, it.parentPhone, it.isActive) } }

    suspend fun save(entity: StudentEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                if (queries.selectById(entity.id).executeAsOneOrNull() == null) {
                    queries.insertRow(
                        entity.id,
                        entity.fullName,
                        entity.gradeLevel,
                        entity.studentPhone,
                        entity.parentPhone,
                        entity.isActive,
                    )
                } else {
                    queries.updateRow(
                        entity.fullName,
                        entity.gradeLevel,
                        entity.studentPhone,
                        entity.parentPhone,
                        entity.isActive,
                        entity.id,
                    )
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }

    fun getStudentWithPayments(id: String): Flow<StudentWithPayments?> =
        queries.selectWithPayments(id).asFlow().mapToList(dispatcher).map { rows ->
            val first = rows.firstOrNull()
            if (first == null) {
                null
            } else {
                StudentWithPayments(
                    StudentEntity(first.id, first.name, first.gradeLevel, first.studentPhone, first.parentPhone, first.isActive),
                    rows.mapNotNull { row ->
                        row.paymentId?.let { paymentId ->
                            PaymentEntity(
                                paymentId,
                                requireNotNull(row.studentId),
                                requireNotNull(row.classId),
                                requireNotNull(row.amountPaid),
                                requireNotNull(row.teacherShare),
                                requireNotNull(row.centerShare),
                                requireNotNull(row.paymentDate),
                            )
                        }
                    },
                )
            }
        }
}
