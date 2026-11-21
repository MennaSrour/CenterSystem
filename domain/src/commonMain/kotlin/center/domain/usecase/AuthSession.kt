package center.domain.usecase

import center.domain.repository.AppUser
import center.domain.repository.AuthRepository
import center.domain.repository.Permission
import center.domain.repository.UserRole
import center.domain.repository.allows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSession(
    private val repository: AuthRepository,
) {
    private val current = MutableStateFlow<AppUser?>(null)
    val user = current.asStateFlow()

    suspend fun signIn(
        username: String,
        password: String,
    ): Boolean {
        val authenticated = repository.authenticate(username, password)?.takeIf { it.isActive } ?: return false
        return current.compareAndSet(null, authenticated)
    }

    fun logout() {
        current.value = null
    }

    fun allows(permission: Permission) = current.value?.role?.allows(permission) == true

    fun users() = requireManager().observeUsers()

    suspend fun addUser(
        username: String,
        password: String,
        displayName: String,
        role: UserRole,
    ) {
        requireManager().addUser(username, password, displayName, role)
    }

    suspend fun changePassword(
        id: String,
        password: String,
    ) {
        requireManager().changePassword(id, password)
    }

    private fun requireManager(): AuthRepository {
        check(allows(Permission.MANAGE_USERS)) { "Account management requires an administrator" }
        return repository
    }
}
