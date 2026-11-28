package center.domain.usecase

import center.domain.model.AttendanceSession
import center.domain.model.ClassGroup
import center.domain.model.GroupStatus
import center.domain.model.LedgerDirection
import center.domain.model.LedgerEntry
import center.domain.model.LedgerKind
import center.domain.model.MakeupSession
import center.domain.model.PersonType
import center.domain.model.StaffMember
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.model.lessonAnalytics
import kotlinx.datetime.LocalDate

data class MonthlyReportInput(
    val month: LocalDate,
    val students: List<Student>,
    val teachers: List<Teacher>,
    val staff: List<StaffMember>,
    val groups: List<ClassGroup>,
    val ledger: List<LedgerEntry>,
    val sessions: List<AttendanceSession>,
    val monthlyDueMinorByStudent: Map<String, Long>,
    val remainingMinorByStudent: Map<String, Long>,
    val expectedTeacherSalaryMinorById: Map<String, Long>,
    val makeupSessions: List<MakeupSession> = emptyList(),
)

data class MonthlyCenterReport(
    val month: LocalDate,
    val treasury: ReportTreasurySummary,
    val studentsSummary: ReportStudentsSummary,
    val teachers: List<ReportTeacherRow>,
    val students: List<ReportStudentRow>,
    val staff: List<ReportStaffRow>,
)

data class ReportTreasurySummary(
    val collectionsMinor: Long,
    val expensesMinor: Long,
    val teacherPayoutsMinor: Long,
    val staffSalariesMinor: Long,
    val netProfitMinor: Long,
    val overdueMinor: Long,
)

data class ReportStudentsSummary(
    val activeCount: Int,
    val attendanceCount: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val scheduledSessionCount: Int = 0,
    val makeupSessionCount: Int = 0,
    val rescheduledDateCount: Int = 0,
)

data class ReportTeacherRow(
    val id: String,
    val name: String,
    val lessonCount: Int,
    val arabicStudentCount: Int,
    val languagesStudentCount: Int,
    val privateStudentCount: Int,
    val attendanceDays: Int,
    val workedMinutes: Long,
    val expectedSalaryMinor: Long,
    val paidMinor: Long,
)

data class ReportStudentRow(
    val id: String,
    val code: String,
    val name: String,
    val grade: String,
    val lessonCount: Int,
    val monthlyDueMinor: Long,
    val paidMinor: Long,
    val remainingMinor: Long,
)

data class ReportStaffRow(
    val id: String,
    val name: String,
    val role: String,
    val attendanceDays: Int,
    val workedMinutes: Long,
    val monthlySalaryMinor: Long,
)

/** Builds a month snapshot using minor-unit values and only the ledger/session rows in that month. */
class MonthlyReportGenerator {
    fun generate(input: MonthlyReportInput): MonthlyCenterReport {
        require(input.month.dayOfMonth == 1) { "Report month must be the first calendar day" }
        val activeStudents = input.students.filter { it.isActive }.sortedBy { it.fullName.lowercase() }
        val activeGroups = input.groups.filter { it.status == GroupStatus.ACTIVE }
        val studentDue = input.monthlyDueMinorByStudent
        val studentRemaining = input.remainingMinorByStudent
        val studentRows =
            activeStudents.map { student ->
                val due = studentDue[student.id] ?: 0L
                val remaining = (studentRemaining[student.id] ?: 0L).coerceIn(0L, due)
                ReportStudentRow(
                    student.id,
                    student.code,
                    student.fullName,
                    student.gradeLevel,
                    student.enrolledGroupIds.count { id -> activeGroups.any { it.id == id } },
                    due,
                    (due - remaining).coerceAtLeast(0),
                    remaining,
                )
            }

        val teacherPayouts =
            input.ledger
                .filter { it.kind == LedgerKind.TEACHER_PAYOUT && it.direction == LedgerDirection.EXPENSE }
                .groupBy { it.teacherId.orEmpty() }
                .mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }
        val teacherRows =
            input.teachers.sortedBy { it.fullName.lowercase() }.map { teacher ->
                val analytics = teacher.lessonAnalytics(activeGroups, activeStudents)
                val sessions = input.sessions.forPerson(PersonType.TEACHER, teacher.id)
                ReportTeacherRow(
                    teacher.id,
                    teacher.fullName,
                    analytics.assignedLessonCount,
                    analytics.arabicStudentCount,
                    analytics.languagesStudentCount,
                    analytics.privateStudentCount,
                    sessions.map { it.date }.distinct().size,
                    sessions.sumWorkedMinutes(),
                    input.expectedTeacherSalaryMinorById[teacher.id] ?: 0L,
                    teacherPayouts[teacher.id] ?: 0L,
                )
            }

        val staffRows =
            input.staff.filter { it.isActive }.sortedBy { it.name.lowercase() }.map { member ->
                val sessions = input.sessions.forPerson(PersonType.STAFF, member.id)
                ReportStaffRow(
                    member.id,
                    member.name,
                    member.role,
                    sessions.map { it.date }.distinct().size,
                    sessions.sumWorkedMinutes(),
                    member.monthlySalaryMinor ?: 0L,
                )
            }

        val collections =
            input.ledger
                .filter {
                    !it.isVoided && it.kind == LedgerKind.STUDENT_PAYMENT && it.direction == LedgerDirection.INCOME
                }.sumOf { it.amountMinor }
        val expenses =
            input.ledger
                .filter {
                    it.kind == LedgerKind.OPERATIONAL && it.direction == LedgerDirection.EXPENSE
                }.sumOf { it.amountMinor }
        val payouts = teacherPayouts.values.sum()
        // Include all active payroll records, even if a staff member has no attendance that month.
        val staffSalaries = input.staff.filter { it.isActive }.sumOf { it.monthlySalaryMinor ?: 0L }
        val overdue = studentRows.sumOf { it.remainingMinor }
        val dueRows = studentRows.filter { it.monthlyDueMinor > 0 }
        val studentsSummary =
            monthlyLessonCounts(activeGroups, input.month, input.makeupSessions).let { lessonCounts ->
                ReportStudentsSummary(
                    activeStudents.size,
                    input.sessions.count { it.person.type == PersonType.STUDENT },
                    dueRows.count { it.remainingMinor == 0L },
                    dueRows.count { it.remainingMinor > 0L },
                    lessonCounts.scheduledSessionCount,
                    lessonCounts.makeupSessionCount,
                    lessonCounts.rescheduledDateCount,
                )
            }

        // The operational-expense total excludes teacher payouts, which have their own line item.
        val treasury =
            ReportTreasurySummary(collections, expenses, payouts, staffSalaries, collections - expenses - payouts - staffSalaries, overdue)
        return MonthlyCenterReport(input.month, treasury, studentsSummary, teacherRows, studentRows, staffRows)
    }

    private fun List<AttendanceSession>.forPerson(
        type: PersonType,
        id: String,
    ) = filter { it.person.type == type && it.person.id == id }

    private fun List<AttendanceSession>.sumWorkedMinutes(): Long =
        sumOf { session ->
            (session.checkOutTime?.minus(session.checkInTime) ?: 0L).coerceAtLeast(0L) / 60_000L
        }
}
