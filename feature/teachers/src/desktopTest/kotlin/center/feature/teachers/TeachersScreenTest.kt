package center.feature.teachers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TeachersScreenTest {
    @Test
    fun directorySearchAndSelectionUseCallerOwnedState() =
        runComposeUiTest {
            val state = mutableStateOf(TeachersPreviewData.state())
            val events = mutableListOf<TeachersEvent>()
            setContent {
                CenterOneTheme {
                    CenterOneLocalization(AppLanguage.English) {
                        TeachersScreen(state.value, { event ->
                            events += event
                            if (event is TeachersEvent.SearchChanged) state.value = state.value.copy(query = event.query)
                        })
                    }
                }
            }
            onNodeWithText("Karim Adel").assertExists()
            onNodeWithText("Collection deficit: 3 students").assertExists()
            onNode(hasSetTextAction()).performTextReplacement("الرياضيات")
            onNodeWithText("Karim Adel").assertDoesNotExist()
            onNodeWithText("Noha Hassan").performClick()
            onNodeWithText("Add teacher").performClick()
            runOnIdle {
                assertEquals(
                    listOf(TeachersEvent.SearchChanged("الرياضيات"), TeachersEvent.OpenTeacher("2"), TeachersEvent.OpenAddTeacher),
                    events,
                )
            }
        }

    @Test
    fun localizedDetailsDisplayFullSalarySeparatelyFromStaffFollowUp() =
        runComposeUiTest {
            val teacher = TeachersPreviewData.state().teachers.first()
            val language = mutableStateOf(AppLanguage.English)
            val events = mutableListOf<TeachersEvent>()
            setContent {
                CenterOneTheme {
                    CenterOneLocalization(language.value) { TeacherDetails(teacher, { events += it }) }
                }
            }
            onNodeWithText("4,000 EGP").assertExists()
            onNodeWithText("600 EGP", substring = true).assertExists()
            onNodeWithText("Youssef Ahmed").assertExists()
            onNodeWithText("Grade 12 · Group A").assertExists()
            onNodeWithText("Pay Salary").performClick()
            runOnIdle { language.value = AppLanguage.Arabic }
            val salary = onNodeWithText("4,000 ج.م")
            assertEquals(LayoutDirection.Rtl, salary.fetchSemanticsNode().layoutInfo.layoutDirection)
            onNodeWithText("يوسف أحمد").assertExists()
            runOnIdle { assertEquals<TeachersEvent>(TeachersEvent.PaySalary("1"), events.single()) }
        }
}
