package center.feature.financials

/** Explicit UI fixtures. No sample record represents a saved transaction. */
object FinancialsPreviewData {
    fun state() =
        FinancialsState(
            summary = FinancialSummary("12,500", "4,200", "8,300", "15,000"),
            userName = "نور أحمد",
            dateLabel = "عرض تجريبي — بيانات توضيحية",
            incomeRecords =
                listOf(
                    FinancialRecord("in-1", "يوسف أحمد", "Youssef Ahmed", "اشتراك كيمياء — أكتوبر", "+ 150 ج.م", "12 أكتوبر 2026", "10:42"),
                    FinancialRecord("in-2", "مريم خالد", "Mariam Khaled", "اشتراك فيزياء — أكتوبر", "+ 200 ج.م", "12 أكتوبر 2026", "10:18"),
                    FinancialRecord("in-3", "أحمد محمد", "Ahmed Mohamed", "حصة رياضيات", "+ 100 ج.م", "11 أكتوبر 2026", "18:55"),
                    FinancialRecord(
                        "in-4",
                        "سارة محمود",
                        "Sara Mahmoud",
                        "اشتراك اللغة الإنجليزية",
                        "+ 175 ج.م",
                        "11 أكتوبر 2026",
                        "17:40",
                    ),
                ),
            expenseRecords =
                listOf(
                    FinancialRecord(
                        "out-1",
                        "تسوية أ. محمود",
                        "Mr. Mahmoud payout",
                        "تسوية حساب مدرس",
                        "− 2,800 ج.م",
                        "12 أكتوبر 2026",
                        "12:15",
                    ),
                    FinancialRecord("out-2", "كهرباء القاعة", "Hall electricity", "مصروفات تشغيل", "− 500 ج.م", "12 أكتوبر 2026", "09:30"),
                    FinancialRecord(
                        "out-3",
                        "مستلزمات نظافة",
                        "Cleaning supplies",
                        "مصروفات تشغيل",
                        "− 300 ج.م",
                        "11 أكتوبر 2026",
                        "16:20",
                    ),
                ),
        )

    val students = listOf(StudentOption("student-1", "يوسف أحمد", "10234"), StudentOption("student-2", "يوسف محمود", "10811"))
    val classes = listOf(ClassOption("class-1", "فيزياء ٣ ثانوي — أ. محمود"), ClassOption("class-2", "كيمياء ٣ ثانوي — أ. كريم"))
    val teachers =
        listOf(TeacherOption("teacher-1", "أ. كريم عادل / Karim Adel", "4,000 ج.م"), TeacherOption("teacher-2", "أ. محمود", "2,800 ج.م"))

    fun receivePayment() =
        ReceivePaymentState(
            studentQuery = "يوسف",
            studentResults = students,
            selectedStudentId = students.first().id,
            classes = classes,
            selectedClassId = classes.first().id,
            split = FinancialSplit("100 ج.م", "70 ج.م", "30 ج.م", "70 / 30"),
            amount = "100",
            canConfirm = true,
        )

    fun recordExpense(type: ExpenseType = ExpenseType.TeacherPayout) =
        RecordExpenseState(
            type = type,
            teachers = teachers,
            selectedTeacherId = teachers.first().id,
            amount = "1000",
            notes = "تسوية مستحقات شهر أكتوبر",
            canConfirm = true,
        )
}
