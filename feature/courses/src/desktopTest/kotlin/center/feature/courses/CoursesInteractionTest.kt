package center.feature.courses

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
            onNodeWithContentDescription("خيارات درس فيزياء").performClick()
            runOnIdle { assertEquals(CoursesEvent.GroupMenuChanged(group.id), events.last()) }
            onNodeWithText("تعديل بيانات الدرس").assertDoesNotExist()
            runOnIdle { expanded.value = true }
            onNodeWithText("تعديل بيانات الدرس").performClick()
            runOnIdle { assertEquals(CoursesEvent.EditGroup(group.id), events.last()) }
            onNodeWithText("إيقاف الدرس").performClick()
            runOnIdle { assertEquals(CoursesEvent.DeactivateGroup(group.id), events.last()) }
            onNodeWithText("حذف الدرس").performClick()
            runOnIdle { assertEquals(CoursesEvent.RequestDeleteGroup(group.id), events.last()) }
        }

    @Test
    fun scheduleTimePickerAndDayChipsEmitStableEvents() =
        runComposeUiTest {
            val form = CoursesPreviewData.editForm(CoursesPreviewData.state().groups.first())
            val events = mutableListOf<CoursesEvent>()
            setContent {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { GroupFormModalContent(form, events::add) }
                }
            }
            onNodeWithText("من / From").performScrollTo().performClick()
            onNodeWithText("اختيار").performClick()
            runOnIdle {
                assertEquals(CoursesEvent.FromTimeChanged(form.timeSlots.first().id, "16:00"), events.last())
                assertEquals("16:00", form.timeSlots.first().from)
            }
            onNodeWithContentDescription("حذف الموعد 1").performScrollTo().performClick()
            runOnIdle { assertEquals(CoursesEvent.ToggleScheduleDay(form.timeSlots.first().day!!), events.last()) }
            onNodeWithText("الأربعاء").performScrollTo().performClick()
            runOnIdle { assertEquals(CoursesEvent.ToggleScheduleDay(ScheduleDay.Wednesday), events.last()) }
        }

    @Test
    fun submittingFormDisablesSaveCancelAndScheduleInputs() =
        runComposeUiTest {
            setContent {
                CenterOneTheme {
                    GroupFormModalContent(
                        CoursesPreviewData.editForm(CoursesPreviewData.state().groups.first()).copy(isSubmitting = true),
                        {},
                    )
                }
            }
            onNodeWithText("حفظ الدرس / Save Lesson").performScrollTo().assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
            onNodeWithText("من / From").performScrollTo().assertIsNotEnabled()
            onNodeWithText("السبت").assertIsNotEnabled()
        }

    @Test
    fun deletionRequiresConfirmationAndDisablesActionsWhileDeleting() =
        runComposeUiTest {
            val state = mutableStateOf(DeleteGroupState("group-1", "فيزياء"))
            val events = mutableListOf<CoursesEvent>()
            setContent { CenterOneTheme { DeleteConfirmationModalContent(state.value, events::add) } }
            onNodeWithText("نعم، احذف الدرس").performClick()
            runOnIdle {
                assertEquals(CoursesEvent.ConfirmDeleteGroup, events.last())
                state.value = state.value.copy(isDeleting = true)
            }
            onNodeWithText("نعم، احذف الدرس").assertIsNotEnabled()
            onNodeWithText("إلغاء / Cancel").assertIsNotEnabled()
        }
}
