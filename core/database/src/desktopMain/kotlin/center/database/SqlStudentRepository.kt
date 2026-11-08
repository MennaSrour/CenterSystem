package center.database

import center.domain.Student
import center.domain.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlStudentRepository(
    private val database: CenterDatabase,
) : StudentRepository {
    override suspend fun save(student: Student): Unit =
        withContext(Dispatchers.IO) {
            database.studentQueries.upsert(student.id, student.name)
        }

    override suspend fun students(): List<Student> =
        withContext(Dispatchers.IO) {
            database.studentQueries
                .selectAll()
                .executeAsList()
                .map { Student(it.id, it.name) }
        }
}
