package center.feature.dashboard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import center.ui.components.CenterOneBrand
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DashboardLocalizationTest {
    @Test
    fun toggleChangesTextAndDirectionAndPreservesTheStudentCode() =
        runComposeUiTest {
            val state = mutableStateOf(DashboardState("نور", "عرض تجريبي"))
            setContent {
                CenterOneTheme {
                    DashboardScreen(state.value, { event ->
                        when (event) {
                            is DashboardEvent.LanguageSelected -> state.value = state.value.copy(language = event.language)
                            is DashboardEvent.StudentCodeChanged -> state.value = state.value.copy(studentCode = event.value)
                            else -> Unit
                        }
                    })
                }
            }
            onNode(hasSetTextAction()).performTextReplacement("10234")
            onNodeWithText("نظرة عامة").assertExists()
            assertEquals(LayoutDirection.Rtl, onNodeWithText("نظرة عامة").fetchSemanticsNode().layoutInfo.layoutDirection)
            onNodeWithText("English").performClick()
            onNodeWithText("Overview").assertExists()
            onNodeWithText("نظرة عامة").assertDoesNotExist()
            onNodeWithText("Home").assertExists()
            onNodeWithText("الرئيسية").assertDoesNotExist()
            assertEquals(LayoutDirection.Ltr, onNodeWithText("Overview").fetchSemanticsNode().layoutInfo.layoutDirection)
            onNode(hasSetTextAction() and hasText("10234")).assertExists()
            onNodeWithText("عربي").performClick()
            onNodeWithText("نظرة عامة").assertExists()
            assertEquals(LayoutDirection.Rtl, onNodeWithText("نظرة عامة").fetchSemanticsNode().layoutInfo.layoutDirection)
        }

    @Test
    fun activityTableDisplaysOnlyTheSelectedLanguage() =
        runComposeUiTest {
            val language = mutableStateOf(AppLanguage.Arabic)
            val activities = listOf(RecentActivity("1", "أحمد محمد", "Ahmed Mohamed", "10234", "09:42", ActivityStatus.Completed))
            setContent {
                CenterOneTheme {
                    CenterOneLocalization(language.value) { RecentActivityTable(activities, {}) }
                }
            }
            onNodeWithText("أحمد محمد").assertExists()
            onNodeWithText("Ahmed Mohamed").assertDoesNotExist()
            runOnIdle { language.value = AppLanguage.English }
            onNodeWithText("Ahmed Mohamed").assertExists()
            onNodeWithText("أحمد محمد").assertDoesNotExist()
            onNodeWithText("Student name").assertExists()
            onNodeWithText("Completed").assertExists()
        }

    @Test
    fun brandRendersSuppliedPainterAndOneLocalizedNameWithoutANumberPlaceholder() =
        runComposeUiTest {
            setContent {
                CenterOneTheme {
                    CenterOneLocalization(AppLanguage.English) {
                        CenterOneBrand("سنتر ون", "Center One", logo = ColorPainter(Color.Blue))
                    }
                }
            }
            onNodeWithContentDescription("Center One").assertIsDisplayed()
            onNodeWithText("سنتر ون").assertDoesNotExist()
            onNodeWithText("1").assertDoesNotExist()
        }
}
