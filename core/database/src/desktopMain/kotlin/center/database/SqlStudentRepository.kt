package center.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import center.domain.model.EducationStage
import center.domain.model.EducationSystem
import center.domain.model.Student
import center.domain.repository.StudentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlStudentRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StudentRepository {
    private val directory = database.operationsQueries

    override fun observeStudents(): Flow<List<Student>> =
        directory
            .selectDirectory(::studentWithGroup)
            .asFlow()
            .mapToList(dispatcher)
            .map { it.studentsWithEnrollments() }

    override suspend fun getStudentByCode(code: String): Student? =
        withContext(dispatcher) {
            directory
                .selectStudentByCode(code.trim(), ::studentWithGroup)
                .executeAsList()
                .studentsWithEnrollments()
                .singleOrNull()
        }

    override suspend fun getStudentById(id: String): Student? =
        withContext(dispatcher) {
            directory
                .selectDirectoryStudentById(id, ::studentWithGroup)
                .executeAsList()
                .studentsWithEnrollments()
                .singleOrNull()
        }

    override suspend fun save(student: Student): Unit =
        withContext(dispatcher) {
            require(student.id.isNotBlank() && student.fullName.isNotBlank() && student.code.isNotBlank())
            require(student.system != EducationSystem.BOTH) { "Students must use one education system" }
            database.transaction {
                val students = database.studentQueries
                val existing = students.selectById(student.id).executeAsOneOrNull()
                if (existing == null) {
                    directory.insertDirectoryStudent(
                        student.id,
                        student.fullName,
                        student.gradeLevel,
                        student.studentPhone,
                        student.parentPhone,
                        student.isActive,
                        student.code.trim(),
                        student.stage?.name,
                        student.system?.name,
                    )
                } else {
                    students.updateRow(
                        student.fullName,
                        student.gradeLevel,
                        student.studentPhone,
                        student.parentPhone,
                        student.isActive,
                        student.id,
                    )
                    directory.updateStudentDetails(student.code.trim(), student.stage?.name, student.system?.name, student.id)
                }
                val existingGroups =
                    directory
                        .selectStudentByCode(student.code.trim())
                        .executeAsList()
                        .mapNotNull { it.enrolledGroupId }
                        .toSet()
                if (existingGroups != student.enrolledGroupIds) {
                    (student.enrolledGroupIds - existingGroups).forEach { groupId ->
                        val group =
                            requireNotNull(database.classGroupQueries.selectById(groupId).executeAsOneOrNull()) { "Group not found" }
                        require(group.status == "ACTIVE") { "Cannot enroll in an archived group" }
                    }
                    directory.clearEnrollments(student.id)
                    student.enrolledGroupIds.forEach { directory.insertEnrollment(student.id, it) }
                }
            }
        }

    override suspend fun delete(id: String): Unit =
        withContext(dispatcher) {
            database.transaction {
                directory.clearEnrollments(id)
                database.studentQueries.deleteById(id)
            }
        }

    override suspend fun students(): List<Student> = observeStudents().first()
}

private fun studentWithGroup(
    id: String,
    name: String,
    grade: String,
    studentPhone: String?,
    parentPhone: String,
    active: Boolean,
    code: String?,
    stage: String?,
    system: String?,
    groupId: String?,
): Pair<Student, String?> =
    Student(
        id,
        name,
        grade,
        studentPhone,
        parentPhone,
        active,
        code ?: id,
        stage?.let(EducationStage::valueOf),
        system?.let(EducationSystem::valueOf),
    ) to groupId

private fun List<Pair<Student, String?>>.studentsWithEnrollments(): List<Student> =
    groupBy({ it.first }, { it.second }).map { (student, groups) -> student.copy(enrolledGroupIds = groups.filterNotNull().toSet()) }
