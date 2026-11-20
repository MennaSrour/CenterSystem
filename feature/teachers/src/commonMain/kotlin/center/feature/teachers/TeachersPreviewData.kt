package center.feature.teachers

import center.ui.localization.LocalizedText

object TeachersPreviewData {
    fun state() =
        TeachersState(
            teachers =
                listOf(
                    Teacher(
                        id = "1",
                        name = LocalizedText("كريم عادل", "Karim Adel"),
                        phone = "01000001001",
                        subject = LocalizedText("الفيزياء", "Physics"),
                        monthlySalaryMinor = 4000_00,
                        groups =
                            listOf(
                                TeacherGroup(
                                    LocalizedText("الثالث الثانوي · المجموعة أ", "Grade 12 · Group A"),
                                    LocalizedText("الأحد والثلاثاء · ٤ مساءً", "Sun and Tue · 4 PM"),
                                ),
                                TeacherGroup(
                                    LocalizedText("الثالث الثانوي · المجموعة ب", "Grade 12 · Group B"),
                                    LocalizedText("الاثنين والأربعاء · ٦ مساءً", "Mon and Wed · 6 PM"),
                                ),
                            ),
                        unpaidStudents =
                            listOf(
                                UnpaidStudent(LocalizedText("يوسف أحمد", "Youssef Ahmed"), 20_000),
                                UnpaidStudent(LocalizedText("مريم خالد", "Mariam Khaled"), 20_000),
                                UnpaidStudent(LocalizedText("سارة محمود", "Sara Mahmoud"), 20_000),
                            ),
                    ),
                    Teacher(
                        id = "2",
                        name = LocalizedText("نهى حسن", "Noha Hassan"),
                        phone = "01000001002",
                        subject = LocalizedText("الرياضيات", "Mathematics"),
                        monthlySalaryMinor = 3500_00,
                        groups =
                            listOf(
                                TeacherGroup(
                                    LocalizedText("الثاني الثانوي · المجموعة أ", "Grade 11 · Group A"),
                                    LocalizedText("السبت والثلاثاء · ٥ مساءً", "Sat and Tue · 5 PM"),
                                ),
                            ),
                    ),
                    Teacher(
                        id = "3",
                        name = LocalizedText("هبة سامي", "Heba Sami"),
                        phone = "01000001003",
                        subject = LocalizedText("اللغة الإنجليزية", "English"),
                        monthlySalaryMinor = 3800_00,
                        groups =
                            listOf(
                                TeacherGroup(
                                    LocalizedText("الأول الثانوي · المجموعة أ", "Grade 10 · Group A"),
                                    LocalizedText("الأحد والأربعاء · ٥ مساءً", "Sun and Wed · 5 PM"),
                                ),
                            ),
                        unpaidStudents = listOf(UnpaidStudent(LocalizedText("عمر مصطفى", "Omar Mostafa"), 18_000)),
                    ),
                ),
        )
}
