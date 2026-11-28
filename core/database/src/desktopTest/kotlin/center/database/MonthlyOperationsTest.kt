package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import center.database.repository.SqlAttendanceRepository
import center.database.repository.SqlClassGroupRepository
import center.database.repository.SqlMessageQueueRepository
import center.database.repository.SqlMonthlyBillingRepository
import center.database.repository.SqlPaymentRepository
import center.database.repository.SqlTeacherRepository
import center.database.repository.SqlTreasuryRepository
import center.domain.model.AttendanceStatus
import center.domain.model.ClassGroup
import center.domain.model.CollectionDeficit
import center.domain.model.EducationStage
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LedgerDirection
import center.domain.model.LedgerKind
import center.domain.model.LessonEnrollment
import center.domain.model.LessonType
import center.domain.model.ReminderChannel
import center.domain.model.Student
import center.domain.model.StudentTrack
import center.domain.model.Teacher
import center.domain.model.TreasuryExpense
import center.domain.usecase.EpochClock
import center.domain.usecase.GetOverdueStudentsUseCase
import center.domain.usecase.GetTeacherCollectionDeficitUseCase
import center.domain.usecase.GetTeacherMonthlySalaryUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.QueueReminderUseCase
import center.domain.usecase.RecordAttendanceUseCase
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonthlyOperationsTest {
    private val month = LocalDate(2026, 11, 1)
    private val day = LocalDate(2026, 11, 5)
    private val now = day.atStartOfDayIn(TimeZone.of("Africa/Cairo")).toEpochMilliseconds()

    @Test
    fun paymentPersistsWithSqlDelightSyncFieldsAndDoesNotRequireAReceiptPhone() =
        runTest {
            val path = Files.createTempDirectory("center-payment-persist")
            try {
                openDatabase(path).use { driver ->
                    val database = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val teachers = SqlTeacherRepository(database, dispatcher)
                    val students = SqlStudentRepository(database, dispatcher)
                    val groups = SqlClassGroupRepository(database, dispatcher)
                    val payments = SqlPaymentRepository(database, dispatcher)
                    val treasury = SqlTreasuryRepository(database, dispatcher)
                    teachers.save(Teacher("teacher", "Teacher", "Math", commissionRate = 0.7))
                    val student = Student("student", "Student", code = "ST-500", parentPhone = "")
                    students.save(student)
                    groups.save(
                        ClassGroup(
                            id = "lesson",
                            name = "Math",
                            teacherId = "teacher",
                            monthlyPriceMinor = 100_000,
                            commissionBasisPoints = 7_000,
                            roomNumber = 1,
                            system = EducationSystem.ARABIC,
                            status = GroupStatus.ACTIVE,
                            enrollments = mapOf(student.id to LessonEnrollment(100_000, StudentTrack.ARABIC)),
                        ),
                    )
                    // Existing lessons can have a null commission after the column was introduced.
                    driver.execute(null, "UPDATE class_group SET commissionBasisPoints = NULL WHERE id = 'lesson'", 0)
                    var id = 0
                    val process =
                        ProcessPaymentUseCase(
                            students,
                            teachers,
                            payments,
                            EpochClock { now },
                            IdGenerator { "payment-${++id}" },
                        )

                    val payment = process(student.code, "lesson", 500.0, month).getOrThrow()

                    assertEquals(500.0, payment.amountPaid)
                    assertEquals(350.0, payment.teacherShare)
                    assertEquals(150.0, payment.centerShare)
                    assertEquals(listOf(payment), payments.getPaymentsForStudent(student.id).first())
                    val row = database.paymentQueries.selectById(payment.id).executeAsOne()
                    assertEquals(month.toString(), row.billingMonth)
                    assertEquals("PENDING", row.syncStatus)
                    assertTrue(row.updatedAt > 0L)
                    assertEquals(
                        50_000L,
                        treasury
                            .observeLedger(now, now + 1)
                            .first()
                            .single()
                            .amountMinor,
                    )
                    assertEquals(
                        50_000L,
                        SqlMonthlyBillingRepository(database, dispatcher)
                            .remainingBalances(month)
                            .single()
                            .remainingMinor,
                    )
                    assertTrue(SqlMessageQueueRepository(database, dispatcher).getPendingMessages().first().isEmpty())

                    payments.voidPayment(payment.id, now + 1_000L, "Duplicate entry")
                    val voided = payments.getPaymentsForStudent(student.id).first().single()
                    assertEquals(now + 1_000L, voided.voidedAt)
                    assertEquals("Duplicate entry", voided.voidReason)
                    assertEquals(
                        100_000L,
                        SqlMonthlyBillingRepository(database, dispatcher).remainingBalances(month).single().remainingMinor,
                    )
                    assertEquals(0L, treasury.observeTotals(day).first().drawerBalanceMinor)
                    assertEquals(
                        0.0,
                        center.domain.usecase
                            .GetTeacherMonthlyDuesUseCase(payments)("teacher", 11, 2026)
                            .getOrThrow(),
                    )
                    val auditEntry = treasury.observeLedger(now, now + 2_000L).first().single()
                    assertTrue(auditEntry.isVoided)
                    assertEquals("Duplicate entry", auditEntry.voidReason)
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }

    @Test
    fun mixedTracksCustomPricesAndMultipleLessonsAggregateMonthlyDue() =
        runTest {
            val path = Files.createTempDirectory("center-enrollment-pricing")
            try {
                openDatabase(path).use { driver ->
                    val database = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val teachers = SqlTeacherRepository(database, dispatcher)
                    val students = SqlStudentRepository(database, dispatcher)
                    val groups = SqlClassGroupRepository(database, dispatcher)
                    val billing = SqlMonthlyBillingRepository(database, dispatcher)
                    teachers.save(Teacher("teacher", "Teacher", "Arabic"))
                    val arabicStudent = Student("student-ar", "Student Arabic", parentPhone = "01000000001", code = "ST-1001")
                    val languagesStudent = Student("student-lang", "Student Languages", parentPhone = "01000000002", code = "ST-1002")
                    students.save(arabicStudent)
                    students.save(languagesStudent)
                    groups.save(
                        ClassGroup(
                            id = "mixed",
                            name = "Arabic and Languages",
                            teacherId = "teacher",
                            monthlyPriceMinor = 12_000,
                            commissionBasisPoints = 7_000,
                            roomNumber = 1,
                            system = EducationSystem.BOTH,
                            status = GroupStatus.ACTIVE,
                            lessonType = LessonType.MIXED,
                            languagesPriceMinor = 15_000,
                            enrollments =
                                mapOf(
                                    arabicStudent.id to LessonEnrollment(11_000, StudentTrack.ARABIC),
                                    languagesStudent.id to LessonEnrollment(15_000, StudentTrack.LANGUAGES),
                                ),
                        ),
                    )
                    groups.save(
                        ClassGroup(
                            id = "private",
                            name = "Private lesson",
                            teacherId = "teacher",
                            monthlyPriceMinor = 7_000,
                            commissionBasisPoints = 7_000,
                            roomNumber = 2,
                            system = EducationSystem.ARABIC,
                            status = GroupStatus.ACTIVE,
                            lessonType = LessonType.PRIVATE,
                            enrollments = mapOf(arabicStudent.id to LessonEnrollment(7_000, StudentTrack.PRIVATE)),
                        ),
                    )
                    val balances = billing.remainingBalances(month).associateBy { it.studentId to it.groupId }
                    assertEquals(11_000L, balances.getValue(arabicStudent.id to "mixed").remainingMinor)
                    assertEquals(15_000L, balances.getValue(languagesStudent.id to "mixed").remainingMinor)
                    val totals = billing.monthlyDueTotals(month).associate { it.studentId to it.totalMinor }
                    assertEquals(18_000L, totals.getValue(arabicStudent.id))
                    assertEquals(15_000L, totals.getValue(languagesStudent.id))
                    val persistedArabic = students.getStudentById(arabicStudent.id)!!
                    assertEquals(11_000L, persistedArabic.groupCustomPricesMinor.getValue("mixed"))
                    assertEquals(StudentTrack.ARABIC, persistedArabic.groupTracks["mixed"] ?: StudentTrack.ARABIC)
                    assertEquals(15_000L, students.getStudentById(languagesStudent.id)!!.groupCustomPricesMinor.getValue("mixed"))
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }

    @Test
    fun joinsMonthlyBalancesAndLedgerWithoutReducingSalaryForUnpaidStudents() =
        runTest {
            val path = Files.createTempDirectory("center-monthly")
            try {
                openDatabase(path).use { driver ->
                    val database = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val teachers = SqlTeacherRepository(database, dispatcher)
                    val groups = SqlClassGroupRepository(database, dispatcher)
                    val students = SqlStudentRepository(database, dispatcher)
                    val payments = SqlPaymentRepository(database, dispatcher)
                    val billing = SqlMonthlyBillingRepository(database, dispatcher)
                    val treasury = SqlTreasuryRepository(database, dispatcher)
                    val messages = SqlMessageQueueRepository(database, dispatcher)
                    val attendance = SqlAttendanceRepository(database, dispatcher)
                    val teacher = Teacher("t", "Karim Adel", "Math", phone = "01000001001", notes = "Contact after class")
                    val slot = GroupScheduleSlot(DayOfWeek.MONDAY, LocalTime(16, 0), LocalTime(17, 30))
                    val group = ClassGroup("g", "Math A", "t", 20_000, 6_000, 8, EducationSystem.BOTH, GroupStatus.ACTIVE, listOf(slot))
                    teachers.save(teacher)
                    groups.save(group)
                    assertEquals(listOf(group), groups.observeGroups().first())
                    assertFailsWith<IllegalArgumentException> { groups.save(group.copy(roomNumber = 9)) }
                    assertEquals(teacher.copy(assignedGroupIds = setOf("g")), teachers.observeTeachers().first().single())
                    val first =
                        Student(
                            id = "internal-1",
                            fullName = "Youssef",
                            gradeLevel = "Grade 12",
                            parentPhone = "01000001002",
                            code = "ST-1",
                            stage = EducationStage.SECONDARY,
                            system = EducationSystem.ARABIC,
                            enrolledGroupIds = setOf("g"),
                        )
                    students.save(first)
                    students.save(first.copy(id = "internal-2", code = "ST-2"))
                    students.save(first.copy(id = "inactive", code = "ST-3", isActive = false))
                    students.save(first.copy(id = "not-enrolled", code = "ST-4", enrolledGroupIds = emptySet()))
                    assertEquals(first, students.getStudentByCode("ST-1"))
                    assertNull(students.getStudentByCode("internal-1"))
                    val salary = GetTeacherMonthlySalaryUseCase(billing)
                    val deficit = GetTeacherCollectionDeficitUseCase(billing)
                    val overdue = GetOverdueStudentsUseCase(billing, EpochClock { now })
                    assertEquals(24_000L, salary("t").getOrThrow())
                    assertEquals(CollectionDeficit(2, 40_000), deficit("t", month).getOrThrow())
                    var eventId = 0
                    val ids = IdGenerator { "event-${++eventId}" }
                    val process = ProcessPaymentUseCase(students, teachers, payments, EpochClock { now }, ids)
                    assertTrue(process("ST-4", "g", 100.0).isFailure)
                    val partial = process("ST-1", "g", 100.0).getOrThrow()
                    assertEquals(60.0, partial.teacherShare)
                    assertEquals(month, partial.billingMonth)
                    assertEquals(listOf(partial), payments.getPaymentsForStudent(first.id).first())
                    assertTrue(process("ST-1", "g", 100.01).isFailure)
                    process("ST-2", "g", 200.0, LocalDate(2026, 12, 1)).getOrThrow()
                    assertEquals(listOf(10_000L, 20_000L), overdue().getOrThrow().map { it.remainingMinor })
                    assertEquals(CollectionDeficit(2, 30_000), deficit("t", month).getOrThrow())
                    assertEquals(24_000L, salary("t").getOrThrow())
                    process("ST-1", "g", 100.0).getOrThrow()
                    assertEquals(listOf("ST-2"), overdue().getOrThrow().map { it.studentCode })

                    treasury.recordExpense(TreasuryExpense("electricity", LedgerKind.OPERATIONAL, 1_500, now, "Electricity"))
                    treasury.recordExpense(TreasuryExpense("salary", LedgerKind.TEACHER_PAYOUT, 24_000, now, "November salary", "t"))
                    val ledger = treasury.observeLedger(now, now + 1).first()
                    assertEquals(
                        listOf(10_000L, 20_000L, 10_000L),
                        ledger.filter { it.direction == LedgerDirection.INCOME }.map { it.amountMinor },
                    )
                    assertEquals(
                        listOf(LedgerKind.OPERATIONAL, LedgerKind.TEACHER_PAYOUT),
                        ledger
                            .filter {
                                it.direction ==
                                    LedgerDirection.EXPENSE
                            }.map { it.kind },
                    )

                    var attendanceTime = now
                    val record = RecordAttendanceUseCase(students, teachers, attendance, EpochClock { attendanceTime }, ids)
                    record("ST-1", "g", AttendanceStatus.PRESENT).getOrThrow()
                    attendanceTime++
                    assertTrue(record("ST-1", "g", AttendanceStatus.PRESENT).isFailure)
                    val reminder =
                        QueueReminderUseCase(students, messages, EpochClock { now }, ids)
                            .invoke("ST-2", "Monthly subscription", ReminderChannel.NOTIFICATION)
                            .getOrThrow()
                    assertEquals("internal-2", reminder.targetPhone)
                    assertEquals(reminder, messages.getPendingMessages().first().single { it.channel == ReminderChannel.NOTIFICATION })

                    groups.archive("g")
                    assertEquals(
                        GroupStatus.ARCHIVED,
                        groups
                            .observeGroups()
                            .first()
                            .single()
                            .status,
                    )
                    assertTrue(overdue().getOrThrow().isEmpty())
                    assertEquals(0L, salary("t").getOrThrow())
                    assertEquals(5, treasury.observeLedger(now, now + 1).first().size)
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }

    @Test
    fun versionTwoUpgradeKeepsLegacyRowsAndDoesNotConvertSessionPricing() =
        runTest {
            val path = Files.createTempDirectory("center-v2")
            try {
                JdbcSqliteDriver("jdbc:sqlite:${path.resolve("center.db")}").use { driver ->
                    driver.execute(null, "CREATE TABLE student (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
                    CenterDatabase.Schema.migrate(driver, 1, 2)
                    driver.execute(null, "INSERT INTO student(id,name) VALUES ('legacy','Existing Student')", 0)
                    driver.execute(null, "INSERT INTO teacher(id,fullName,subject) VALUES ('t','Teacher','Math')", 0)
                    driver.execute(null, "INSERT INTO class_group VALUES ('g','Group','t',12.50)", 0)
                    driver.execute(null, "INSERT INTO payment VALUES ('p','legacy','g',10,7,3,100)", 0)
                    driver.execute(null, "INSERT INTO message_queue VALUES ('m','01000000000','Legacy reminder','PENDING',100)", 0)
                    driver.execute(null, "PRAGMA user_version = 2", 0)
                }
                repeat(2) {
                    openDatabase(path).use { driver ->
                        val database = CenterDatabase(driver)
                        val dispatcher = StandardTestDispatcher(testScheduler)
                        assertEquals("legacy", SqlStudentRepository(database, dispatcher).students().single().code)
                        assertEquals(
                            12.5,
                            database.classGroupQueries
                                .selectById("g")
                                .executeAsOne()
                                .sessionPrice,
                        )
                        assertNull(
                            SqlClassGroupRepository(database, dispatcher)
                                .observeGroups()
                                .first()
                                .single()
                                .monthlyPriceMinor,
                        )
                        assertNull(
                            SqlPaymentRepository(database, dispatcher)
                                .getPaymentsForStudent("legacy")
                                .first()
                                .single()
                                .billingMonth,
                        )
                        assertEquals(
                            ReminderChannel.WHATSAPP,
                            SqlMessageQueueRepository(database, dispatcher)
                                .getPendingMessages()
                                .first()
                                .single()
                                .channel,
                        )
                        assertEquals(
                            1_000L,
                            SqlTreasuryRepository(database, dispatcher)
                                .observeLedger(0, 101)
                                .first()
                                .single()
                                .amountMinor,
                        )
                    }
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }
}
