package center.database

import center.domain.Student
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.sql.DriverManager
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseBackupTest {
    @Test
    fun exportsAStandaloneSnapshotIncludingCommittedWalChangesAndPreservesEarlierExports() =
        runTest {
            val directory = Files.createTempDirectory("center-backup-test")
            try {
                val folder = Files.createDirectory(directory.resolve("نسخ احتياطية"))
                val date = LocalDate.of(2026, 11, 22)
                val backup = DatabaseBackup(directory)
                openDatabase(directory).use { driver ->
                    DriverManager.getConnection("jdbc:sqlite:${directory.resolve(DATABASE_FILE)}").use { writer ->
                        writer.createStatement().use { statement ->
                            statement.execute("PRAGMA journal_mode=WAL")
                            statement.executeUpdate("INSERT INTO student(id, name) VALUES ('wal', 'مريم')")
                        }
                        assertTrue(Files.size(directory.resolve("$DATABASE_FILE-wal")) > 0)
                        val first = backup.export(folder, date)
                        val second = backup.export(folder, date)
                        assertEquals("center_one_backup_2026-11-22.db", first.fileName.toString())
                        assertEquals("center_one_backup_2026-11-22-2.db", second.fileName.toString())
                        assertTrue(Files.size(first) > 0)
                        DriverManager.getConnection("jdbc:sqlite:$first").use { copy ->
                            copy.createStatement().use { statement ->
                                statement.executeQuery("SELECT name FROM student WHERE id = 'wal'").use { rows ->
                                    assertTrue(rows.next())
                                    assertEquals("مريم", rows.getString(1))
                                }
                            }
                        }
                    }
                    assertEquals("مريم", SqlStudentRepository(CenterDatabase(driver)).getStudentById("wal")?.fullName)
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }

    @Test
    fun stagesValidatedRestoreAndKeepsThePreRestoreDatabase() =
        runTest {
            val directory = Files.createTempDirectory("center-restore-test")
            try {
                val backup = DatabaseBackup(directory)
                lockDatabaseDirectory(directory).use {
                    val invalid = Files.writeString(directory.resolve("invalid.db"), "invalid")
                    openDatabase(directory).use { driver ->
                        val repository = SqlStudentRepository(CenterDatabase(driver))
                        repository.save(Student("old", "Original"))
                        val snapshot = backup.export(directory, LocalDate.of(2026, 11, 22))
                        repository.save(Student("new", "Current"))
                        assertFailsWith<java.sql.SQLException> { backup.stageRestore(invalid) }
                        backup.stageRestore(snapshot)
                        assertEquals(2, repository.students().size)
                    }
                    backup.applyPendingRestore()
                    backup.applyPendingRestore()
                    openDatabase(directory).use { driver ->
                        assertEquals(listOf("old"), SqlStudentRepository(CenterDatabase(driver)).students().map { it.id })
                    }
                    val previous = directory.resolve("center.before-restore.db")
                    DriverManager.getConnection("jdbc:sqlite:$previous").use { connection ->
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SELECT count(*) FROM student").use { rows ->
                                assertTrue(rows.next())
                                assertEquals(2, rows.getInt(1))
                            }
                        }
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
