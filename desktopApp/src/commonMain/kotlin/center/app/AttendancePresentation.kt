package center.app

import center.domain.model.AttendanceSession
import center.domain.model.GroupScheduleSlot
import center.domain.model.PersonType
import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText
import center.ui.text.isolateLtr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

internal fun LocalTime.hhmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

internal fun Long.localTimeLabel(): String =
    Instant
        .fromEpochMilliseconds(this)
        .toLocalDateTime(Cairo)
        .time
        .hhmm()

internal fun AttendanceSession.summary(groupName: String? = null): LocalizedText {
    val heading = date.toString().isolateLtr() + groupName?.let { " · $it" }.orEmpty()
    val checkIn = checkInTime.localTimeLabel().isolateLtr()
    val checkOut = checkOutTime?.localTimeLabel()?.isolateLtr() ?: "—"
    return LocalizedText(
        "$heading · حضور $checkIn · انصراف $checkOut",
        "$heading · In $checkIn · Out $checkOut",
    )
}

internal fun List<GroupScheduleSlot>.scheduleText(language: AppLanguage): String =
    joinToString(" · ") { slot ->
        val day = slot.day.localizedDay().resolve(language)
        "$day ${"${slot.from.hhmm()}–${slot.to.hhmm()}".isolateLtr()}"
    }

internal fun DayOfWeek.localizedDay(): LocalizedText =
    LocalizedText(
        when (this) {
            DayOfWeek.MONDAY -> "الاثنين"
            DayOfWeek.TUESDAY -> "الثلاثاء"
            DayOfWeek.WEDNESDAY -> "الأربعاء"
            DayOfWeek.THURSDAY -> "الخميس"
            DayOfWeek.FRIDAY -> "الجمعة"
            DayOfWeek.SATURDAY -> "السبت"
            DayOfWeek.SUNDAY -> "الأحد"
        },
        name.lowercase().replaceFirstChar(Char::uppercase),
    )

internal fun PersonType.label(): LocalizedText =
    when (this) {
        PersonType.STUDENT -> LocalizedText("طالب", "Student")
        PersonType.TEACHER -> LocalizedText("مدرس", "Teacher")
        PersonType.STAFF -> LocalizedText("موظف", "Staff")
    }
