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
            require(
                student.groupDiscounts.filterKeys { it in student.enrolledGroupIds }.values.all {
                    it.isFinite() && it in 0.0..1_000_000_000.0 && kotlin.math.abs(it * 100 - kotlin.math.round(it * 100)) < 0.000001
                },
            ) { "Invalid enrollment discount" }
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
                val existingDiscounts =
                    directory
                        .selectStudentByCode(student.code.trim())
                        .executeAsList()
                        .mapNotNull { row -> row.enrolledGroupId?.let { it to checkNotNull(row.discountAmount) } }
                        .toMap()
                val discounts = student.enrolledGroupIds.associateWith { student.groupDiscounts[it] ?: 0.0 }
                if (existingDiscounts != discounts) {
                    (student.enrolledGroupIds - existingDiscounts.keys).forEach { groupId ->
                        val group =
                            requireNotNull(database.classGroupQueries.selectById(groupId).executeAsOneOrNull()) { "Group not found" }
                        require(group.status == "ACTIVE") { "Cannot enroll in an archived group" }
                    }
                    directory.clearEnrollments(student.id)
                    discounts.forEach { (groupId, discount) -> directory.insertEnrollment(student.id, groupId, discount) }
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
    discountAmount: Double?,
): Pair<Student, Pair<String, Double>?> =
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
    ) to groupId?.let { it to checkNotNull(discountAmount) }

private fun List<Pair<Student, Pair<String, Double>?>>.studentsWithEnrollments(): List<Student> =
    groupBy({ it.first }, { it.second }).map { (student, groups) ->
        val discounts = groups.filterNotNull().toMap()
        student.copy(enrolledGroupIds = discounts.keys, groupDiscounts = discounts.filterValues { it != 0.0 })
    }
