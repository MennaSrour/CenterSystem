package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTable
import center.ui.components.CenterOneTableColumn
import center.ui.components.CenterOneTextField
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.Navy

internal enum class OverdueText(
    private val text: LocalizedText,
) {
    Tab(LocalizedText("المتأخرات", "Overdue")),
    Search(LocalizedText("ابحث بالاسم أو الكود أو الدرس أو المدرس", "Search by student, code, lesson, or teacher")),
    Student(LocalizedText("الطالب والكود", "Student & code")),
    Group(LocalizedText("الدرس والمدرس", "Lesson & teacher")),
    Phone(LocalizedText("هاتف ولي الأمر", "Parent phone")),
    Amount(LocalizedText("المبلغ المستحق", "Amount due")),
    Actions(LocalizedText("الإجراءات", "Actions")),
    Remind(LocalizedText("إرسال تذكير", "Send Reminder")),
    RemindAll(LocalizedText("إرسال تذكير للكل", "Remind All")),
    PayNow(LocalizedText("دفع الآن", "Pay Now")),
    Empty(LocalizedText("لا توجد اشتراكات متأخرة مطابقة.", "No matching overdue payments.")),
    Currency(LocalizedText("ج.م", "EGP")),
    ;

    fun label(language: AppLanguage): String = text.resolve(language)
}

@Composable
internal fun OverdueActions(
    query: String,
    rows: List<OverdueStudentRow>,
    onEvent: (StudentsEvent) -> Unit,
    enabled: Boolean = true,
) {
    val language = LocalAppLanguage.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CenterOneTextField(
            value = query,
            onValueChange = { onEvent(StudentsEvent.SearchChanged(it)) },
            modifier = Modifier.weight(1f),
            placeholder = { Text(OverdueText.Search.label(language)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        CenterOneOutlinedButton(
            onClick = { onEvent(StudentsEvent.RemindAll(rows.map { it.student.id }.distinct())) },
            enabled = enabled && rows.isNotEmpty(),
            color = Navy,
        ) {
            ReminderLabel(OverdueText.RemindAll.label(language))
        }
    }
}

@Composable
internal fun OverdueStudentsTable(
    rows: List<OverdueStudentRow>,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
    remindersEnabled: Boolean = true,
) {
    val language = LocalAppLanguage.current
    val columns =
        listOf(
            CenterOneTableColumn(OverdueText.Student.label(language), 1.25f),
            CenterOneTableColumn(OverdueText.Group.label(language), 1.45f),
            CenterOneTableColumn(OverdueText.Phone.label(language), 1.1f),
            CenterOneTableColumn(OverdueText.Amount.label(language), 0.85f),
            CenterOneTableColumn(OverdueText.Actions.label(language), 2.15f),
        )
    CenterOneTable(columns, rows, { it.payment.id }, OverdueText.Empty.label(language), modifier) { (student, payment), column ->
        when (column) {
            0 ->
                Column {
                    Text(
                        if (language ==
                            AppLanguage.Arabic
                        ) {
                            student.name
                        } else {
                            student.englishName
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LtrTableValue(student.code)
                }
            1 ->
                Column {
                    Text(payment.group.resolve(language), style = MaterialTheme.typography.titleSmall)
                    Text(
                        payment.teacher.resolve(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            2 -> LtrTableValue(student.parentPhone)
            3 -> {
                val whole =
                    (payment.remainingMinor / 100)
                        .toString()
                        .reversed()
                        .chunked(3)
                        .joinToString(",")
                        .reversed()
                val fraction = payment.remainingMinor % 100
                val amount = if (fraction == 0L) whole else "$whole.${fraction.toString().padStart(2, '0')}"
                Text("$amount ${OverdueText.Currency.label(language)}", style = MaterialTheme.typography.titleSmall)
            }
            4 ->
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CenterOneOutlinedButton(
                        onClick = { onEvent(StudentsEvent.SendReminder(student.id)) },
                        enabled = remindersEnabled,
                        color = Navy,
                    ) {
                        ReminderLabel(OverdueText.Remind.label(language))
                    }
                    CenterOnePrimaryButton(onClick = { onEvent(StudentsEvent.PayNow(payment.id)) }) {
                        Text(OverdueText.PayNow.label(language))
                    }
                }
        }
    }
}

@Composable
private fun LtrTableValue(value: String) {
    Text(value, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
}

@Composable
private fun ReminderLabel(label: String) {
    Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
    Text(label, Modifier.padding(start = 6.dp))
}
