package center.feature.financials

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import center.ui.theme.CenterOneTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FinancialsInteractionTest {
    @Test
    fun paymentEditsAndConfirmEmitEventsWithoutChangingTheCallerSnapshot() =
        runComposeUiTest {
            val form = FinancialsPreviewData.receivePayment()
            val events = mutableListOf<FinancialsEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ReceivePaymentModalContent(form, events::add)
                    }
                }
            }
            onNode(hasSetTextAction() and hasText("100")).performScrollTo().performTextReplacement("125")
            runOnIdle {
                assertEquals(FinancialsEvent.PaymentAmountChanged("125"), events.last())
                assertEquals("100", form.amount)
            }
            onNodeWithText("تأكيد التحصيل / Confirm Payment").performScrollTo().performClick()
            runOnIdle { assertEquals(FinancialsEvent.ConfirmReceivePayment, events.last()) }
        }

    @Test
    fun expenseTypeChangesOnlyWhenTheOwnerSuppliesANewSnapshot() =
        runComposeUiTest {
            val state = mutableStateOf(FinancialsPreviewData.recordExpense())
            val events = mutableListOf<FinancialsEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        RecordExpenseModalContent(state.value, events::add)
                    }
                }
            }
            onNodeWithText("مصروفات تشغيل").performClick()
            runOnIdle { assertEquals(FinancialsEvent.ExpenseTypeSelected(ExpenseType.Operational), events.last()) }
            onNodeWithText("اختر المدرس / Select Teacher").assertExists()
            runOnIdle { state.value = state.value.copy(type = ExpenseType.Operational) }
            onNodeWithText("اختر المدرس / Select Teacher").assertDoesNotExist()
        }

    @Test
    fun submittingExpenseDisablesConfirmCancelAndAttachmentActions() =
        runComposeUiTest {
            setContent {
                CenterOneTheme {
                    RecordExpenseModalContent(FinancialsPreviewData.recordExpense().copy(isSubmitting = true), {})
                }
            }
            onNodeWithText("تأكيد الصرف / Confirm Payout").performScrollTo().assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
            onNodeWithText("رفع صورة الإيصال / Attach Receipt").performScrollTo().assertIsNotEnabled()
            onNodeWithText("إغلاق").performScrollTo().assertIsNotEnabled()
        }
}
