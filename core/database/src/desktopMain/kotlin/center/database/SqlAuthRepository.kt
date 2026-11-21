package center.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import center.domain.repository.AppUser
import center.domain.repository.AuthRepository
import center.domain.repository.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID

class SqlAuthRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : AuthRepository {
    private val users = database.appUserQueries
    private val passwords = PasswordHasher()

    suspend fun seedIfEmpty() =
        withContext(dispatcher) {
            if (users.countUsers().executeAsOne() != 0L) return@withContext
            val admin = passwords.hash("admin123")
            val reception = passwords.hash("1234")
            database.transaction {
                if (users.countUsers().executeAsOne() == 0L) {
                    users.insertUser(UUID.randomUUID().toString(), "admin", admin, "Admin", UserRole.ADMIN.name)
                    users.insertUser(UUID.randomUUID().toString(), "reception", reception, "Reception", UserRole.RECEPTION.name)
                }
            }
        }

    override suspend fun authenticate(
        username: String,
        password: String,
    ): AppUser? =
        withContext(dispatcher) {
            if (username.isBlank() || username.length > 64 || password.isEmpty() || password.length > 256) return@withContext null
            val row = users.findUser(username.trim().lowercase()).executeAsOneOrNull() ?: return@withContext null
            if (!row.isActive || !passwords.verify(password, row.passwordHash)) return@withContext null
            AppUser(row.id, row.username, row.displayName, UserRole.valueOf(row.role), row.isActive)
        }

    override fun observeUsers() =
        users
            .selectUsers { id, username, name, role, active ->
                AppUser(id, username, name, UserRole.valueOf(role), active)
            }.asFlow()
            .mapToList(dispatcher)

    override suspend fun addUser(
        username: String,
        password: String,
        displayName: String,
        role: UserRole,
    ): Unit =
        withContext(dispatcher) {
            val login = username.trim().lowercase()
            require(login.length in 1..64 && login.none { it.isWhitespace() || it.isISOControl() })
            require(displayName.trim().length in 1..100)
            require(password.length in 4..256)
            users.insertUser(UUID.randomUUID().toString(), login, passwords.hash(password), displayName.trim(), role.name)
        }

    override suspend fun changePassword(
        userId: String,
        password: String,
    ) = withContext(dispatcher) {
        require(password.length in 4..256)
        val hash = passwords.hash(password)
        database.transaction {
            require(users.userExists(userId).executeAsOne())
            users.updatePassword(hash, userId)
        }
    }
}
