package center.database

import center.database.SqlStudentRepository
import center.database.repository.SqlAttendanceSessionRepository
import center.database.repository.SqlClassGroupRepository
import center.database.repository.SqlMakeupSessionRepository
import center.database.repository.SqlTeacherRepository
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LessonAttendance
import center.domain.model.LessonEnrollment
import center.domain.model.MakeupSession
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.usecase.monthlyLessonCounts
import center.domain.usecase.scheduledLessonsForCheckIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MakeupSessionTest {
    @Test
    fun makeupSessionShowsOnTargetDatePersistsAttendanceAndOffsetsCanceledDate() =
        runTest {
            val path = Files.createTempDirectory("center-makeup-session")
            try {
                openDatabase(path).use { driver ->
                    val database = CenterDatabase(driver)
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val teacherRepository = SqlTeacherRepository(database, dispatcher)
                    val studentRepository = SqlStudentRepository(database, dispatcher)
                    val groupRepository = SqlClassGroupRepository(database, dispatcher)
                    val makeupRepository = SqlMakeupSessionRepository(database, dispatcher)
                    val attendanceRepository = SqlAttendanceSessionRepository(database, dispatcher)
                    teacherRepository.save(Teacher("teacher", "Teacher", "Math"))
                    val student = Student("student", "Student", code = "ST-1")
                    studentRepository.save(student)
                    val group =
                        ClassGroup(
                            id = "lesson",
                            name = "Math",
                            teacherId = "teacher",
                            monthlyPriceMinor = 10_000,
                            commissionBasisPoints = 7_000,
                            roomNumber = 1,
                            system = EducationSystem.ARABIC,
                            status = GroupStatus.ACTIVE,
                            schedule = listOf(GroupScheduleSlot(DayOfWeek.THURSDAY, LocalTime(13, 0), LocalTime(14, 0))),
                            enrollments = mapOf(student.id to LessonEnrollment(10_000, center.domain.model.StudentTrack.ARABIC)),
                        )
                    groupRepository.save(group)
                    val makeup =
                        MakeupSession(
                            id = "makeup-1",
                            lessonId = group.id,
                            originalCanceledDate = LocalDate(2026, 11, 5),
                            makeupDate = LocalDate(2026, 11, 12),
                            startTime = LocalTime(15, 0),
                            endTime = LocalTime(16, 0),
                            notes = "Rescheduled",
                            createdAt = 100,
                        )
                    makeupRepository.save(makeup)

                    val persisted = makeupRepository.observeForDate(makeup.makeupDate).first().single()
                    assertEquals(makeup, persisted)
                    val now = makeup.makeupDate.atStartOfDayIn(TimeZone.of("Africa/Cairo")).toEpochMilliseconds() + 12 * 60 * 60_000L
                    val options = scheduledLessonsForCheckIn(listOf(group), makeup.makeupDate, now, makeupSessions = listOf(persisted))
                    assertEquals(2, options.size)
                    val makeupOption = options.single { it.isMakeup }
                    assertEquals("makeup-1", makeupOption.key)
                    assertTrue(makeupOption.attended)

                    attendanceRepository.saveLessonAttendance(
                        listOf(
                            LessonAttendance(
                                student.id,
                                group.id,
                                makeup.makeupDate,
                                true,
                                now,
                                makeupSessionId = makeupOption.makeupSessionId,
                            ),
                        ),
                    )
                    assertEquals(
                        "makeup-1",
                        attendanceRepository
                            .observeLessonHistory(student.id)
                            .first()
                            .single()
                            .makeupSessionId,
                    )

                    val counts = monthlyLessonCounts(listOf(group), LocalDate(2026, 11, 1), listOf(persisted))
                    assertEquals(4, counts.scheduledSessionCount)
                    assertEquals(1, counts.makeupSessionCount)
                    assertEquals(1, counts.rescheduledDateCount)
                }
            } finally {
                path.toFile().deleteRecursively()
            }
        }
}
