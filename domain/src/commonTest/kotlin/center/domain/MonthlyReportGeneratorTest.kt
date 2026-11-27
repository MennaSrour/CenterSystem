package center.domain

import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LedgerDirection
import center.domain.model.LedgerEntry
import center.domain.model.LedgerKind
import center.domain.model.LessonType
import center.domain.model.MakeupSession
import center.domain.model.PersonType
import center.domain.model.StaffMember
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.usecase.MonthlyReportGenerator
import center.domain.usecase.MonthlyReportInput
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyReportGeneratorTest {
    @Test
    fun aggregatesFinancialsAttendanceAndPeopleForSelectedMonth() {
        val month = LocalDate(2026, 9, 1)
        val group =
            ClassGroup(
                "g1",
                "درس العربي",
                "t1",
                10_000,
                7_000,
                null,
                EducationSystem.ARABIC,
                GroupStatus.ACTIVE,
                listOf(GroupScheduleSlot(DayOfWeek.TUESDAY, LocalTime(13, 0), LocalTime(14, 0))),
                lessonType = LessonType.ARABIC,
            )
        val students =
            listOf(
                Student("s1", "أحمد", gradeLevel = "grade-1", code = "ST-1001", enrolledGroupIds = setOf("g1")),
                Student("s2", "Mariam", code = "ST-1002", enrolledGroupIds = setOf("g1")),
                Student("s3", "Inactive", isActive = false),
            )
        val teacher = Teacher("t1", "سارة", "Arabic")
        val staff = listOf(StaffMember("SF-1001", "Nour", "Reception", "010", monthlySalaryMinor = 20_000))
        val sessions =
            listOf(
                session(PersonType.STUDENT, "s1", month, 8, 9),
                session(PersonType.STUDENT, "s1", month, 9, 10),
                session(PersonType.STUDENT, "s2", month, 10, 11),
                session(PersonType.TEACHER, "t1", month, 11, 13),
                session(PersonType.TEACHER, "t1", month.plus(DatePeriod(days = 1)), 11, 12),
                session(PersonType.STAFF, "SF-1001", month, 8, 12),
            )
        val ledger =
            listOf(
                ledger("collection", LedgerDirection.INCOME, LedgerKind.STUDENT_PAYMENT, 14_000),
                ledger("expense", LedgerDirection.EXPENSE, LedgerKind.OPERATIONAL, 3_000),
                ledger("payout", LedgerDirection.EXPENSE, LedgerKind.TEACHER_PAYOUT, 7_000, teacherId = "t1"),
            )

        val report =
            MonthlyReportGenerator().generate(
                MonthlyReportInput(
                    month,
                    students,
                    listOf(teacher),
                    staff,
                    listOf(group),
                    ledger,
                    sessions,
                    monthlyDueMinorByStudent = mapOf("s1" to 10_000, "s2" to 4_000),
                    remainingMinorByStudent = mapOf("s1" to 2_000),
                    expectedTeacherSalaryMinorById = mapOf("t1" to 7_500),
                    makeupSessions =
                        listOf(
                            MakeupSession(
                                "makeup-1",
                                group.id,
                                LocalDate(2026, 9, 1),
                                LocalDate(2026, 9, 8),
                                LocalTime(15, 0),
                                LocalTime(16, 0),
                                createdAt = 1,
                            ),
                        ),
                ),
            )

        assertEquals(14_000, report.treasury.collectionsMinor)
        assertEquals(3_000, report.treasury.expensesMinor)
        assertEquals(7_000, report.treasury.teacherPayoutsMinor)
        assertEquals(20_000, report.treasury.staffSalariesMinor)
        assertEquals(-16_000, report.treasury.netProfitMinor)
        assertEquals(2_000, report.treasury.overdueMinor)
        assertEquals(2, report.studentsSummary.activeCount)
        assertEquals(3, report.studentsSummary.attendanceCount)
        assertEquals(1, report.studentsSummary.paidCount)
        assertEquals(1, report.studentsSummary.unpaidCount)
        assertEquals(5, report.studentsSummary.scheduledSessionCount)
        assertEquals(1, report.studentsSummary.makeupSessionCount)
        assertEquals(1, report.studentsSummary.rescheduledDateCount)
        assertEquals(1, report.teachers.single().lessonCount)
        assertEquals(2, report.teachers.single().attendanceDays)
        assertEquals(180, report.teachers.single().workedMinutes)
        assertEquals(7_500, report.teachers.single().expectedSalaryMinor)
        assertEquals(7_000, report.teachers.single().paidMinor)
        assertEquals(8_000, report.students.first { it.id == "s1" }.paidMinor)
        assertEquals(20_000, report.staff.single().monthlySalaryMinor)
        assertEquals(240, report.staff.single().workedMinutes)
    }

    private fun session(
        type: PersonType,
        id: String,
        date: LocalDate,
        checkInHour: Int,
        checkOutHour: Int,
    ) = AttendanceSession(
        AttendancePerson(type, id, if (type == PersonType.STUDENT) "g1" else ""),
        date,
        date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() + checkInHour * 3_600_000L,
        date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() + checkOutHour * 3_600_000L,
    )

    private fun ledger(
        id: String,
        direction: LedgerDirection,
        kind: LedgerKind,
        amount: Long,
        teacherId: String? = null,
    ) = LedgerEntry(id, direction, kind, amount, 0, "", teacherId = teacherId)
}
