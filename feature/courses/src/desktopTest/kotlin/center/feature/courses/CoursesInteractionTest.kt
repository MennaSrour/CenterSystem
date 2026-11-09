package center.feature.courses

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
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
class CoursesInteractionTest {
    @Test
    fun kebabAndMenuActionsAreControlledByTheCallerAndUseGroupIds() =
        runComposeUiTest {
            val group = CoursesPreviewData.state().groups.first()
            val expanded = mutableStateOf(false)
            val events = mutableListOf<CoursesEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        CourseCard(group, "أ. محمود حسن", "الثالث الثانوي", expanded.value, events::add)
                    }
                }
            }
            onNodeWithContentDescription("خيارات مجموعة فيزياء").performClick()
            runOnIdle { assertEquals(CoursesEvent.GroupMenuChanged(group.id), events.last()) }
            onNodeWithText("تعديل بيانات المجموعة").assertDoesNotExist()
            runOnIdle { expanded.value = true }
            onNodeWithText("تعديل بيانات المجموعة").performClick()
            runOnIdle { assertEquals(CoursesEvent.EditGroup(group.id), events.last()) }
            onNodeWithText("إيقاف المجموعة").performClick()
            runOnIdle { assertEquals(CoursesEvent.DeactivateGroup(group.id), events.last()) }
            onNodeWithText("حذف المجموعة").performClick()
            runOnIdle { assertEquals(CoursesEvent.RequestDeleteGroup(group.id), events.last()) }
        }

    @Test
    fun scheduleEditsAndAddRemoveActionsEmitStableSlotIds() =
        runComposeUiTest {
            val form = CoursesPreviewData.editForm(CoursesPreviewData.state().groups.first())
            val events = mutableListOf<CoursesEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { GroupFormModalContent(form, events::add) }
                }
            }
            onAllNodes(hasSetTextAction() and hasText("16:00")).onFirst().performScrollTo().performTextReplacement("17:00")
            runOnIdle {
                assertEquals(CoursesEvent.FromTimeChanged(form.timeSlots.first().id, "17:00"), events.last())
                assertEquals("16:00", form.timeSlots.first().from)
            }
            onNodeWithContentDescription("حذف الموعد 1").performScrollTo().performClick()
            runOnIdle { assertEquals(CoursesEvent.RemoveTimeSlot(form.timeSlots.first().id), events.last()) }
            onNodeWithText("+ إضافة موعد").performScrollTo().performClick()
            runOnIdle { assertEquals(CoursesEvent.AddTimeSlot, events.last()) }
        }

    @Test
    fun submittingFormDisablesSaveCancelAndAddingSlots() =
        runComposeUiTest {
            setContent {
                CenterOneTheme {
                    GroupFormModalContent(
                        CoursesPreviewData.editForm(CoursesPreviewData.state().groups.first()).copy(isSubmitting = true),
                        {},
                    )
                }
            }
            onNodeWithText("حفظ المجموعة / Save Group").performScrollTo().assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
            onNodeWithText("+ إضافة موعد").performScrollTo().assertIsNotEnabled()
        }

    @Test
    fun deletionRequiresConfirmationAndDisablesActionsWhileDeleting() =
        runComposeUiTest {
            val state = mutableStateOf(DeleteGroupState("group-1", "فيزياء"))
            val events = mutableListOf<CoursesEvent>()
            setContent { CenterOneTheme { DeleteConfirmationModalContent(state.value, events::add) } }
            onNodeWithText("نعم، احذف المجموعة").performClick()
            runOnIdle {
                assertEquals(CoursesEvent.ConfirmDeleteGroup, events.last())
                state.value = state.value.copy(isDeleting = true)
            }
            onNodeWithText("نعم، احذف المجموعة").assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
        }
}
