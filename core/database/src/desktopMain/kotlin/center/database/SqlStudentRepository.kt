package center.database

import center.domain.Student
import center.domain.StudentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlStudentRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StudentRepository {
    override suspend fun save(student: Student): Unit =
        withContext(dispatcher) {
            database.transaction {
                val existing = database.studentQueries.selectById(student.id).executeAsOneOrNull()
                if (existing == null) {
                    database.studentQueries.insertRow(student.id, student.name, "", null, "", true)
                } else {
                    database.studentQueries.updateRow(
                        student.name,
                        existing.gradeLevel,
                        existing.studentPhone,
                        existing.parentPhone,
                        existing.isActive,
                        student.id,
                    )
                }
            }
        }

    override suspend fun students(): List<Student> =
        withContext(dispatcher) {
            database.studentQueries
                .selectAll()
                .executeAsList()
                .map { Student(it.id, it.name) }
        }
}
