package center.ui.localization

data class LocalizedText(
    val arabic: String,
    val english: String,
) {
    fun resolve(language: AppLanguage): String =
        when (language) {
            AppLanguage.Arabic -> arabic
            AppLanguage.English -> english
        }
}

/** Portable map catalog. Missing keys fail visibly; arguments are inserted without interpreting their contents. */
class StringResources<Key>(
    private val entries: Map<Key, LocalizedText>,
) {
    fun get(
        key: Key,
        language: AppLanguage,
        vararg arguments: String,
    ): String {
        val template = entries.getValue(key).resolve(language)
        return Regex("%([1-9][0-9]*)[${'$'}]s").replace(template) { match ->
            val index = match.groupValues[1].toInt() - 1
            require(index in arguments.indices) { "Missing string argument ${index + 1} for $key" }
            arguments[index]
        }
    }
}

/** Shared dictionary for labels that are used across feature modules. */
enum class AppStringKey {
    Save,
    Cancel,
    Close,
    Delete,
    Edit,
    Search,
    Logout,
    Name,
    Phone,
    Role,
    Admin,
    Reception,
    Student,
    Students,
    Teacher,
    Teachers,
    Staff,
    Lessons,
    Lesson,
    Arabic,
    ArabicLanguage,
    EnglishLanguage,
    Languages,
    Mixed,
    Private,
    Pending,
    Synced,
    Failed,
    Paid,
    Overdue,
    Partial,
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday,
    Currency,
    Today,
    Error,
    Retry,
    Copy,
    Print,
    Treasury,
    Settings,
    ExportPdf,
    MonthlyReport,
    MakeupSession,
    AddMakeupSession,
    MakeupDate,
    OriginalCanceledDate,
    OptionalNote,
    StartTime,
    EndTime,
    ScheduledSessions,
    MakeupSessionsCount,
    RescheduledDates,
    FeatureUnavailable,
    BillingWorkspace,
    ReceptionWorkspace,
    Username,
    Login,
    LoggingIn,
    Password,
    Hide,
    Show,
    ManageAccounts,
    AddUser,
    ChangePassword,
    DisplayName,
    PasswordLengthHint,
    AccountSaveError,
    VoidPayment,
    ConfirmVoidPayment,
    VoidReason,
    Voided,
    PrintReceipt,
    PrintRoster,
    PrintTeacherStatement,
    CloseOpenCheckIns,
}

object AppStrings {
    private val dictionary =
        mapOf(
            AppStringKey.Save to LocalizedText("حفظ", "Save"),
            AppStringKey.Cancel to LocalizedText("إلغاء", "Cancel"),
            AppStringKey.Close to LocalizedText("إغلاق", "Close"),
            AppStringKey.Delete to LocalizedText("حذف", "Delete"),
            AppStringKey.Edit to LocalizedText("تعديل", "Edit"),
            AppStringKey.Search to LocalizedText("بحث", "Search"),
            AppStringKey.Logout to LocalizedText("تسجيل الخروج", "Log out"),
            AppStringKey.Name to LocalizedText("الاسم", "Name"),
            AppStringKey.Phone to LocalizedText("رقم الهاتف", "Phone number"),
            AppStringKey.Role to LocalizedText("الدور الوظيفي", "Role"),
            AppStringKey.Admin to LocalizedText("مدير", "Admin"),
            AppStringKey.Reception to LocalizedText("استقبال", "Reception"),
            AppStringKey.Student to LocalizedText("طالب", "Student"),
            AppStringKey.Students to LocalizedText("الطلاب", "Students"),
            AppStringKey.Teacher to LocalizedText("مدرس", "Teacher"),
            AppStringKey.Teachers to LocalizedText("المدرسون", "Teachers"),
            AppStringKey.Staff to LocalizedText("موظف", "Staff member"),
            AppStringKey.Lessons to LocalizedText("الدروس", "Lessons"),
            AppStringKey.Lesson to LocalizedText("الدرس", "Lesson"),
            AppStringKey.Arabic to LocalizedText("عربي", "Arabic"),
            AppStringKey.ArabicLanguage to LocalizedText("العربية", "Arabic"),
            AppStringKey.EnglishLanguage to LocalizedText("الإنجليزية", "English"),
            AppStringKey.Languages to LocalizedText("لغات", "Languages"),
            AppStringKey.Mixed to LocalizedText("مشترك عربي ولغات", "Arabic and languages"),
            AppStringKey.Private to LocalizedText("برايفت", "Private"),
            AppStringKey.Pending to LocalizedText("قيد الانتظار", "Pending"),
            AppStringKey.Synced to LocalizedText("تمت المزامنة", "Synced"),
            AppStringKey.Failed to LocalizedText("فشل", "Failed"),
            AppStringKey.Paid to LocalizedText("مدفوع", "Paid"),
            AppStringKey.Overdue to LocalizedText("متأخر السداد", "Overdue"),
            AppStringKey.Partial to LocalizedText("مدفوع جزئياً", "Partially paid"),
            AppStringKey.Monday to LocalizedText("الاثنين", "Monday"),
            AppStringKey.Tuesday to LocalizedText("الثلاثاء", "Tuesday"),
            AppStringKey.Wednesday to LocalizedText("الأربعاء", "Wednesday"),
            AppStringKey.Thursday to LocalizedText("الخميس", "Thursday"),
            AppStringKey.Friday to LocalizedText("الجمعة", "Friday"),
            AppStringKey.Saturday to LocalizedText("السبت", "Saturday"),
            AppStringKey.Sunday to LocalizedText("الأحد", "Sunday"),
            AppStringKey.Currency to LocalizedText("ج.م", "EGP"),
            AppStringKey.Today to LocalizedText("اليوم", "Today"),
            AppStringKey.Error to LocalizedText("حدث خطأ", "An error occurred"),
            AppStringKey.Retry to LocalizedText("إعادة المحاولة", "Try again"),
            AppStringKey.Copy to LocalizedText("نسخ", "Copy"),
            AppStringKey.Print to LocalizedText("طباعة", "Print"),
            AppStringKey.Treasury to LocalizedText("الخزنة", "Treasury"),
            AppStringKey.Settings to LocalizedText("الإعدادات", "Settings"),
            AppStringKey.ExportPdf to LocalizedText("تصدير PDF", "Export PDF"),
            AppStringKey.MonthlyReport to LocalizedText("التقرير الشهري", "Monthly report"),
            AppStringKey.MakeupSession to LocalizedText("تعويضية", "Make-up"),
            AppStringKey.AddMakeupSession to LocalizedText("إضافة حصة تعويضية", "Add Make-up Session"),
            AppStringKey.MakeupDate to LocalizedText("تاريخ الحصة التعويضية", "Make-up Date"),
            AppStringKey.OriginalCanceledDate to LocalizedText("تاريخ الحصة الملغاة", "Original Canceled Date"),
            AppStringKey.OptionalNote to LocalizedText("ملاحظة اختيارية", "Optional Note"),
            AppStringKey.StartTime to LocalizedText("وقت البداية", "Start Time"),
            AppStringKey.EndTime to LocalizedText("وقت النهاية", "End Time"),
            AppStringKey.ScheduledSessions to LocalizedText("جلسات الدروس هذا الشهر: %1${'$'}s", "Lesson sessions this month: %1${'$'}s"),
            AppStringKey.MakeupSessionsCount to
                LocalizedText("الحصص التعويضية هذا الشهر: %1${'$'}s", "Make-up sessions this month: %1${'$'}s"),
            AppStringKey.RescheduledDates to LocalizedText("تواريخ تم تعويضها: %1${'$'}s", "Rescheduled dates: %1${'$'}s"),
            AppStringKey.FeatureUnavailable to LocalizedText("هذه الوظيفة غير متاحة بعد.", "This feature is not available yet."),
            AppStringKey.BillingWorkspace to LocalizedText("مساحة الفواتير", "Billing workspace"),
            AppStringKey.ReceptionWorkspace to LocalizedText("مساحة الاستقبال", "Reception workspace"),
            AppStringKey.Username to LocalizedText("اسم المستخدم", "Username"),
            AppStringKey.Login to LocalizedText("تسجيل الدخول", "Sign in"),
            AppStringKey.LoggingIn to LocalizedText("جارٍ تسجيل الدخول…", "Signing in…"),
            AppStringKey.Password to LocalizedText("كلمة المرور", "Password"),
            AppStringKey.Hide to LocalizedText("إخفاء", "Hide"),
            AppStringKey.Show to LocalizedText("إظهار", "Show"),
            AppStringKey.ManageAccounts to LocalizedText("إدارة الحسابات", "Manage Users"),
            AppStringKey.AddUser to LocalizedText("إضافة مستخدم", "Add user"),
            AppStringKey.ChangePassword to LocalizedText("تغيير كلمة المرور", "Change password"),
            AppStringKey.DisplayName to LocalizedText("الاسم", "Display name"),
            AppStringKey.PasswordLengthHint to LocalizedText("كلمة المرور من ٤ إلى ٢٥٦ حرفًا.", "Use 4–256 characters."),
            AppStringKey.AccountSaveError to
                LocalizedText(
                    "تعذر الحفظ. تحقق من البيانات وتفرّد اسم المستخدم.",
                    "Could not save. Check the details and use a unique username.",
                ),
            AppStringKey.VoidPayment to LocalizedText("إلغاء / استرداد دفعة", "Void / Refund Payment"),
            AppStringKey.ConfirmVoidPayment to LocalizedText("تأكيد إلغاء الدفعة؟", "Confirm voiding this payment?"),
            AppStringKey.VoidReason to LocalizedText("سبب الإلغاء (اختياري)", "Reason (optional)"),
            AppStringKey.Voided to LocalizedText("ملغاة", "Voided"),
            AppStringKey.PrintReceipt to LocalizedText("طباعة إيصال", "Print receipt"),
            AppStringKey.PrintRoster to LocalizedText("طباعة كشف طلاب الدرس", "Print lesson roster"),
            AppStringKey.PrintTeacherStatement to LocalizedText("طباعة كشف حساب المدرس", "Print teacher statement"),
            AppStringKey.CloseOpenCheckIns to LocalizedText("إغلاق الحضور المفتوح", "Close Today's Open Check-Ins"),
        )

    val entries: Map<AppStringKey, LocalizedText> get() = dictionary

    val featureEntries: Map<String, LocalizedText> get() = FeatureResourceStrings.entries

    fun text(key: AppStringKey): LocalizedText = dictionary.getValue(key)

    fun get(
        key: AppStringKey,
        language: AppLanguage,
        vararg arguments: String,
    ): String {
        val template = text(key).resolve(language)
        return Regex("%([1-9][0-9]*)" + '\\' + '$' + "[sd]").replace(template) { match ->
            val index = match.groupValues[1].toInt() - 1
            require(index in arguments.indices) { "Missing string argument ${index + 1} for $key" }
            arguments[index]
        }
    }

    fun getResource(
        module: String,
        key: String,
        language: AppLanguage,
        vararg arguments: Any,
    ): String {
        val resourceKey = "$module:$key"
        val template = FeatureResourceStrings.entries.getValue(resourceKey).resolve(language)
        return Regex("%([1-9][0-9]*)" + '\\' + '$' + "[sd]").replace(template) { match ->
            val index = match.groupValues[1].toInt() - 1
            require(index in arguments.indices) { "Missing string argument ${index + 1} for $resourceKey" }
            arguments[index].toString()
        }
    }
}
