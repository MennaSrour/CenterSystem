package center.feature.students

/** Screenshot fixtures only. No row or ID represents a newly registered student. */
object StudentsPreviewData {
    val grades =
        listOf(
            StudentGrade("primary-1", "الأول الابتدائي", EducationStage.Primary),
            StudentGrade("primary-6", "السادس الابتدائي", EducationStage.Primary),
            StudentGrade("prep-1", "الأول الإعدادي", EducationStage.Prep),
            StudentGrade("prep-2", "الثاني الإعدادي", EducationStage.Prep),
            StudentGrade("prep-3", "الثالث الإعدادي", EducationStage.Prep),
            StudentGrade("secondary-1", "الأول الثانوي", EducationStage.Secondary),
            StudentGrade("secondary-2", "الثاني الثانوي", EducationStage.Secondary),
            StudentGrade("secondary-3", "الثالث الثانوي", EducationStage.Secondary),
        )

    fun state() =
        StudentsState(
            summary = StudentsSummary("248", "95", "81", "72"),
            userName = "نور أحمد",
            dateLabel = "عرض تجريبي — بيانات توضيحية",
            grades = grades,
            students =
                listOf(
                    StudentRow(
                        "student-1",
                        "ST-10234",
                        "أحمد محمد علي",
                        "Ahmed Mohamed",
                        "secondary-3",
                        StudentSystem.Arabic,
                        "+20 10 2468 1357",
                    ),
                    StudentRow(
                        "student-2",
                        "ST-10817",
                        "سارة محمود حسن",
                        "Sara Mahmoud",
                        "prep-2",
                        StudentSystem.Languages,
                        "+20 11 7845 2091",
                    ),
                    StudentRow(
                        "student-3",
                        "ST-10452",
                        "عمر مصطفى كريم",
                        "Omar Mostafa",
                        "secondary-1",
                        StudentSystem.Arabic,
                        "+20 12 3654 8170",
                    ),
                    StudentRow(
                        "student-4",
                        "ST-10903",
                        "مريم خالد أحمد",
                        "Mariam Khaled",
                        "primary-6",
                        StudentSystem.Languages,
                        "+20 10 9214 5638",
                    ),
                ),
        )

    fun addStudent() = AddStudentState(grades = grades)

    fun filledForm() =
        AddStudentState(
            name = "يوسف أحمد",
            code = "ST-11024",
            grades = grades,
            selectedGradeId = "secondary-3",
            parentPhone = "+20 10 1234 5678",
            canSubmit = true,
        )
}
