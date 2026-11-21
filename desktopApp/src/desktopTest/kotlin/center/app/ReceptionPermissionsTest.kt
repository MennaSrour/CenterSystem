package center.app

import center.database.SqlAuthRepository
import center.database.openDatabase
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupStatus
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.repository.Permission
import center.domain.usecase.AuthSession
import center.feature.courses.CoursesEvent
import center.feature.courses.CoursesModal
import center.feature.students.StudentsEvent
import center.feature.teachers.TeachersEvent
import center.feature.teachers.TeachersModal
import center.ui.localization.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionPermissionsTest {
    @Test
    fun receptionCannotOpenOrConfirmDeletesAndNeverReceivesMonthlyProfit() =
        runTest {
            val directory = Files.createTempDirectory("center-reception-role")
            try {
                openDatabase(directory).use { driver ->
                    val koin = koinApplication { modules(persistenceModule(driver, StandardTestDispatcher(testScheduler))) }
                    try {
                        koin.koin.get<SqlAuthRepository>().seedIfEmpty()
                        val services = koin.koin.get<DirectoryServices>()
                        val session = koin.koin.get<AuthSession>()
                        assertTrue(session.signIn("reception", "1234"))
                        val canDelete = { session.allows(Permission.DELETE_DIRECTORY_RECORDS) }
                        services.teachers.save(Teacher("t", "Karim", "Physics"))
                        services.groups.save(ClassGroup("g", "Physics A", "t", 10_000, 6_000, 1, EducationSystem.BOTH, GroupStatus.ACTIVE))
                        services.students.save(Student("s", "Amira"))
                        val students = StudentsStateHolder(services, backgroundScope, AppLanguage.English, canDelete = canDelete)
                        val courses = CoursesStateHolder(services, backgroundScope, AppLanguage.English, canDelete)
                        val teachers = TeachersStateHolder(services, backgroundScope, AppLanguage.English, canDelete)
                        val treasury =
                            FinancialsStateHolder(
                                services,
                                backgroundScope,
                                AppLanguage.English,
                                canViewProfit = session.allows(Permission.VIEW_MONTHLY_PROFIT),
                            )
                        runCurrent()

                        students.onEvent(StudentsEvent.DeleteStudent("s"))
                        students.onEvent(StudentsEvent.ConfirmDelete)
                        courses.onEvent(CoursesEvent.RequestDeleteGroup("g"))
                        courses.onEvent(CoursesEvent.ConfirmDeleteGroup)
                        teachers.onEvent(TeachersEvent.OpenTeacher("t"))
                        teachers.onEvent(TeachersEvent.RequestDeleteTeacher("t"))
                        teachers.onEvent(TeachersEvent.ConfirmDeleteTeacher)
                        runCurrent()

                        assertFalse(students.uiState.value.canDelete)
                        assertFalse(courses.uiState.value.canDelete)
                        assertFalse(teachers.state.value.canDelete)
                        assertNull(students.uiState.value.deleteStudent)
                        assertEquals(CoursesModal.Closed, courses.uiState.value.modal)
                        assertIs<TeachersModal.Details>(teachers.state.value.modal)
                        assertNull(treasury.uiState.value.summary.monthlyProfit)
                        assertEquals(1, students.uiState.value.students.size)
                        assertEquals(1, courses.uiState.value.groups.size)
                        assertEquals(1, teachers.state.value.teachers.size)

                        session.logout()
                        assertTrue(session.signIn("admin", "admin123"))
                        val adminTeachers = TeachersStateHolder(services, backgroundScope, AppLanguage.English, canDelete)
                        services.teachers.save(Teacher("delete", "Unused teacher", "Physics"))
                        runCurrent()
                        assertTrue(adminTeachers.state.value.canDelete)
                        adminTeachers.onEvent(TeachersEvent.OpenTeacher("delete"))
                        adminTeachers.onEvent(TeachersEvent.RequestDeleteTeacher("delete"))
                        assertIs<TeachersModal.Delete>(adminTeachers.state.value.modal)
                        adminTeachers.onEvent(TeachersEvent.ConfirmDeleteTeacher)
                        runCurrent()
                        assertEquals(
                            listOf("t"),
                            adminTeachers.state.value.teachers
                                .map { it.id },
                        )
                    } finally {
                        koin.close()
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
