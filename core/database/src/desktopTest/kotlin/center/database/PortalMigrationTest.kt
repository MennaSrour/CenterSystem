package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortalMigrationTest {
    @Test
    fun versionSixMigratesExistingStudentsPaymentsAndAttendance() {
        val path = Files.createTempDirectory("center-portal-migration")
        try {
            JdbcSqliteDriver("jdbc:sqlite:${path.resolve("center.db")}").use { driver ->
                driver.execute(null, "CREATE TABLE student (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
                CenterDatabase.Schema.migrate(driver, 1, 6)
                driver.execute(null, "INSERT INTO student (id, name) VALUES ('s', 'Student')", 0)
                driver.execute(null, "INSERT INTO teacher (id, fullName, subject) VALUES ('t', 'Teacher', 'Math')", 0)
                driver.execute(null, "INSERT INTO class_group (id, name, teacherId, sessionPrice) VALUES ('g', 'Group', 't', 10)", 0)
                driver.execute(null, "UPDATE class_group SET monthlyPriceMinor = 12000 WHERE id = 'g'", 0)
                driver.execute(null, "INSERT INTO enrollment(studentId, classId, discountAmount) VALUES ('s', 'g', 2)", 0)
                driver.execute(
                    null,
                    """INSERT INTO payment (id, studentId, classId, amountPaid, teacherShare, centerShare, paymentDate)
                       VALUES ('p', 's', 'g', 10, 7, 3, 1)""",
                    0,
                )
                driver.execute(
                    null,
                    """INSERT INTO attendance_session (personType, personId, groupId, date, checkInTime)
                       VALUES ('STUDENT', 's', 'g', '2026-03-01', 10)""",
                    0,
                )
                driver.execute(null, "PRAGMA user_version = 6", 0)
            }

            openDatabase(path).use { driver ->
                val db = CenterDatabase(driver)
                val student = db.studentQueries.selectById("s").executeAsOne()
                assertEquals("PENDING", student.syncStatus)
                assertEquals(0L, student.updatedAt)
                val payment = db.paymentQueries.selectById("p").executeAsOne()
                assertEquals(10.0, payment.amountPaid)
                assertEquals("PENDING", payment.syncStatus)
                val migratedLesson = db.classGroupQueries.selectById("g").executeAsOne()
                assertEquals("MIXED", migratedLesson.lessonType)
                assertEquals(null, migratedLesson.languagesPriceMinor)
                val enrollment = db.operationsQueries.selectEnrollmentDetails("s", "g").executeAsOne()
                assertEquals(11_800L, enrollment.customPriceMinor)
                assertEquals("ARABIC", enrollment.studentTrack)
                assertEquals(
                    "",
                    db.teacherQueries
                        .selectById("t")
                        .executeAsOne()
                        .portalPin,
                )
                db.peopleQueries.setCheckOut(20, "STUDENT", "s", "g", "2026-03-01")
                val session = db.peopleQueries.selectSession("STUDENT", "s", "g", "2026-03-01").executeAsOne()
                assertEquals(20L, session.checkOutTime)
                assertTrue(session.updatedAt > 0)
                driver.execute(null, "UPDATE student SET syncStatus = 'SYNCED' WHERE id = 's'", 0)
                db.studentQueries.updateRow("New Name", "", null, "01012345678", true, "s")
                val updated = db.studentQueries.selectById("s").executeAsOne()
                assertEquals("PENDING", updated.syncStatus)
                assertTrue(updated.updatedAt > 0)
            }
        } finally {
            path.toFile().deleteRecursively()
        }
    }
}
