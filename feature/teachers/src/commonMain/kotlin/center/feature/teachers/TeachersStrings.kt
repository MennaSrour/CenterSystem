package center.feature.teachers

import androidx.compose.runtime.Composable
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.localization.StringResources

internal enum class TeacherText {
    Demo,
    Search,
    Add,
    NoResults,
    GroupsCount,
    DeficitBadge,
    SalaryDue,
    MonthlySalary,
    PaySalary,
    DemoPaid,
    NoSalary,
    Details,
    Close,
    Phone,
    Groups,
    NoGroups,
    SalaryPolicy,
    DeficitTitle,
    DeficitDescription,
    AddHint,
    Name,
    Subject,
    Notes,
    Save,
    Currency,
}

private val strings =
    StringResources(
        mapOf(
            TeacherText.Demo to LocalizedText("بيانات تجريبية", "Demo data"),
            TeacherText.Search to LocalizedText("ابحث بالاسم أو المادة أو رقم الهاتف", "Search by name, subject, or phone"),
            TeacherText.Add to LocalizedText("إضافة مدرس", "Add teacher"),
            TeacherText.NoResults to LocalizedText("لا يوجد مدرسون مطابقون.", "No matching teachers."),
            TeacherText.GroupsCount to LocalizedText("عدد المجموعات: %1\$s", "Groups: %1\$s"),
            TeacherText.DeficitBadge to LocalizedText("عجز تحصيل: %1\$s", "Collection deficit: %1\$s"),
            TeacherText.SalaryDue to LocalizedText("المرتب الشهري المستحق", "Monthly salary due"),
            TeacherText.MonthlySalary to LocalizedText("المرتب الشهري", "Monthly salary"),
            TeacherText.PaySalary to LocalizedText("صرف المرتب", "Pay Salary"),
            TeacherText.DemoPaid to LocalizedText("تم الصرف في العرض التجريبي", "Paid in demo"),
            TeacherText.NoSalary to LocalizedText("لم يُحدد المرتب بعد", "Salary pending setup"),
            TeacherText.Details to LocalizedText("بيانات المدرس", "Teacher details"),
            TeacherText.Close to LocalizedText("إغلاق", "Close"),
            TeacherText.Phone to LocalizedText("رقم الهاتف", "Phone"),
            TeacherText.Groups to LocalizedText("المجموعات المسندة", "Assigned groups"),
            TeacherText.NoGroups to LocalizedText("لا توجد مجموعات مسندة.", "No assigned groups."),
            TeacherText.SalaryPolicy to
                LocalizedText(
                    "يُصرف المرتب الشهري كاملاً حتى لو تأخر الطلاب في الدفع.",
                    "The full monthly salary is paid regardless of student payment delays.",
                ),
            TeacherText.DeficitTitle to LocalizedText("عجز التحصيل · للمتابعة فقط", "Collection deficit · Staff follow-up only"),
            TeacherText.DeficitDescription to
                LocalizedText(
                    "عدد الطلاب الذين لم يدفعوا اشتراك هذا الشهر: %1\$s، بإجمالي %2\$s. لا يُخصم هذا المبلغ من مرتب المدرس.",
                    "Students with unpaid subscriptions this month: %1\$s, totaling %2\$s. This amount does not reduce the teacher’s salary.",
                ),
            TeacherText.AddHint to LocalizedText("أدخل الاسم ورقم الهاتف والمادة.", "Enter the teacher’s name, phone, and subject."),
            TeacherText.Name to LocalizedText("الاسم", "Name"),
            TeacherText.Subject to LocalizedText("المادة", "Subject"),
            TeacherText.Notes to LocalizedText("ملاحظات (اختياري)", "Notes (optional)"),
            TeacherText.Save to LocalizedText("حفظ المدرس", "Save teacher"),
            TeacherText.Currency to LocalizedText("%1\$s ج.م", "%1\$s EGP"),
        ),
    )

@Composable
internal fun teacherString(
    key: TeacherText,
    vararg arguments: String,
): String = strings.get(key, LocalAppLanguage.current, *arguments)

@Composable
internal fun formatEgp(egp: Long): String {
    val grouped =
        egp
            .toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return teacherString(TeacherText.Currency, grouped)
}

@Composable
internal fun deficitBadge(count: Int): String {
    val students =
        when (LocalAppLanguage.current) {
            AppLanguage.Arabic -> if (count in 3..10) "$count طلاب" else "$count طالب"
            AppLanguage.English -> if (count == 1) "1 student" else "$count students"
        }
    return teacherString(TeacherText.DeficitBadge, students)
}
