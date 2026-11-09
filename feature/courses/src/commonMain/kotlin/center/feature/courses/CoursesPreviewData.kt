package center.feature.courses

/** Explicit screenshot fixtures, never a default source of persisted groups. */
object CoursesPreviewData {
    val teachers =
        listOf(
            CourseTeacher("teacher-1", "أ. محمود حسن"),
            CourseTeacher("teacher-2", "أ. أحمد سامي"),
            CourseTeacher("teacher-3", "أ. كريم عادل"),
            CourseTeacher("teacher-4", "أ. سارة علي"),
            CourseTeacher("teacher-5", "أ. محمد فؤاد"),
            CourseTeacher("teacher-6", "أ. مي خالد"),
        )
    val grades =
        listOf(
            CourseGrade("secondary-3", "الثالث الثانوي", CourseStage.Secondary),
            CourseGrade("secondary-2", "الثاني الثانوي", CourseStage.Secondary),
            CourseGrade("secondary-1", "الأول الثانوي", CourseStage.Secondary),
            CourseGrade("prep-3", "الثالث الإعدادي", CourseStage.Prep),
            CourseGrade("primary-6", "السادس الابتدائي", CourseStage.Primary),
        )
    val rooms = (1..8).map { CourseRoom("room-$it", "قاعة $it") }

    fun state() =
        CoursesState(
            summary = CoursesSummary("18", "15", "12"),
            teachers = teachers,
            grades = grades,
            rooms = rooms,
            userName = "نور أحمد",
            dateLabel = "عرض تجريبي — بيانات توضيحية",
            groups =
                listOf(
                    group(
                        "group-1",
                        "فيزياء",
                        "teacher-1",
                        "secondary-3",
                        120,
                        150,
                        ScheduleDay.Saturday,
                        ScheduleDay.Tuesday,
                    ),
                    group(
                        "group-2",
                        "كيمياء",
                        "teacher-2",
                        "secondary-3",
                        98,
                        120,
                        ScheduleDay.Sunday,
                        ScheduleDay.Wednesday,
                    ),
                    group(
                        "group-3",
                        "رياضيات",
                        "teacher-3",
                        "secondary-2",
                        85,
                        100,
                        ScheduleDay.Monday,
                        ScheduleDay.Thursday,
                    ),
                    group(
                        "group-4",
                        "اللغة الإنجليزية",
                        "teacher-4",
                        "secondary-1",
                        72,
                        90,
                        ScheduleDay.Saturday,
                        ScheduleDay.Wednesday,
                    ),
                    group(
                        "group-5",
                        "اللغة العربية",
                        "teacher-5",
                        "prep-3",
                        110,
                        130,
                        ScheduleDay.Sunday,
                        ScheduleDay.Tuesday,
                    ),
                    group(
                        "group-6",
                        "العلوم",
                        "teacher-6",
                        "primary-6",
                        64,
                        80,
                        ScheduleDay.Monday,
                        ScheduleDay.Thursday,
                    ).copy(isActive = false),
                ),
        )

    private fun group(
        id: String,
        subject: String,
        teacher: String,
        grade: String,
        enrolled: Int,
        capacity: Int,
        first: ScheduleDay,
        second: ScheduleDay,
    ) = CourseGroup(
        id,
        subject,
        teacher,
        grade,
        CourseSystem.Arabic,
        "100",
        "70",
        "room-3",
        enrolled,
        capacity,
        listOf(CourseSchedule("$id-slot-1", first, "16:00", "17:30"), CourseSchedule("$id-slot-2", second, "16:00", "17:30")),
    )

    fun newForm() =
        GroupFormState(teachers = teachers, grades = grades, rooms = rooms, timeSlots = listOf(GroupTimeSlotState("draft-slot-1")))

    fun editForm(group: CourseGroup) =
        GroupFormState(
            mode = GroupFormMode.Edit(group.id),
            subject = group.subject,
            teachers = teachers,
            grades = grades,
            rooms = rooms,
            selectedTeacherId = group.teacherId,
            selectedGradeId = group.gradeId,
            system = group.system,
            monthlyPrice = group.monthlyPrice,
            teacherCommission = group.teacherCommission,
            maxCapacity = group.maxCapacity.toString(),
            selectedRoomId = group.roomId,
            timeSlots = group.schedule.map { GroupTimeSlotState(it.id, it.day, it.from, it.to) },
            canSubmit = true,
        )
}
