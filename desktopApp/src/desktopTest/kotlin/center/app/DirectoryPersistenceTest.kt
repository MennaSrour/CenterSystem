package center.app

import center.database.openDatabase
import center.domain.model.LedgerKind
import center.domain.repository.MessageQueueRepository
import center.domain.usecase.EpochClock
import center.feature.courses.CoursesEvent
import center.feature.courses.CoursesModal
import center.feature.courses.ScheduleDay
import center.feature.students.StudentStageFilter
import center.feature.students.StudentsEvent
import center.feature.teachers.TeachersEvent
import center.ui.localization.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.dsl.koinApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryPersistenceTest {
    @Test
    fun liveDirectoriesPersistEditsEnrollmentRemindersAndSalary() =
        runTest {
            val directory = Files.createTempDirectory("center-directories")
            val now = LocalDate(2026, 11, 5).startMillis()
            try {
                openDatabase(directory).use { driver ->
                    val app =
                        koinApplication { modules(persistenceModule(driver, StandardTestDispatcher(testScheduler), EpochClock { now })) }
                    try {
                        val services = app.koin.get<DirectoryServices>()
                        val teachers = TeachersStateHolder(services, backgroundScope, AppLanguage.English)
                        val courses = CoursesStateHolder(services, backgroundScope, AppLanguage.English)
                        val students = StudentsStateHolder(services, backgroundScope, AppLanguage.English)
                        runCurrent()

                        teachers.onEvent(TeachersEvent.OpenAddTeacher)
                        teachers.onEvent(TeachersEvent.NameChanged("Karim"))
                        teachers.onEvent(TeachersEvent.PhoneChanged("01000000001"))
                        teachers.onEvent(TeachersEvent.SubjectChanged("Physics"))
                        teachers.onEvent(TeachersEvent.SaveTeacher)
                        runCurrent()
                        val teacherId =
                            teachers.state.value.teachers
                                .single()
                                .id
                        teachers.onEvent(TeachersEvent.EditTeacher(teacherId))
                        teachers.onEvent(TeachersEvent.NameChanged("Karim Adel"))
                        teachers.onEvent(TeachersEvent.SaveTeacher)
                        runCurrent()
                        assertEquals(
                            "Karim Adel",
                            courses.uiState.value.teachers
                                .single()
                                .name,
                        )

                        courses.onEvent(CoursesEvent.OpenAddGroup)
                        val slotId =
                            (courses.uiState.value.modal as CoursesModal.Form)
                                .state.timeSlots
                                .single()
                                .id
                        listOf(
                            CoursesEvent.SubjectChanged("Physics A"),
                            CoursesEvent.TeacherSelected(teacherId),
                            CoursesEvent.GradeSelected("secondary-3"),
                            CoursesEvent.RoomSelected("8"),
                            CoursesEvent.MonthlyPriceChanged("200.50"),
                            CoursesEvent.CommissionChanged("60"),
                            CoursesEvent.MaxCapacityChanged("10"),
                            CoursesEvent.DaySelected(slotId, ScheduleDay.Monday),
                            CoursesEvent.FromTimeChanged(slotId, "16:00"),
                            CoursesEvent.ToTimeChanged(slotId, "17:30"),
                            CoursesEvent.SubmitGroup,
                        ).forEach(courses::onEvent)
                        runCurrent()
                        val group =
                            courses.uiState.value.groups
                                .single()
                        assertEquals(10, group.maxCapacity)
                        courses.onEvent(CoursesEvent.EditGroup(group.id))
                        courses.onEvent(CoursesEvent.SubjectChanged("Physics B"))
                        courses.onEvent(CoursesEvent.SubmitGroup)
                        runCurrent()
                        assertEquals(
                            "Physics B",
                            courses.uiState.value.groups
                                .single()
                                .subject,
                        )

                        listOf(
                            StudentsEvent.OpenAddStudent,
                            StudentsEvent.NameChanged("Youssef"),
                            StudentsEvent.CodeChanged("ST-1"),
                            StudentsEvent.ParentPhoneChanged("01000000002"),
                            StudentsEvent.AddGradeSelected("secondary-3"),
                            StudentsEvent.ToggleGroup(group.id),
                            StudentsEvent.SubmitAddStudent,
                            StudentsEvent.StageSelected(StudentStageFilter.Overdue),
                        ).forEach(students::onEvent)
                        runCurrent()
                        val studentId =
                            students.uiState.value.students
                                .single()
                                .id
                        assertEquals(
                            20_050L,
                            students.uiState.value.visibleOverdueRows
                                .single()
                                .payment.remainingMinor,
                        )
                        assertEquals(
                            12_030L,
                            teachers.state.value.teachers
                                .single()
                                .monthlySalaryMinor,
                        )
                        assertEquals(
                            20_050L,
                            teachers.state.value.teachers
                                .single()
                                .unpaidStudents
                                .single()
                                .subscriptionMinor,
                        )
                        students.onEvent(StudentsEvent.SendReminder(studentId))
                        runCurrent()
                        students.onEvent(StudentsEvent.RemindAll(listOf(studentId, studentId)))
                        runCurrent()
                        assertEquals(
                            2,
                            app.koin
                                .get<MessageQueueRepository>()
                                .getPendingMessages()
                                .first()
                                .size,
                        )

                        teachers.onEvent(TeachersEvent.OpenTeacher(teacherId))
                        teachers.onEvent(TeachersEvent.PaySalary(teacherId))
                        runCurrent()
                        val payout =
                            services.treasury
                                .observeLedger(now, now + 1)
                                .first()
                                .single()
                        assertEquals(LedgerKind.TEACHER_PAYOUT, payout.kind)
                        assertEquals(12_030L, payout.amountMinor)
                        assertTrue(services.paySalary(teacherId).isFailure)
                        assertTrue(
                            teachers.state.value.teachers
                                .single()
                                .salaryPaid,
                        )
                        assertEquals(
                            20_050L,
                            teachers.state.value.teachers
                                .single()
                                .unpaidStudents
                                .single()
                                .subscriptionMinor,
                        )

                        students.onEvent(StudentsEvent.EditStudent(studentId))
                        students.onEvent(StudentsEvent.NameChanged("Youssef Ahmed"))
                        students.onEvent(StudentsEvent.SubmitAddStudent)
                        runCurrent()
                        assertEquals("Youssef Ahmed", services.students.getStudentByCode("ST-1")?.fullName)
                        students.onEvent(StudentsEvent.DeleteStudent(studentId))
                        students.onEvent(StudentsEvent.ConfirmDelete)
                        runCurrent()
                        assertTrue(
                            students.uiState.value.students
                                .isEmpty(),
                        )
                        courses.onEvent(CoursesEvent.DeactivateGroup(group.id))
                        runCurrent()
                        assertTrue(
                            !courses.uiState.value.groups
                                .single()
                                .isActive,
                        )
                        courses.onEvent(CoursesEvent.RequestDeleteGroup(group.id))
                        courses.onEvent(CoursesEvent.ConfirmDeleteGroup)
                        runCurrent()
                        assertTrue(
                            courses.uiState.value.groups
                                .isEmpty(),
                        )
                    } finally {
                        app.close()
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
