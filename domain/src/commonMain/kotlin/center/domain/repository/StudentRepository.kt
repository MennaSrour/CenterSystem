package center.domain.repository

import center.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>

    suspend fun getStudentByCode(code: String): Student?

    suspend fun getStudentById(id: String): Student? = students().firstOrNull { it.id == id }

    suspend fun save(student: Student)

    suspend fun delete(id: String): Unit = throw UnsupportedOperationException("Student deletion is unavailable")

    suspend fun students(): List<Student>
}
