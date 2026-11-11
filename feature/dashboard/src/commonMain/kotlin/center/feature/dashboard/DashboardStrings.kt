package center.feature.dashboard

import androidx.compose.runtime.Composable
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.localization.StringResources

enum class DashboardText {
    BRAND_AR,
    BRAND_EN,
    MAIN_MENU,
    HOME,
    STUDENTS,
    COURSES,
    SCHEDULE,
    PAYMENTS,
    REPORTS,
    SETTINGS,
    LOGOUT,
    OVERVIEW,
    OVERVIEW_EN,
    DAILY_DASHBOARD,
    WELCOME,
    WELCOME_SUBTITLE,
    SYSTEM_ONLINE,
    SYSTEM_OFFLINE,
    QUICK_CHECK_IN,
    QUICK_CHECK_IN_EN,
    SCAN_DESCRIPTION,
    SCAN_PLACEHOLDER,
    CHECK_IN,
    CHECKING_IN,
    TODAY_STATS,
    TODAY_STATS_EN,
    STATS_SUBTITLE,
    UPDATED_NOW,
    ATTENDANCE,
    REVENUE,
    PENDING_MESSAGES,
    GROUPS,
    TODAY,
    CURRENCY,
    WAITING,
    ACTIVE_NOW,
    RECENT_ACTIVITY,
    RECENT_ACTIVITY_EN,
    ACTIVITY_SUBTITLE,
    VIEW_ALL,
    STUDENT_NAME,
    STUDENT_ID,
    ACTION,
    TIME,
    STATUS,
    CHECK_IN_ACTION,
    COMPLETED,
    PENDING,
    EMPTY_ACTIVITY,
    EMPTY_STATS,
    RETRY,
    LOADING,
    NOTIFICATIONS,
    MANAGER,
    BRANCH,
    CONNECTED,
    LANGUAGE_AR,
    LANGUAGE_EN,
    DEMO_USER,
    DEMO_DATE,
    CHECK_IN_UNAVAILABLE,
    FEATURE_UNAVAILABLE,
    ALL_ACTIVITY_MESSAGE,
}

private val strings =
    StringResources(
        mapOf(
            DashboardText.BRAND_AR to
                LocalizedText(
                    "سنتر ون",
                    "Center One",
                ),
            DashboardText.BRAND_EN to
                LocalizedText(
                    "سنتر ون",
                    "Center One",
                ),
            DashboardText.MAIN_MENU to
                LocalizedText(
                    "القائمة الرئيسية",
                    "Main menu",
                ),
            DashboardText.HOME to
                LocalizedText(
                    "الرئيسية",
                    "Home",
                ),
            DashboardText.STUDENTS to
                LocalizedText(
                    "الطلاب",
                    "Students",
                ),
            DashboardText.COURSES to
                LocalizedText(
                    "الدورات",
                    "Courses",
                ),
            DashboardText.SCHEDULE to
                LocalizedText(
                    "الجدول",
                    "Schedule",
                ),
            DashboardText.PAYMENTS to
                LocalizedText(
                    "المدفوعات",
                    "Payments",
                ),
            DashboardText.REPORTS to
                LocalizedText(
                    "التقارير",
                    "Reports",
                ),
            DashboardText.SETTINGS to
                LocalizedText(
                    "الإعدادات",
                    "Settings",
                ),
            DashboardText.LOGOUT to
                LocalizedText(
                    "تسجيل الخروج",
                    "Log out",
                ),
            DashboardText.OVERVIEW to
                LocalizedText(
                    "نظرة عامة",
                    "Overview",
                ),
            DashboardText.OVERVIEW_EN to
                LocalizedText(
                    "نظرة عامة",
                    "Overview",
                ),
            DashboardText.DAILY_DASHBOARD to
                LocalizedText(
                    "لوحة التحكم اليومية",
                    "Daily dashboard",
                ),
            DashboardText.WELCOME to
                LocalizedText(
                    "مرحباً، %1\$s",
                    "Welcome, %1\$s",
                ),
            DashboardText.WELCOME_SUBTITLE to
                LocalizedText(
                    "إليك ملخص سريع لما يحدث في المركز اليوم.",
                    "Here is a quick summary of what is happening at the center today.",
                ),
            DashboardText.SYSTEM_ONLINE to
                LocalizedText(
                    "النظام يعمل بكفاءة",
                    "System is running normally",
                ),
            DashboardText.SYSTEM_OFFLINE to
                LocalizedText(
                    "النظام غير متصل",
                    "System is offline",
                ),
            DashboardText.QUICK_CHECK_IN to
                LocalizedText(
                    "تسجيل حضور سريع",
                    "Quick check-in",
                ),
            DashboardText.QUICK_CHECK_IN_EN to
                LocalizedText(
                    "تسجيل حضور سريع",
                    "Quick check-in",
                ),
            DashboardText.SCAN_DESCRIPTION to
                LocalizedText(
                    "امسح باركود الطالب أو أدخل رقم التعريف",
                    "Scan a student barcode or enter their ID",
                ),
            DashboardText.SCAN_PLACEHOLDER to
                LocalizedText(
                    "امسح الكود أو أدخل رقم الطالب",
                    "Scan a barcode or enter a student ID",
                ),
            DashboardText.CHECK_IN to
                LocalizedText(
                    "تسجيل الحضور",
                    "Check in",
                ),
            DashboardText.CHECKING_IN to
                LocalizedText(
                    "جارٍ التسجيل…",
                    "Checking in…",
                ),
            DashboardText.TODAY_STATS to
                LocalizedText(
                    "إحصائيات اليوم",
                    "Today’s statistics",
                ),
            DashboardText.TODAY_STATS_EN to
                LocalizedText(
                    "إحصائيات اليوم",
                    "Today’s statistics",
                ),
            DashboardText.STATS_SUBTITLE to
                LocalizedText(
                    "تحديث مباشر لبيانات المركز",
                    "Live center statistics",
                ),
            DashboardText.UPDATED_NOW to
                LocalizedText(
                    "تم التحديث الآن",
                    "Updated just now",
                ),
            DashboardText.ATTENDANCE to
                LocalizedText(
                    "الحضور",
                    "Attendance",
                ),
            DashboardText.REVENUE to
                LocalizedText(
                    "التحصيلات",
                    "Revenue",
                ),
            DashboardText.PENDING_MESSAGES to
                LocalizedText(
                    "رسائل معلقة",
                    "Pending messages",
                ),
            DashboardText.GROUPS to
                LocalizedText(
                    "المجموعات",
                    "Groups",
                ),
            DashboardText.TODAY to
                LocalizedText(
                    "اليوم",
                    "Today",
                ),
            DashboardText.CURRENCY to
                LocalizedText(
                    "جنيه مصري",
                    "EGP",
                ),
            DashboardText.WAITING to
                LocalizedText(
                    "بانتظار الاتصال",
                    "Waiting for connection",
                ),
            DashboardText.ACTIVE_NOW to
                LocalizedText(
                    "نشطة الآن",
                    "Active now",
                ),
            DashboardText.RECENT_ACTIVITY to
                LocalizedText(
                    "آخر النشاطات",
                    "Recent activity",
                ),
            DashboardText.RECENT_ACTIVITY_EN to
                LocalizedText(
                    "آخر النشاطات",
                    "Recent activity",
                ),
            DashboardText.ACTIVITY_SUBTITLE to
                LocalizedText(
                    "أحدث عمليات تسجيل الحضور",
                    "Latest attendance check-ins",
                ),
            DashboardText.VIEW_ALL to
                LocalizedText(
                    "عرض الكل",
                    "View all",
                ),
            DashboardText.STUDENT_NAME to
                LocalizedText(
                    "اسم الطالب",
                    "Student name",
                ),
            DashboardText.STUDENT_ID to
                LocalizedText(
                    "الكود",
                    "ID",
                ),
            DashboardText.ACTION to
                LocalizedText(
                    "العملية",
                    "Action",
                ),
            DashboardText.TIME to
                LocalizedText(
                    "الوقت",
                    "Time",
                ),
            DashboardText.STATUS to
                LocalizedText(
                    "الحالة",
                    "Status",
                ),
            DashboardText.CHECK_IN_ACTION to
                LocalizedText(
                    "تسجيل حضور",
                    "Check-in",
                ),
            DashboardText.COMPLETED to
                LocalizedText(
                    "تمت",
                    "Completed",
                ),
            DashboardText.PENDING to
                LocalizedText(
                    "معلقة",
                    "Pending",
                ),
            DashboardText.EMPTY_ACTIVITY to
                LocalizedText(
                    "لا توجد نشاطات حتى الآن",
                    "No activity yet",
                ),
            DashboardText.EMPTY_STATS to
                LocalizedText(
                    "لا توجد إحصائيات متاحة",
                    "No statistics available",
                ),
            DashboardText.RETRY to
                LocalizedText(
                    "إعادة المحاولة",
                    "Retry",
                ),
            DashboardText.LOADING to
                LocalizedText(
                    "جارٍ تحميل لوحة التحكم…",
                    "Loading dashboard…",
                ),
            DashboardText.NOTIFICATIONS to
                LocalizedText(
                    "الإشعارات",
                    "Notifications",
                ),
            DashboardText.MANAGER to
                LocalizedText(
                    "مديرة المركز",
                    "Center manager",
                ),
            DashboardText.BRANCH to
                LocalizedText(
                    "فرع حصة",
                    "Gamasa branch",
                ),
            DashboardText.CONNECTED to
                LocalizedText(
                    "متصل الآن",
                    "Connected",
                ),
            DashboardText.LANGUAGE_AR to
                LocalizedText(
                    "عربي",
                    "عربي",
                ),
            DashboardText.LANGUAGE_EN to
                LocalizedText(
                    "English",
                    "English",
                ),
            DashboardText.DEMO_USER to
                LocalizedText(
                    "نور أحمد",
                    "Nour Ahmed",
                ),
            DashboardText.DEMO_DATE to
                LocalizedText(
                    "عرض تجريبي — بيانات توضيحية",
                    "Demo — illustrative data",
                ),
            DashboardText.CHECK_IN_UNAVAILABLE to
                LocalizedText(
                    "تسجيل الحضور غير متاح في العرض التجريبي. لم يتم حفظ أي حضور.",
                    "Check-in is unavailable in this demo. No attendance was saved.",
                ),
            DashboardText.FEATURE_UNAVAILABLE to
                LocalizedText(
                    "هذه الوظيفة غير متاحة بعد.",
                    "This feature is not available yet.",
                ),
            DashboardText.ALL_ACTIVITY_MESSAGE to
                LocalizedText(
                    "تُعرض كل النشاطات التجريبية بالفعل.",
                    "All demo activities are already displayed.",
                ),
        ),
    )

fun DashboardText.text(
    language: AppLanguage,
    vararg arguments: String,
): String = strings.get(this, language, *arguments)

@Composable
internal fun dashboardString(
    key: DashboardText,
    vararg arguments: String,
): String = key.text(LocalAppLanguage.current, *arguments)
