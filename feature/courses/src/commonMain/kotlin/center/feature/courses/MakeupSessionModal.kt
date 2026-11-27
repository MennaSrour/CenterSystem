package center.feature.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText

@Composable
fun MakeupSessionModal(
    state: MakeupSessionFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isSubmitting) onEvent(CoursesEvent.DismissModal) }) {
        val language = LocalAppLanguage.current
        CenterOneModalContent(
            title = AppStrings.get(AppStringKey.AddMakeupSession, language),
            subtitle = state.lessonName,
            closeLabel = AppStrings.get(AppStringKey.Close, language),
            onClose = { onEvent(CoursesEvent.DismissModal) },
            modifier = modifier,
            enabled = !state.isSubmitting,
            maxWidth = 560.dp,
        ) {
            MakeupInput(state.makeupDate, AppStringKey.MakeupDate, language) { onEvent(CoursesEvent.MakeupDateChanged(it)) }
            MakeupInput(state.startTime, AppStringKey.StartTime, language) { onEvent(CoursesEvent.MakeupStartTimeChanged(it)) }
            MakeupInput(state.endTime, AppStringKey.EndTime, language) { onEvent(CoursesEvent.MakeupEndTimeChanged(it)) }
            MakeupInput(state.originalCanceledDate, AppStringKey.OriginalCanceledDate, language) {
                onEvent(CoursesEvent.OriginalCanceledDateChanged(it))
            }
            CenterOneTextField(
                value = state.notes,
                onValueChange = { onEvent(CoursesEvent.MakeupNotesChanged(it)) },
                enabled = !state.isSubmitting,
                singleLine = false,
                minLines = 2,
                label = { Text(AppStrings.get(AppStringKey.OptionalNote, language)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                LocalizedText(
                    "استخدم التاريخ بصيغة سنة-شهر-يوم والوقت بصيغة ٢٤ ساعة.",
                    "Use YYYY-MM-DD dates and 24-hour times.",
                ).resolve(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Column(Modifier.fillMaxWidth()) {
                CenterOnePrimaryButton(
                    onClick = { onEvent(CoursesEvent.SubmitMakeupSession) },
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(AppStrings.get(AppStringKey.Save, language))
                }
                CenterOneTextButton(onClick = { onEvent(CoursesEvent.DismissModal) }, enabled = !state.isSubmitting) {
                    Text(AppStrings.get(AppStringKey.Cancel, language))
                }
            }
        }
    }
}

@Composable
private fun MakeupInput(
    value: String,
    label: AppStringKey,
    language: center.ui.localization.AppLanguage,
    onChange: (String) -> Unit,
) {
    CenterOneTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(AppStrings.get(label, language)) },
        modifier = Modifier.fillMaxWidth(),
    )
}
