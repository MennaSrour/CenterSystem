package center.domain.repository

import center.domain.model.Teacher
import kotlinx.coroutines.flow.Flow

interface TeacherRepository {
    fun observeTeachers(): Flow<List<Teacher>>

    suspend fun getTeacherForClass(classId: String): Teacher?

    suspend fun save(teacher: Teacher)
}
