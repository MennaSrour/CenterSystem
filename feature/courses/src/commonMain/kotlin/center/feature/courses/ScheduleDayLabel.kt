package center.feature.courses

import androidx.compose.runtime.Composable
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage

@Composable
internal fun ScheduleDay.label(): String {
    val key =
        when (this) {
            ScheduleDay.Saturday -> AppStringKey.Saturday
            ScheduleDay.Sunday -> AppStringKey.Sunday
            ScheduleDay.Monday -> AppStringKey.Monday
            ScheduleDay.Tuesday -> AppStringKey.Tuesday
            ScheduleDay.Wednesday -> AppStringKey.Wednesday
            ScheduleDay.Thursday -> AppStringKey.Thursday
            ScheduleDay.Friday -> AppStringKey.Friday
        }
    return AppStrings.get(key, LocalAppLanguage.current)
}
