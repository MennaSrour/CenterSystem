package center.feature.dashboard

import androidx.compose.runtime.Composable
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText

enum class DashboardText(
    private val label: LocalizedText,
) {
    DEMO(LocalizedText("بيانات تجريبية", "Demo data")),
    ATTENDANCE(LocalizedText("حضور اليوم", "Today's Attendance")),
    TREASURY(LocalizedText("خزنة اليوم", "Today's Treasury")),
    CURRENCY(LocalizedText("ج.م", "EGP")),
    SEARCH_HEADING(LocalizedText("البحث عن طالب", "Find a student")),
    SEARCH_HINT(LocalizedText("اسم الطالب أو رقمه أو امسح الباركود", "Student name, ID, or scan a barcode")),
    SEARCH(LocalizedText("بحث", "Search")),
    NO_STUDENT(LocalizedText("لم يتم العثور على طالب.", "No student found.")),
    PAID(LocalizedText("مدفوع", "Paid")),
    OVERDUE(LocalizedText("متأخر السداد", "Overdue")),
    CHECK_IN(LocalizedText("تسجيل حضور", "Register attendance")),
    PAY_SUBSCRIPTION(LocalizedText("دفع اشتراك", "Pay subscription")),
    ADD_STUDENT(LocalizedText("طالب جديد", "New student")),
    COLLECT_SUBSCRIPTION(LocalizedText("تحصيل اشتراك", "Collect subscription")),
    RECORD_EXPENSE(LocalizedText("تسجيل مصروف", "Record expense")),
    ATTENDANCE_DEMO(LocalizedText("تسجيل حضور تجريبي. لم يُحفظ الحضور.", "Demo check-in. Attendance was not saved.")),
    FEATURE_UNAVAILABLE(LocalizedText("هذه الصفحة غير متاحة بعد.", "This page is not available yet.")),
    ;

    fun text(language: AppLanguage): String = label.resolve(language)
}

@Composable
internal fun dashboardString(key: DashboardText): String = key.text(LocalAppLanguage.current)
