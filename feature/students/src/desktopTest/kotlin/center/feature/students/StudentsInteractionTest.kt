package center.feature.students

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StudentsInteractionTest {
    @Test
    fun formEditsSystemChoiceAndSaveEmitEventsWithoutMutatingTheSnapshot() =
        runComposeUiTest {
            val form = StudentsPreviewData.filledForm()
            val events = mutableListOf<StudentsEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        AddStudentModalContent(form, events::add)
                    }
                }
            }
            onNode(hasSetTextAction() and hasText("يوسف أحمد")).performScrollTo().performTextReplacement("مريم أحمد")
            runOnIdle { assertEquals(StudentsEvent.NameChanged("مريم أحمد"), events.last()) }
            onNodeWithText("لغات").performScrollTo().performClick()
            runOnIdle {
                assertEquals(StudentsEvent.SystemSelected(StudentSystem.Languages), events.last())
                assertEquals(StudentSystem.Arabic, form.system)
                assertEquals("يوسف أحمد", form.name)
            }
            onNodeWithText("حفظ الطالب / Save Student").performScrollTo().performClick()
            runOnIdle { assertEquals(StudentsEvent.SubmitAddStudent, events.last()) }
            onNodeWithText("إلغاء / Cancel").performClick()
            runOnIdle { assertEquals(StudentsEvent.DismissAddStudent, events.last()) }
        }

    @Test
    fun submissionDisablesSaveCancelAndFormEdits() =
        runComposeUiTest {
            setContent {
                CenterOneTheme { AddStudentModalContent(StudentsPreviewData.filledForm().copy(isSubmitting = true), {}) }
            }
            onNodeWithText("يوسف أحمد").assertIsNotEnabled()
            onNodeWithText("حفظ الطالب / Save Student").performScrollTo().assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
            onNodeWithText("إغلاق").performScrollTo().assertIsNotEnabled()
        }

    @Test
    fun rowActionsUseStableStudentIds() =
        runComposeUiTest {
            val state = StudentsPreviewData.state()
            val events = mutableListOf<StudentsEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        StudentsDataGrid(state.students, state.grades, events::add)
                    }
                }
            }
            onNodeWithContentDescription("تعديل بيانات أحمد محمد علي").performClick()
            runOnIdle { assertEquals(StudentsEvent.EditStudent("student-1"), events.last()) }
            onNodeWithContentDescription("حذف أحمد محمد علي").performClick()
            runOnIdle { assertEquals(StudentsEvent.DeleteStudent("student-1"), events.last()) }
        }

    @Test
    fun overdueTabAndActionsEmitStableIdsFromTheFilteredTable() =
        runDesktopComposeUiTest(1440, 1000) {
            val state = mutableStateOf(StudentsPreviewData.state().copy(searchQuery = "ST-10234"))
            val events = mutableListOf<StudentsEvent>()
            setContent {
                CenterOneTheme {
                    CenterOneLocalization(AppLanguage.English) {
                        StudentsScreen(state.value, { event ->
                            events += event
                            if (event is StudentsEvent.StageSelected) state.value = state.value.copy(stageFilter = event.filter)
                        })
                    }
                }
            }
            onNodeWithText("Overdue").performScrollTo().performClick()
            onNodeWithText("Ahmed Mohamed").assertExists()
            onNodeWithText("Sara Mahmoud").assertDoesNotExist()
            onNodeWithText("Remind All").performScrollTo().performClick()
            onNodeWithText("Send Reminder").performScrollTo().performClick()
            onNodeWithText("Pay Now").performScrollTo().performClick()
            runOnIdle {
                assertEquals(
                    listOf(
                        StudentsEvent.StageSelected(StudentStageFilter.Overdue),
                        StudentsEvent.RemindAll(listOf("student-1")),
                        StudentsEvent.SendReminder("student-1"),
                        StudentsEvent.PayNow("payment-1"),
                    ),
                    events,
                )
            }
            runOnIdle { state.value = state.value.copy(searchQuery = "no matches") }
            onNodeWithText("Remind All").assertIsNotEnabled()
        }
}
