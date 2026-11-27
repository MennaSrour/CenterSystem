package center.domain.repository

import kotlinx.coroutines.flow.Flow

enum class UserRole { ADMIN, RECEPTION }

enum class Permission { DELETE_DIRECTORY_RECORDS, VIEW_MONTHLY_PROFIT, MANAGE_USERS, VOID_PAYMENTS }

fun UserRole.allows(permission: Permission): Boolean =
    when (permission) {
        Permission.DELETE_DIRECTORY_RECORDS, Permission.VIEW_MONTHLY_PROFIT, Permission.MANAGE_USERS, Permission.VOID_PAYMENTS ->
            this ==
                UserRole.ADMIN
    }

data class AppUser(
    val id: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val isActive: Boolean = true,
)

interface AuthRepository {
    suspend fun authenticate(
        username: String,
        password: String,
    ): AppUser?

    fun observeUsers(): Flow<List<AppUser>>

    suspend fun addUser(
        username: String,
        password: String,
        displayName: String,
        role: UserRole,
    )

    suspend fun changePassword(
        userId: String,
        password: String,
    )
}
