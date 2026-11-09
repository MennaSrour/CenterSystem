package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import center.database.entity.AttendanceEntity
import center.database.entity.AttendanceStatus
import center.database.entity.ClassGroupEntity
import center.database.entity.MessageQueueEntity
import center.database.entity.MessageStatus
import center.database.entity.PaymentEntity
import center.database.entity.StudentEntity
import center.database.entity.TeacherEntity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CenterDaosTest {
    private val teacher = TeacherEntity("teacher", "Teacher", "Math")
    private val student = StudentEntity("student", "Menna", "Grade 10", parentPhone = "01012345678")
    private val group = ClassGroupEntity("class", "Math A", teacher.id, 100.0)

    @Test
    fun crudPreservesRelationsAndSupportsDeactivation() =
        runTest {
            withDatabase { daos, _ ->
                seed(daos)
                assertEquals(
                    0.70,
                    daos.teachers
                        .observeById(teacher.id)
                        .first()
                        ?.commissionRate,
                )
                daos.students.save(student.copy(isActive = false, studentPhone = "01112345678"))
                assertFalse(requireNotNull(daos.students.observeById(student.id).first()).isActive)
                daos.attendance.save(AttendanceEntity("attendance", student.id, group.id, 100L, AttendanceStatus.PRESENT))
                daos.attendance.save(AttendanceEntity("attendance", student.id, group.id, 100L, AttendanceStatus.ABSENT))
                assertEquals(
                    AttendanceStatus.ABSENT,
                    daos.attendance
                        .getAttendanceForClass(group.id, 100L, 101L)
                        .first()
                        .single()
                        .status,
                )
                daos.attendance.delete("attendance")
                daos.classGroups.save(group.copy(sessionPrice = 120.0))
                assertEquals(
                    120.0,
                    daos.classGroups
                        .observeAll()
                        .first()
                        .single()
                        .sessionPrice,
                )
                daos.classGroups.delete(group.id)
                daos.students.delete(student.id)
                daos.teachers.delete(teacher.id)
                assertNull(daos.students.observeById(student.id).first())
            }
        }

    @Test
    fun studentPaymentRelationReactsToWritesAndKeepsHistoryOnStudentUpdate() =
        runTest {
            withDatabase { daos, _ ->
                seed(daos)
                val observed =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        daos.students
                            .getStudentWithPayments(student.id)
                            .take(3)
                            .toList()
                    }
                runCurrent()
                daos.payments.save(payment("first", 100L))
                runCurrent()
                daos.students.save(student.copy(fullName = "Menna Srour"))
                runCurrent()
                val values = observed.await()
                assertEquals(listOf(0, 1, 1), values.map { requireNotNull(it).payments.size })
                assertEquals("Menna Srour", values.last()?.student?.fullName)
                assertNull(daos.students.getStudentWithPayments("missing").first())
                daos.payments.save(payment("first", 100L).copy(amountPaid = 200.0, teacherShare = 140.0, centerShare = 60.0))
                assertEquals(
                    140.0,
                    daos.payments
                        .getPaymentsForStudent(student.id)
                        .first()
                        .single()
                        .teacherShare,
                )
                daos.payments.delete("first")
                assertEquals(emptyList(), daos.payments.observeAll().first())
            }
        }

    @Test
    fun duesUseStoredSharesAndHalfOpenMonthBoundaries() =
        runTest {
            withDatabase { daos, _ ->
                seed(daos)
                daos.payments.save(payment("before", 99L))
                daos.payments.save(payment("start", 100L))
                daos.payments.save(payment("inside", 199L))
                daos.payments.save(payment("end", 200L))
                daos.teachers.save(teacher.copy(commissionRate = 0.5))
                val dues = daos.payments.getTeacherDuesByMonth(teacher.id, 100L, 200L).first()
                assertEquals(200.0, dues.totalPaid)
                assertEquals(140.0, dues.teacherShare)
                assertEquals(60.0, dues.centerShare)
                assertEquals(
                    0.0,
                    daos.payments
                        .getTeacherDuesByMonth("other", 100L, 200L)
                        .first()
                        .teacherShare,
                )
                assertFailsWith<IllegalArgumentException> { daos.payments.getTeacherDuesByMonth(teacher.id, 200L, 100L) }
            }
        }

    @Test
    fun foreignKeysAndAttendanceUniquenessAreEnforced() =
        runTest {
            withDatabase { daos, _ ->
                assertFailsWith<SQLException> { daos.classGroups.save(group) }
                seed(daos)
                assertFailsWith<SQLException> { daos.payments.save(payment("orphan", 100L).copy(studentId = "missing")) }
                daos.payments.save(payment("payment", 100L))
                daos.teachers.save(teacher.copy(id = "another-teacher"))
                assertFailsWith<IllegalStateException> { daos.classGroups.save(group.copy(teacherId = "another-teacher")) }
                assertFailsWith<SQLException> { daos.students.delete(student.id) }
                assertFailsWith<SQLException> { daos.classGroups.delete(group.id) }
                assertFailsWith<SQLException> { daos.teachers.delete(teacher.id) }
                daos.attendance.save(AttendanceEntity("first", student.id, group.id, 100L, AttendanceStatus.PRESENT))
                assertFailsWith<SQLException> {
                    daos.attendance.save(AttendanceEntity("duplicate", student.id, group.id, 100L, AttendanceStatus.ABSENT))
                }
            }
        }

    @Test
    fun localMessageQueueIsOrderedAndSentMessagesAreExcluded() =
        runTest {
            withDatabase { daos, _ ->
                daos.messages.save(MessageQueueEntity("later", "01012345678", "Later", createdAt = 200L))
                daos.messages.save(MessageQueueEntity("first", "01012345678", "First", createdAt = 100L))
                assertEquals(
                    listOf("first", "later"),
                    daos.messages
                        .getPendingMessages()
                        .first()
                        .map { it.id },
                )
                daos.messages.markSent("first")
                daos.messages.markSent("first")
                assertEquals(
                    MessageStatus.SENT,
                    daos.messages
                        .observeById("first")
                        .first()
                        ?.status,
                )
                assertEquals(
                    listOf("later"),
                    daos.messages
                        .getPendingMessages()
                        .first()
                        .map { it.id },
                )
                daos.messages.delete("later")
                assertEquals(emptyList(), daos.messages.getPendingMessages().first())
            }
        }

    @Test
    fun migratesOriginalStudentDatabaseAndReopensWithoutDataLoss() =
        runTest {
            val directory = Files.createTempDirectory("center-migration")
            try {
                JdbcSqliteDriver("jdbc:sqlite:${directory.resolve("center.db")}").use { driver ->
                    driver.execute(null, "CREATE TABLE student (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
                    driver.execute(null, "INSERT INTO student VALUES ('legacy', 'Existing Student')", 0)
                    driver.execute(null, "PRAGMA user_version = 1", 0)
                }
                repeat(2) {
                    openDatabase(directory).use { driver ->
                        val daos = CenterDaos(CenterDatabase(driver), StandardTestDispatcher(testScheduler))
                        val legacy = requireNotNull(daos.students.observeById("legacy").first())
                        assertEquals("Existing Student", legacy.fullName)
                        assertEquals("", legacy.gradeLevel)
                        assertEquals("", legacy.parentPhone)
                        daos.teachers.save(teacher)
                        assertEquals(
                            teacher,
                            daos.teachers
                                .observeAll()
                                .first()
                                .single(),
                        )
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }

    @Test
    fun rejectsInvalidRatesAndPaymentSplits() {
        assertFailsWith<IllegalArgumentException> { teacher.copy(commissionRate = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { teacher.copy(commissionRate = 1.1) }
        assertFailsWith<IllegalArgumentException> { payment("bad", 100L).copy(centerShare = 25.0) }
        assertFailsWith<IllegalArgumentException> { payment("bad", 100L).copy(amountPaid = Double.POSITIVE_INFINITY) }
    }

    private fun payment(
        id: String,
        date: Long,
    ) = PaymentEntity(id, student.id, group.id, 100.0, 70.0, 30.0, date)

    private suspend fun seed(daos: CenterDaos) {
        daos.teachers.save(teacher)
        daos.students.save(student)
        daos.classGroups.save(group)
    }

    private suspend fun TestScope.withDatabase(block: suspend (CenterDaos, Path) -> Unit) {
        val directory = Files.createTempDirectory("center-daos")
        try {
            openDatabase(directory).use { driver ->
                block(CenterDaos(CenterDatabase(driver), StandardTestDispatcher(testScheduler)), directory)
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
