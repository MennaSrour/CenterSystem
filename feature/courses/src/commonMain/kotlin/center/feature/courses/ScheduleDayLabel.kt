package center.feature.courses

import androidx.compose.runtime.Composable
import center.feature.courses.resources.Res
import center.feature.courses.resources.friday
import center.feature.courses.resources.monday
import center.feature.courses.resources.saturday
import center.feature.courses.resources.sunday
import center.feature.courses.resources.thursday
import center.feature.courses.resources.tuesday
import center.feature.courses.resources.wednesday
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ScheduleDay.label(): String =
    stringResource(
        when (this) {
            ScheduleDay.Saturday -> Res.string.saturday
            ScheduleDay.Sunday -> Res.string.sunday
            ScheduleDay.Monday -> Res.string.monday
            ScheduleDay.Tuesday -> Res.string.tuesday
            ScheduleDay.Wednesday -> Res.string.wednesday
            ScheduleDay.Thursday -> Res.string.thursday
            ScheduleDay.Friday -> Res.string.friday
        },
    )
