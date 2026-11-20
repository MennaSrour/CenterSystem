package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import center.database.repository.SqlAttendanceSessionRepository
import center.database.repository.SqlClassGroupRepository
import center.database.repository.SqlMonthlyBillingRepository
import center.database.repository.SqlPaymentRepository
import center.database.repository.SqlStaffRepository
import center.database.repository.SqlTeacherRepository
import center.domain.model.AttendancePerson
import center.domain.model.ClassGroup
import center.domain.model.CollectionDeficit
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.PersonType
import center.domain.model.StaffMember
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.model.activeSchedule
import center.domain.usecase.EpochClock
import center.domain.usecase.GetOverdueStudentsUseCase
import center.domain.usecase.GetTeacherCollectionDeficitUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.RegisterPresenceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleOperationsTest {
    private val day = LocalDate(2026, 11, 5)
    private val start = day.atStartOfDayIn(TimeZone.of("Africa/Cairo")).toEpochMilliseconds()
    private val shift = GroupScheduleSlot(DayOfWeek.THURSDAY, LocalTime(16, 5), LocalTime(18, 10))

    @Test
    fun discountsSetThePaymentLimitAndDeficitWithoutChangingSalaryAndSchedulesFollowActiveGroups() =
        runTest {
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = CenterDatabase.Schema).use { driver ->
                val db = CenterDatabase(driver)
                val dispatcher = StandardTestDispatcher(testScheduler)
                val groups = SqlClassGroupRepository(db, dispatcher)
                val students = SqlStudentRepository(db, dispatcher)
                val teachers = SqlTeacherRepository(db, dispatcher)
                val billing = SqlMonthlyBillingRepository(db, dispatcher)
                var id = 0
                val clock = EpochClock { start }
                val payment =
                    ProcessPaymentUseCase(students, teachers, SqlPaymentRepository(db, dispatcher), clock, IdGenerator { "id-${id++}" })
                val overdue = GetOverdueStudentsUseCase(billing, clock)
                val deficit = GetTeacherCollectionDeficitUseCase(billing)
                val teacher = Teacher("t", "Karim", "Math")
                val group = ClassGroup("g", "Math A", "t", 20_000, 6_000, 1, EducationSystem.BOTH, GroupStatus.ACTIVE, listOf(shift))
                val otherGroup = group.copy(id = "g2", name = "Math B", schedule = listOf(shift.copy(day = DayOfWeek.SUNDAY)))
                val student =
                    Student(
                        "s",
                        "Amira",
                        studentPhone = "01000000000",
                        parentPhone = "01000000001",
                        enrolledGroupIds = setOf("g", "g2"),
                        groupDiscounts = mapOf("g" to 25.25, "g2" to 500.0),
                    )
                teachers.save(teacher)
                groups.save(group)
                groups.save(otherGroup)
                students.save(student)
                assertEquals(student, students.getStudentById("s"))
                assertEquals(listOf(group, otherGroup), student.activeSchedule(groups.observeGroups().first()))
                assertEquals(17_475L, overdue().getOrThrow().single().remainingMinor)
                assertEquals(CollectionDeficit(1, 17_475), deficit("t", LocalDate(2026, 11, 1)).getOrThrow())
                assertEquals(24_000L, billing.monthlySalaryMinor("t"))
                assertTrue(payment("s", "g2", 0.01).isFailure)
                assertTrue(payment("s", "g", 174.76).isFailure)
                payment("s", "g", 100.0).getOrThrow()
                assertEquals(7_475L, overdue().getOrThrow().single().remainingMinor)
                payment("s", "g", 74.75).getOrThrow()
                assertTrue(overdue().getOrThrow().isEmpty())
                students.save(student.copy(enrolledGroupIds = setOf("g2")))
                assertEquals(listOf(otherGroup), students.students().single().activeSchedule(groups.observeGroups().first()))
                groups.archive("g")
                assertEquals(listOf(otherGroup), teacher.activeSchedule(groups.observeGroups().first()))
            }
        }

    @Test
    fun upgradingKeepsLegacyAttendanceStudentPhoneAndEnrollmentAndRecordsExactTimestampsForEachPersonType() =
        runTest {
            val directory = Files.createTempDirectory("center-people-migration")
            try {
                JdbcSqliteDriver("jdbc:sqlite:${directory.resolve("center.db")}").use { driver ->
                    driver.execute(null, "CREATE TABLE student (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
                    CenterDatabase.Schema.migrate(driver, 1, 4)
                    driver.execute(null, "INSERT INTO student(id,name,studentPhone) VALUES ('p','Amira','01100000000')", 0)
                    driver.execute(null, "INSERT INTO teacher(id,fullName,subject) VALUES ('p','Karim','Math')", 0)
                    driver.execute(null, "INSERT INTO class_group(id,name,teacherId,sessionPrice) VALUES ('g','Group','p',10)", 0)
                    driver.execute(null, "INSERT INTO enrollment(studentId,classId) VALUES ('p','g')", 0)
                    driver.execute(null, "INSERT INTO attendance VALUES ('a','p','g',${start - 1000},'PRESENT')", 0)
                    driver.execute(null, "PRAGMA user_version = 4", 0)
                }
                openDatabase(directory).use { driver ->
                    val db = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val students = SqlStudentRepository(db, dispatcher)
                    val staff = SqlStaffRepository(db, dispatcher)
                    val attendance = SqlAttendanceSessionRepository(db, dispatcher)
                    val member = StaffMember("p", "Salma", "Secretary", "01200000000", listOf(shift))
                    staff.save(member)
                    assertEquals(listOf(member), staff.observeStaff().first())
                    assertEquals("01100000000", students.students().single().studentPhone)
                    assertEquals(setOf("g"), students.students().single().enrolledGroupIds)
                    assertEquals(0.0, db.operationsQueries.enrollmentDiscount("p", "g").executeAsOne())
                    val original = attendance.observeHistory(PersonType.STUDENT, "p").first().single()
                    assertEquals(LocalDate(2026, 11, 4), original.date)
                    assertEquals(start - 1000, original.checkInTime)
                    assertNull(original.checkOutTime)

                    val enter = start + (16 * 60 + 5) * 60_000L
                    val exit = start + (18 * 60 + 10) * 60_000L
                    var now = enter
                    val register = RegisterPresenceUseCase(attendance, students, EpochClock { now }, IdGenerator { "notification" })
                    val people = PersonType.entries.map { AttendancePerson(it, "p", if (it == PersonType.STUDENT) "g" else "") }
                    for (person in people) {
                        assertTrue(register.checkOut(person).isFailure)
                        register.checkIn(person).getOrThrow()
                        assertTrue(register.checkIn(person).isFailure)
                    }
                    now = exit
                    for (person in people) {
                        register.checkOut(person).getOrThrow()
                        assertTrue(register.checkOut(person).isFailure)
                        val session = attendance.observeHistory(person.type, person.id).first().first()
                        assertEquals(person, session.person)
                        assertEquals(day, session.date)
                        assertEquals(enter, session.checkInTime)
                        assertEquals(exit, session.checkOutTime)
                    }
                    assertEquals(3, attendance.observeDay(day).first().size)
                    staff.save(member.copy(isActive = false))
                    assertEquals(listOf(member.copy(isActive = false)), staff.observeStaff().first())
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
