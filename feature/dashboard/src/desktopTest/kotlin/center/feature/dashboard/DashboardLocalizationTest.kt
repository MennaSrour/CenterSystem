package center.feature.dashboard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import center.ui.components.CenterOneBrand
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DashboardLocalizationTest {
    @Test
    fun toggleLocalizesReceptionAndPreservesSearchInput() =
        runComposeUiTest {
            val state = mutableStateOf(DashboardPreviewData.state())
            setContent {
                CenterOneLocalization(state.value.language) {
                    CenterOneTheme {
                        DashboardScreen(state.value, { event ->
                            when (event) {
                                is DashboardEvent.LanguageSelected -> state.value = state.value.copy(language = event.language)
                                is DashboardEvent.QueryChanged -> state.value = state.value.copy(query = event.value)
                                else -> Unit
                            }
                        })
                    }
                }
            }
            onNode(hasSetTextAction()).performTextReplacement("10234")
            val attendance = onNodeWithText("حضور اليوم")
            assertEquals(LayoutDirection.Rtl, attendance.fetchSemanticsNode().layoutInfo.layoutDirection)
            onNodeWithText("العربية").assertIsDisplayed().assertIsSelected()
            onNodeWithText("الإنجليزية").assertIsDisplayed().performClick()
            onNodeWithText("English").assertIsDisplayed().assertIsSelected()
            assertEquals(LayoutDirection.Ltr, onNodeWithText("Today's Attendance").fetchSemanticsNode().layoutInfo.layoutDirection)
            onNodeWithText("Today's Treasury").assertExists()
            onNodeWithText("Youssef Ahmed").assertExists()
            onNodeWithText("Grade 12").assertExists()
            onNodeWithText("Overdue").assertExists()
            onNodeWithText("حضور اليوم").assertDoesNotExist()
            onNodeWithText("Recent activity").assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("10234")).assertExists()
            onNodeWithText("Arabic").assertIsDisplayed().performClick()
            onNodeWithText("العربية").assertIsDisplayed().assertIsSelected()
            assertEquals(LayoutDirection.Rtl, attendance.fetchSemanticsNode().layoutInfo.layoutDirection)
        }

    @Test
    fun scannerStudentActionsShortcutsAndSettingsEmitTypedEvents() =
        runComposeUiTest {
            val events = mutableListOf<DashboardEvent>()
            setContent {
                CenterOneTheme {
                    DashboardScreen(DashboardPreviewData.state(AppLanguage.English).copy(query = "10234"), events::add)
                }
            }
            val search = onNode(hasSetTextAction())
            search.performClick().performKeyInput { pressKey(Key.Enter) }
            onNodeWithText("Check-in").performScrollTo().performClick()
            onNodeWithText("Pay subscription").performScrollTo().performClick()
            onNodeWithText("New student").performScrollTo().performClick()
            onNodeWithText("Collect subscription").performScrollTo().performClick()
            onNodeWithText("Record expense").performScrollTo().performClick()
            onNodeWithText("Teachers").performClick()
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText("Reports").assertDoesNotExist()
            onNodeWithText("Schedule").assertDoesNotExist()
            onNodeWithText("Settings").assertDoesNotExist()
            runOnIdle {
                assertEquals(
                    listOf(
                        DashboardEvent.Search,
                        DashboardEvent.RegisterAttendance("10234"),
                        DashboardEvent.PaySubscription("10234"),
                        DashboardEvent.AddStudent,
                        DashboardEvent.CollectSubscription,
                        DashboardEvent.RecordExpense,
                        DashboardEvent.Navigate(MainNavigation.Teachers),
                        DashboardEvent.Navigate(MainNavigation.Settings),
                    ),
                    events,
                )
            }
        }

    @Test
    fun paidStudentAndEmptySearchRenderFromCallerState() =
        runComposeUiTest {
            val state = mutableStateOf(DashboardPreviewData.state(AppLanguage.English))
            setContent { CenterOneTheme { DashboardScreen(state.value, {}) } }
            val paid = DashboardPreviewData.student.copy(paymentStatus = PaymentStatus.Paid)
            runOnIdle { state.value = state.value.copy(student = paid) }
            onNodeWithText("Paid").assertExists()
            onNodeWithText("Overdue").assertDoesNotExist()
            runOnIdle { state.value = state.value.copy(student = null, searchSubmitted = true) }
            onNodeWithText("No matches found.").assertExists()
            onNodeWithText("Check-in").assertDoesNotExist()
        }

    @Test
    fun brandRendersSuppliedPainterWithLocalizedDescription() =
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
            onNodeWithText("Center One").assertDoesNotExist()
            onNodeWithText("1").assertDoesNotExist()
        }
}
