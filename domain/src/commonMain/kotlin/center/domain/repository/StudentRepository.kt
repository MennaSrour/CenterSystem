package center.domain.repository

import center.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>

    suspend fun getStudentByCode(code: String): Student?

    suspend fun save(student: Student)

    suspend fun students(): List<Student>
}
