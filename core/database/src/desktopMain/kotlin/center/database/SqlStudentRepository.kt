package center.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import center.domain.model.EducationStage
import center.domain.model.EducationSystem
import center.domain.model.Student
import center.domain.model.StudentTrack
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
                val discounts = student.enrolledGroupIds.associateWith { student.groupDiscounts[it] ?: 0.0 }
                val existingRows =
                    directory
                        .selectDirectoryStudentById(student.id, ::studentWithGroup)
                        .executeAsList()
                        .mapNotNull { it.second }
                        .associate { it.groupId to (it.discountAmount to (it.customPriceMinor to it.studentTrack)) }
                val requestedRows =
                    student.enrolledGroupIds.associateWith { groupId ->
                        (discounts[groupId] ?: 0.0) to
                            (student.groupCustomPricesMinor[groupId] to (student.groupTracks[groupId] ?: StudentTrack.ARABIC))
                    }
                if (existingRows != requestedRows) {
                    (student.enrolledGroupIds - existingRows.keys).forEach { groupId ->
                        val group =
                            requireNotNull(database.classGroupQueries.selectById(groupId).executeAsOneOrNull()) { "Group not found" }
                        require(group.status == "ACTIVE") { "Cannot enroll in an archived group" }
                    }
                    directory.clearEnrollments(student.id)
                    requestedRows.forEach { (groupId, enrollment) ->
                        directory.insertEnrollment(
                            student.id,
                            groupId,
                            enrollment.first,
                            enrollment.second.first,
                            enrollment.second.second.name,
                        )
                    }
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
    customPriceMinor: Long?,
    studentTrack: String?,
): Pair<Student, EnrollmentRow?> =
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
    ) to groupId?.let { EnrollmentRow(it, checkNotNull(discountAmount), customPriceMinor, studentTrack?.let(StudentTrack::valueOf)) }

private data class EnrollmentRow(
    val groupId: String,
    val discountAmount: Double,
    val customPriceMinor: Long?,
    val studentTrack: StudentTrack?,
)

private fun List<Pair<Student, EnrollmentRow?>>.studentsWithEnrollments(): List<Student> =
    groupBy({ it.first }, { it.second }).map { (student, rows) ->
        val enrollments = rows.filterNotNull().associateBy { it.groupId }
        student.copy(
            enrolledGroupIds = enrollments.keys,
            groupDiscounts = enrollments.mapValues { it.value.discountAmount }.filterValues { it != 0.0 },
            groupCustomPricesMinor = enrollments.mapNotNull { (id, row) -> row.customPriceMinor?.let { id to it } }.toMap(),
            groupTracks =
                enrollments
                    .mapNotNull { (id, row) -> row.studentTrack?.takeIf { it != StudentTrack.ARABIC }?.let { id to it } }
                    .toMap(),
        )
    }
