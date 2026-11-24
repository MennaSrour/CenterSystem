package center.database.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import center.database.CenterDatabase
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LedgerDirection
import center.domain.model.LedgerEntry
import center.domain.model.LedgerKind
import center.domain.model.SubscriptionBalance
import center.domain.model.TreasuryExpense
import center.domain.model.TreasuryTotals
import center.domain.repository.ClassGroupRepository
import center.domain.repository.MonthlyBillingRepository
import center.domain.repository.TreasuryRepository
import center.domain.usecase.salaryPayoutId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

class SqlClassGroupRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ClassGroupRepository {
    private val queries = database.operationsQueries

    override fun observeGroups(): Flow<List<ClassGroup>> =
        queries.selectGroupsWithSchedule().asFlow().mapToList(dispatcher).map { rows ->
            rows.groupBy { it.id }.values.map { slots ->
                val row = slots.first()
                ClassGroup(
                    id = row.id,
                    name = row.name,
                    teacherId = row.teacherId,
                    monthlyPriceMinor = row.monthlyPriceMinor,
                    commissionBasisPoints = row.commissionBasisPoints?.toInt(),
                    roomNumber = row.roomNumber?.toInt(),
                    system = EducationSystem.valueOf(row.system),
                    status = GroupStatus.valueOf(row.status),
                    gradeId = row.gradeId,
                    maxCapacity = row.maxCapacity?.toInt(),
                    lessonType = center.domain.model.LessonType.valueOf(row.lessonType),
                    languagesPriceMinor = row.languagesPriceMinor,
                    schedule =
                        slots.mapNotNull { slot ->
                            slot.day?.let { day ->
                                GroupScheduleSlot(
                                    DayOfWeek(day.toInt()),
                                    minuteTime(checkNotNull(slot.fromMinute)),
                                    minuteTime(checkNotNull(slot.toMinute)),
                                )
                            }
                        },
                )
            }
        }

    override suspend fun save(group: ClassGroup): Unit =
        withContext(dispatcher) {
            require(group.id.isNotBlank() && group.name.isNotBlank() && group.teacherId.isNotBlank())
            val price = requireNotNull(group.monthlyPriceMinor)
            val commission = requireNotNull(group.commissionBasisPoints)
            val room = requireNotNull(group.roomNumber)
            require(price in 0L..100_000_000_000L)
            require(commission in 0..10_000)
            require(room in 1..8)
            require(group.maxCapacity?.let { it > 0 } != false)
            require(group.languagesPriceMinor?.let { it in 0L..100_000_000_000L } != false)
            require(group.lessonType == center.domain.model.LessonType.MIXED || group.languagesPriceMinor == null)
            group.schedule.groupBy { it.day }.values.forEach { slots ->
                require(slots.sortedBy { it.from }.zipWithNext().none { (a, b) -> a.to > b.from }) { "Group schedule slots overlap" }
            }
            database.transaction {
                val groups = database.classGroupQueries
                val existing = groups.selectById(group.id).executeAsOneOrNull()
                if (existing == null) {
                    groups.insertRow(group.id, group.name, group.teacherId, 0.0)
                } else {
                    if (existing.teacherId != group.teacherId) {
                        check(
                            database.paymentQueries.countForClass(group.id).executeAsOne() == 0L,
                        ) { "Cannot reassign a class with payment history" }
                    }
                    groups.updateRow(group.name, group.teacherId, existing.sessionPrice, group.id)
                }
                queries.updateGroupDetails(
                    price,
                    commission.toLong(),
                    room.toLong(),
                    group.system.name,
                    group.status.name,
                    group.gradeId,
                    group.maxCapacity?.toLong(),
                    group.lessonType.name,
                    group.languagesPriceMinor,
                    group.id,
                )
                queries.clearSchedule(group.id)
                group.schedule.forEach {
                    queries.insertSlot(group.id, it.day.isoDayNumber.toLong(), it.from.minuteOfDay(), it.to.minuteOfDay())
                }
            }
        }

    override suspend fun archive(id: String): Unit = withContext(dispatcher) { queries.archiveGroup(id) }

    override suspend fun delete(id: String): Unit = withContext(dispatcher) { database.classGroupQueries.deleteById(id) }
}

class SqlMonthlyBillingRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
) : MonthlyBillingRepository {
    override fun observeChanges(): Flow<Unit> =
        database.operationsQueries
            .billingChanges()
            .asFlow()
            .map { Unit }

    override suspend fun remainingBalances(
        month: LocalDate,
        teacherId: String?,
    ): List<SubscriptionBalance> =
        withContext(dispatcher) {
            require(month.dayOfMonth == 1)
            val (start, end) = month.bounds(timeZone)
            database.operationsQueries.remainingBalances(month.toString(), start, end, teacherId).executeAsList().map {
                SubscriptionBalance(
                    it.studentId,
                    it.code,
                    it.studentName,
                    it.parentPhone,
                    it.classId,
                    it.groupName,
                    it.teacherId,
                    it.teacherName,
                    checkNotNull(it.remainingMinor),
                )
            }
        }

    override suspend fun monthlySalaryMinor(teacherId: String): Long =
        withContext(dispatcher) { database.operationsQueries.monthlySalary(teacherId).executeAsOne() }
}

class SqlTreasuryRepository(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) : TreasuryRepository {
    override fun observeTotals(day: LocalDate): Flow<TreasuryTotals> {
        val zone = TimeZone.of("Africa/Cairo")
        val (start, end) = day.bounds(zone, DatePeriod(days = 1))
        val monthStart = LocalDate(day.year, day.month, 1).atStartOfDayIn(zone).toEpochMilliseconds()
        return database.operationsQueries
            .treasuryTotals(
                dayEnd = end,
                dayStart = start,
                monthStart = monthStart,
            ).asFlow()
            .mapToOne(dispatcher)
            .map {
                TreasuryTotals(
                    checkNotNull(it.todayIncome),
                    checkNotNull(it.todayExpense),
                    checkNotNull(it.drawerBalance),
                    checkNotNull(it.monthNet),
                )
            }
    }

    override fun observeLedger(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<LedgerEntry>> {
        require(startInclusive < endExclusive)
        return database.operationsQueries.selectLedger(startInclusive, endExclusive).asFlow().mapToList(dispatcher).map { rows ->
            rows.map {
                LedgerEntry(
                    it.id,
                    LedgerDirection.valueOf(it.direction),
                    LedgerKind.valueOf(it.kind),
                    it.amountMinor,
                    it.recordedAt,
                    it.description,
                    it.studentId,
                    it.classId,
                    it.teacherId,
                )
            }
        }
    }

    override suspend fun recordExpense(expense: TreasuryExpense): Unit =
        withContext(dispatcher) {
            database.operationsQueries.insertExpense(
                expense.id,
                expense.kind.name,
                expense.amountMinor,
                expense.recordedAt,
                expense.description,
                expense.teacherId,
            )
        }

    override suspend fun recordSalaryPayout(
        teacherId: String,
        month: LocalDate,
        recordedAt: Long,
        notes: String,
    ): Unit =
        withContext(dispatcher) {
            require(month.dayOfMonth == 1)
            database.transaction {
                val amount = database.operationsQueries.monthlySalary(teacherId).executeAsOne()
                require(amount > 0) { "No salary is due" }
                database.operationsQueries.insertExpense(
                    salaryPayoutId(teacherId, month),
                    LedgerKind.TEACHER_PAYOUT.name,
                    amount,
                    recordedAt,
                    notes.trim().ifEmpty { "Salary $month" },
                    teacherId,
                )
            }
        }
}

internal fun LocalDate.bounds(
    zone: TimeZone,
    period: DatePeriod = DatePeriod(months = 1),
): Pair<Long, Long> = atStartOfDayIn(zone).toEpochMilliseconds() to plus(period).atStartOfDayIn(zone).toEpochMilliseconds()

private fun minuteTime(minute: Long): LocalTime = LocalTime((minute / 60).toInt(), (minute % 60).toInt())

private fun LocalTime.minuteOfDay(): Long = hour * 60L + minute
