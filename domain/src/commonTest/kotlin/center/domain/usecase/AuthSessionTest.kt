package center.domain.usecase

import center.domain.repository.AppUser
import center.domain.repository.AuthRepository
import center.domain.repository.Permission
import center.domain.repository.UserRole
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthSessionTest {
    @Test
    fun receptionCanSignInButOnlyAdminCanUseRestrictedActions() =
        runTest {
            val repo = FakeAuth()
            val session = AuthSession(repo)
            assertFalse(session.signIn("inactive", "valid"))
            assertFalse(session.signIn("admin", "incorrect"))
            assertTrue(session.signIn("reception", "valid"))
            Permission.entries.forEach { assertFalse(session.allows(it)) }
            assertFailsWith<IllegalStateException> { session.users() }
            assertFailsWith<IllegalStateException> { session.addUser("user", "pass", "User", UserRole.ADMIN) }
            assertFailsWith<IllegalStateException> { session.changePassword("admin", "changed") }
            assertEquals(0, repo.writes)
            session.logout()
            assertNull(session.user.value)
            assertFalse(session.allows(Permission.MANAGE_USERS))
            assertTrue(session.signIn("admin", "valid"))
            Permission.entries.forEach { assertTrue(session.allows(it)) }
            session.addUser("user", "pass", "User", UserRole.RECEPTION)
            session.changePassword("reception", "changed")
            assertEquals(2, repo.writes)
        }

    private class FakeAuth : AuthRepository {
        var writes = 0

        override suspend fun authenticate(
            username: String,
            password: String,
        ): AppUser? =
            if (password != "valid") {
                null
            } else {
                AppUser(
                    username,
                    username,
                    username,
                    if (username == "admin") UserRole.ADMIN else UserRole.RECEPTION,
                    isActive = username != "inactive",
                )
            }

        override fun observeUsers() = flowOf(emptyList<AppUser>())

        override suspend fun addUser(
            username: String,
            password: String,
            displayName: String,
            role: UserRole,
        ) {
            writes++
        }

        override suspend fun changePassword(
            userId: String,
            password: String,
        ) {
            writes++
        }
    }
}
