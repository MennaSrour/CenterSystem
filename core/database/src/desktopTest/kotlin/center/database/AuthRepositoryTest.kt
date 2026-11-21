package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import center.domain.repository.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {
    @Test
    fun saltedSha256DerivationProducesUniqueVerifiableHashesAndRejectsInvalidValues() {
        val hasher = PasswordHasher()
        val first = hasher.hash("Secret صعب")
        val second = hasher.hash("Secret صعب")
        assertNotEquals(first, second)
        assertFalse(first.contains("Secret صعب"))
        assertTrue(hasher.verify("Secret صعب", first))
        assertTrue(hasher.verify("Secret صعب", second))
        assertFalse(hasher.verify("wrong", first))
        assertFalse(hasher.verify("Secret صعب", "pbkdf2-sha256-v1\$bad!\$data"))
        assertFalse(hasher.verify("Secret صعب", "Secret صعب"))
    }

    @Test
    fun offlineLoginAndMigrationPreserveExistingDataAndNeverResetPasswords() =
        runTest {
            val path = Files.createTempDirectory("center-auth-upgrade")
            try {
                JdbcSqliteDriver("jdbc:sqlite:${path.resolve("center.db")}").use { driver ->
                    driver.execute(null, "CREATE TABLE student (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
                    CenterDatabase.Schema.migrate(driver, 1, 5)
                    driver.execute(null, "INSERT INTO student(id,name) VALUES ('legacy','Existing Student')", 0)
                    driver.execute(null, "PRAGMA user_version = 5", 0)
                }
                openDatabase(path).use { driver ->
                    val database = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val repository = SqlAuthRepository(database, dispatcher)
                    repository.seedIfEmpty()
                    assertEquals("Existing Student", SqlStudentRepository(database, dispatcher).students().single().fullName)
                    val admin = requireNotNull(repository.authenticate(" Admin ", "admin123"))
                    assertEquals(UserRole.ADMIN, admin.role)
                    assertEquals(UserRole.RECEPTION, repository.authenticate("reception", "1234")?.role)
                    assertNull(repository.authenticate("admin", "wrong"))
                    assertNull(repository.authenticate("missing", "admin123"))
                    assertFalse(
                        database.appUserQueries
                            .findUser("admin")
                            .executeAsOne()
                            .passwordHash
                            .contains("admin123"),
                    )
                    repository.addUser("Nour", "new secret", "Nour Adel", UserRole.RECEPTION)
                    assertFails { repository.addUser("NOUR", "another secret", "Duplicate", UserRole.ADMIN) }
                    val nour = requireNotNull(repository.authenticate("NOUR", "new secret"))
                    driver.execute(null, "UPDATE app_user SET isActive = 0 WHERE username = 'nour'", 0)
                    assertNull(repository.authenticate("nour", "new secret"))
                    assertEquals("Nour Adel", nour.displayName)
                    repository.changePassword(admin.id, "changed secret")
                    repository.seedIfEmpty()
                    assertNull(repository.authenticate("admin", "admin123"))
                    assertEquals(admin, repository.authenticate("admin", "changed secret"))
                    assertEquals(3, repository.observeUsers().first().size)
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }
}
