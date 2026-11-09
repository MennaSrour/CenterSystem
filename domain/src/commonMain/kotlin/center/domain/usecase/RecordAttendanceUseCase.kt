package center.domain.usecase

import center.domain.model.Attendance
import center.domain.model.AttendanceStatus
import center.domain.model.QueuedMessage
import center.domain.repository.AttendanceRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository

class RecordAttendanceUseCase(
    private val students: StudentRepository,
    private val teachers: TeacherRepository,
    private val attendance: AttendanceRepository,
    private val clock: EpochClock,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        studentCode: String,
        classId: String,
        status: AttendanceStatus,
    ): Result<Attendance> =
        domainResult {
            require(studentCode.isNotBlank() && classId.isNotBlank()) { "Student code and class ID are required" }
            val student = students.getStudentByCode(studentCode.trim()) ?: throw StudentNotFoundException(studentCode)
            student.requireNotificationDetails()
            if (teachers.getTeacherForClass(classId) == null) throw ClassTeacherNotFoundException(classId)
            val now = clock.nowMillis()
            val record = Attendance(ids.newId(), student.id, classId, now, status)
            val description =
                when (status) {
                    AttendanceStatus.PRESENT -> "حضور"
                    AttendanceStatus.ABSENT -> "غياب"
                }
            val notification =
                QueuedMessage(
                    ids.newId(),
                    student.parentPhone,
                    "تم تسجيل $description الطالب ${student.fullName} لسنتر Center One",
                    createdAt = now,
                )
            attendance.saveWithNotification(record, notification)
            record
        }
}
