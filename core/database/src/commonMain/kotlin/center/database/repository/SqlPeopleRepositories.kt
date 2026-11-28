package center.database.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import center.database.CenterDatabase
import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.GroupScheduleSlot
import center.domain.model.LessonAttendance
import center.domain.model.MakeupSession
import center.domain.model.PersonType
import center.domain.model.QueuedMessage
import center.domain.model.StaffMember
import center.domain.repository.AttendanceSessionRepository
import center.domain.repository.MakeupSessionRepository
import center.domain.repository.StaffRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class SqlStaffRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : StaffRepository {
    private val people = database.peopleQueries

    override fun observeStaff() =
        people.selectStaff().asFlow().mapToList(dispatcher).map { rows ->
            rows.groupBy { it.id }.values.map { memberRows ->
                val row = memberRows.first()
                StaffMember(
                    row.id,
                    row.name,
                    row.role,
                    row.phone,
                    memberRows.mapNotNull { shift ->
                        shift.day?.let { day ->
                            GroupScheduleSlot(
                                DayOfWeek(day.toInt()),
                                time(checkNotNull(shift.fromMinute)),
                                time(checkNotNull(shift.toMinute)),
                            )
                        }
                    },
                    row.isActive,
                    row.monthlySalaryMinor,
                )
            }
        }

    override suspend fun save(member: StaffMember): Unit =
        withContext(dispatcher) {
            require(listOf(member.id, member.name, member.role, member.phone).all { it.isNotBlank() })
            require(member.monthlySalaryMinor == null || member.monthlySalaryMinor in 0L..100_000_000_000L)
            member.workSchedule.groupBy { it.day }.values.forEach { shifts ->
                require(shifts.sortedBy { it.from }.zipWithNext().none { (a, b) -> a.to > b.from }) { "Staff shifts overlap" }
            }
            database.transaction {
                val (id, name, role, phone, schedule, active, monthlySalary) = member
                if (people.selectStaffById(id).executeAsOneOrNull() == null) {
                    people.saveStaff(id, name.trim(), role.trim(), phone.trim(), active, monthlySalary)
                } else {
                    people.updateStaff(name.trim(), role.trim(), phone.trim(), active, monthlySalary, id)
                }
                people.clearShifts(id)
                schedule.forEach { people.insertShift(id, it.day.isoDayNumber.toLong(), it.from.minuteOfDay(), it.to.minuteOfDay()) }
            }
        }

    override suspend fun delete(id: String): Unit =
        withContext(dispatcher) {
            database.transaction { people.deleteStaff(id) }
        }
}

class SqlAttendanceSessionRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : AttendanceSessionRepository {
    private val people = database.peopleQueries
    private val zone = TimeZone.of("Africa/Cairo")

    override fun observeDay(date: LocalDate) = people.sessionsOnDate(date.toString(), ::session).asFlow().mapToList(dispatcher)

    override suspend fun closeOpenCheckInsBefore(date: LocalDate): Int = closeOpenSessions(date, before = true)

    override suspend fun closeOpenCheckInsOn(date: LocalDate): Int = closeOpenSessions(date, before = false)

    private suspend fun closeOpenSessions(
        date: LocalDate,
        before: Boolean,
    ): Int =
        withContext(dispatcher) {
            database.transactionWithResult {
                val rows: List<OpenCheckIn> =
                    if (before) {
                        people
                            .openSessionsBeforeDate(date.toString()) { type, person, group, sessionDate, checkIn, _ ->
                                OpenCheckIn(type, person, group, sessionDate, checkIn)
                            }.executeAsList()
                    } else {
                        people
                            .openSessionsOnDate(date.toString()) { type, person, group, sessionDate, checkIn, _ ->
                                OpenCheckIn(type, person, group, sessionDate, checkIn)
                            }.executeAsList()
                    }
                rows.forEach { row ->
                    val sessionDate = LocalDate.parse(row.date)
                    val weekday = sessionDate.dayOfWeek.isoDayNumber.toLong()
                    val lastMinute =
                        people
                            .lastScheduledMinute(weekday, row.personType, row.groupId, row.personId, row.date)
                            .executeAsOne()
                            .max
                    val dayEnd =
                        sessionDate
                            .plus(DatePeriod(days = 1))
                            .atStartOfDayIn(zone)
                            .toEpochMilliseconds() - 1
                    val scheduledEnd =
                        lastMinute?.let { minute ->
                            sessionDate.atStartOfDayIn(zone).toEpochMilliseconds() + minute * 60_000L
                        } ?: dayEnd
                    val checkout = maxOf(row.checkInTime, scheduledEnd)
                    people.closeCheckIn(checkout, row.personType, row.personId, row.groupId, row.date)
                }
                rows.size
            }
        }

    override fun observePeriod(
        startInclusive: Long,
        endExclusive: Long,
    ) = people.sessionsInPeriod(startInclusive, endExclusive, ::session).asFlow().mapToList(dispatcher)

    override fun observeHistory(
        type: PersonType,
        personId: String,
    ) = people.sessionHistory(type.name, personId, ::session).asFlow().mapToList(dispatcher)

    override suspend fun checkIn(
        session: AttendanceSession,
        notification: QueuedMessage?,
        lessonAttendance: List<LessonAttendance>,
    ): Unit =
        withContext(dispatcher) {
            val (person, date, timestamp) = session
            database.transaction {
                val (type, id, group) = person
                when (type) {
                    PersonType.STUDENT -> {
                        require(
                            database.studentQueries
                                .selectById(id)
                                .executeAsOneOrNull()
                                ?.isActive == true,
                        )
                        if (group.isNotEmpty()) {
                            require(
                                database.classGroupQueries
                                    .selectById(group)
                                    .executeAsOneOrNull()
                                    ?.status == "ACTIVE",
                            )
                            require(database.operationsQueries.hasEnrollment(id, group).executeAsOne())
                            val (start, end) = date.bounds(zone, DatePeriod(days = 1))
                            check(database.operationsQueries.countDailyAttendance(id, group, start, end).executeAsOne() == 0L)
                        }
                    }
                    PersonType.TEACHER -> require(database.teacherQueries.selectById(id).executeAsOneOrNull() != null)
                    PersonType.STAFF -> require(people.selectStaffById(id).executeAsOneOrNull()?.isActive == true)
                }
                check(people.selectSession(type.name, id, group, date.toString()).executeAsOneOrNull() == null)
                people.insertCheckIn(type.name, id, group, date.toString(), timestamp, timestamp)
                lessonAttendance.forEach { item ->
                    require(type == PersonType.STUDENT && item.studentId == id && item.date == date)
                    require(database.operationsQueries.hasEnrollment(id, item.lessonId).executeAsOne())
                    item.makeupSessionId?.let { makeupId ->
                        require(
                            database.makeupSessionQueries
                                .hasMakeupSessionForLessonDate(
                                    makeupId,
                                    item.lessonId,
                                    date.toString(),
                                ).executeAsOne(),
                        )
                    }
                    people.upsertLessonAttendance(
                        item.studentId,
                        item.lessonId,
                        item.date.toString(),
                        item.attended,
                        item.recordedAt,
                        item.checkOutAt,
                        item.makeupSessionId.orEmpty(),
                    )
                }
                notification?.let { database.insertMessage(it.toEntity()) }
            }
        }

    override suspend fun checkOut(
        person: AttendancePerson,
        date: LocalDate,
        timestamp: Long,
    ): Unit =
        withContext(dispatcher) {
            val (type, id, group) = person
            database.transaction {
                val existing = checkNotNull(people.selectSession(type.name, id, group, date.toString()).executeAsOneOrNull())
                check(existing.checkOutTime == null && timestamp >= existing.checkInTime)
                people.setCheckOut(timestamp, type.name, id, group, date.toString())
            }
        }

    override fun observeLessonHistory(studentId: String) =
        people
            .lessonAttendanceForStudent(studentId) { id, classId, date, attended, recordedAt, checkOutAt, makeupSessionId ->
                LessonAttendance(id, classId, LocalDate.parse(date), attended, recordedAt, checkOutAt, makeupSessionId)
            }.asFlow()
            .mapToList(dispatcher)

    override suspend fun saveLessonAttendance(records: List<LessonAttendance>): Unit =
        withContext(dispatcher) {
            database.transaction {
                records.forEach { item ->
                    require(database.operationsQueries.hasEnrollment(item.studentId, item.lessonId).executeAsOne())
                    item.makeupSessionId?.let { makeupId ->
                        require(
                            database.makeupSessionQueries
                                .hasMakeupSessionForLessonDate(makeupId, item.lessonId, item.date.toString())
                                .executeAsOne(),
                        )
                    }
                    people.upsertLessonAttendance(
                        item.studentId,
                        item.lessonId,
                        item.date.toString(),
                        item.attended,
                        item.recordedAt,
                        item.checkOutAt,
                        item.makeupSessionId.orEmpty(),
                    )
                }
            }
        }

    override suspend fun markFutureLessonsMissed(
        studentId: String,
        date: LocalDate,
        timestamp: Long,
    ): Unit =
        withContext(dispatcher) {
            val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone)
            people.markFutureLessonsMissed(
                timestamp,
                studentId,
                date.toString(),
                local.dayOfWeek.isoDayNumber.toLong(),
                (local.hour * 60 + local.minute).toLong(),
            )
            people.markFutureMakeupLessonsMissed(
                timestamp,
                studentId,
                date.toString(),
                LocalTime(local.hour, local.minute).toString(),
            )
        }

    private fun session(
        type: String,
        id: String,
        group: String,
        date: String,
        checkIn: Long,
        checkOut: Long?,
    ) = AttendanceSession(
        AttendancePerson(PersonType.valueOf(type), id, group),
        if (date.isEmpty()) Instant.fromEpochMilliseconds(checkIn).toLocalDateTime(zone).date else LocalDate.parse(date),
        checkIn,
        checkOut,
    )
}

private data class OpenCheckIn(
    val personType: String,
    val personId: String,
    val groupId: String,
    val date: String,
    val checkInTime: Long,
)

class SqlMakeupSessionRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MakeupSessionRepository {
    private val queries = database.makeupSessionQueries

    override fun observeForDate(date: LocalDate) =
        queries
            .selectMakeupSessionsForDate(date.toString(), ::makeupSession)
            .asFlow()
            .mapToList(dispatcher)

    override fun observeForPeriod(
        startInclusive: LocalDate,
        endExclusive: LocalDate,
    ) = queries
        .selectMakeupSessionsForPeriod(
            startInclusive.toString(),
            endExclusive.toString(),
            startInclusive.toString(),
            endExclusive.toString(),
            ::makeupSession,
        ).asFlow()
        .mapToList(dispatcher)

    override suspend fun save(session: MakeupSession): Unit =
        withContext(dispatcher) {
            require(session.startTime < session.endTime)
            database.transaction {
                check(database.classGroupQueries.selectById(session.lessonId).executeAsOneOrNull() != null)
                queries.insertMakeupSession(
                    session.id,
                    session.lessonId,
                    session.originalCanceledDate?.toString(),
                    session.makeupDate.toString(),
                    session.startTime.toString(),
                    session.endTime.toString(),
                    session.notes.trim(),
                    session.createdAt,
                )
            }
        }

    private fun makeupSession(
        id: String,
        lessonId: String,
        originalCanceledDate: String?,
        makeupDate: String,
        startTime: String,
        endTime: String,
        notes: String,
        createdAt: Long,
    ) = MakeupSession(
        id,
        lessonId,
        originalCanceledDate?.let(LocalDate::parse),
        LocalDate.parse(makeupDate),
        LocalTime.parse(startTime),
        LocalTime.parse(endTime),
        notes,
        createdAt,
    )
}

private fun time(minute: Long): LocalTime = LocalTime((minute / 60).toInt(), (minute % 60).toInt())

private fun LocalTime.minuteOfDay(): Long = hour * 60L + minute
