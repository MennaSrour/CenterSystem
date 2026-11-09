package center.database

import center.database.dao.StudentDao
import center.database.entity.StudentEntity
import center.domain.model.Student
import center.domain.repository.StudentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SqlStudentRepository(
    database: CenterDatabase,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StudentRepository {
    private val dao = StudentDao(database, dispatcher)

    override fun observeStudents(): Flow<List<Student>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getStudentByCode(code: String): Student? = dao.observeById(code).first()?.toDomain()

    override suspend fun save(student: Student) =
        dao.save(
            StudentEntity(student.id, student.fullName, student.gradeLevel, student.studentPhone, student.parentPhone, student.isActive),
        )

    override suspend fun students(): List<Student> = observeStudents().first()
}

private fun StudentEntity.toDomain() = Student(id, fullName, gradeLevel, studentPhone, parentPhone, isActive)
