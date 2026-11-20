package center.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText

@Composable
fun AttendanceHistory(entries: List<LocalizedText>) {
    val language = LocalAppLanguage.current
    Text(
        LocalizedText("سجل الحضور والانصراف", "Recent check-ins and check-outs").resolve(language),
        style = MaterialTheme.typography.titleSmall,
    )
    if (entries.isEmpty()) {
        Text(LocalizedText("لا توجد سجلات بعد", "No attendance records yet").resolve(language))
    } else {
        entries.forEach { Text(it.resolve(language), style = MaterialTheme.typography.bodySmall) }
    }
}
