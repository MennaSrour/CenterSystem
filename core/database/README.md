# Local database

SQLDelight 2.1 generates `CenterDatabase`, row types and SQL queries from
`src/commonMain/sqldelight/center/database/*.sq`. The public persistence models
are in `entity/Entities.kt`; concrete reactive DAOs are in `dao/`. These database
models are infrastructure contracts, separate from business models in `domain`.

`DatabaseFactory.kt` opens the SQLite file, creates or migrates the schema and enables
foreign keys on every JDBC connection through SQLite driver properties. Database
version 4 applies migrations `1.sqm` through `3.sqm`. The latest migration
adds the optional grade and capacity columns to existing groups. The desktop Koin
module registers domain repositories, use cases, and the legacy DAOs; the driver is closed at application shutdown.
The path remains `%LOCALAPPDATA%/CenterSystem/center.db` (or `~/CenterSystem/center.db`).
There is no server, synchronization client or messaging transport in this module.

## API

All six DAOs provide `observeAll()`, `observeById(id)`, `suspend save(entity)` and
`suspend delete(id)`. Save inserts or updates by primary key in one transaction;
it never uses SQLite REPLACE, which could delete related history. Queries expose
cold `Flow`s backed by SQLDelight invalidation listeners, not an in-memory cache.
SQL reads/writes run on the injected dispatcher; desktop production uses `Dispatchers.IO`.

Additional queries:

- `StudentDao.getStudentWithPayments(id)` observes a single LEFT JOIN snapshot,
  returning null for a missing student and an empty payment list for an existing
  student without payments. Changes to either table invalidate the flow.
- `PaymentDao.getPaymentsForStudent(studentId)` orders payments by date then ID.
- `PaymentDao.getTeacherDuesByMonth(teacherId, monthStartInclusive, nextMonthStartExclusive)`
  sums persisted payment amounts and shares, returning zero totals for no matches.
- `AttendanceDao.getAttendanceForClass(classId, startInclusive, endExclusive)`
  filters attendance by class and time window.
- `MessageQueueDao.getPendingMessages()` orders pending messages by creation time
  then ID. `markSent(id)` updates local status and is idempotent. No messages are sent.

Example at the infrastructure/application boundary:

```kotlin
val daos = CenterDaos(CenterDatabase(driver), Dispatchers.IO)
daos.teachers.save(TeacherEntity(teacherId, "Teacher name", "Mathematics"))
val pendingMessages: Flow<List<MessageQueueEntity>> = daos.messages.getPendingMessages()
val studentWithPayments: Flow<StudentWithPayments?> = daos.students.getStudentWithPayments(studentId)
```

## Storage rules

- Migration preserves existing IDs, payments, attendance, and queued messages.
  Legacy students fall back to their ID as a code. Existing per-session prices
  stay in storage for compatibility and are never converted to monthly fees.
  Staff must configure a legacy group's monthly terms and enrollments before it
  enters billing calculations. Existing messages default to WhatsApp.
- Groups store monthly prices in piastres, commission in basis points, room
  numbers 1–8, education system, status, and ordered day/time slots. Students
  and groups are related through the enrollment table.
- Monthly billing uses explicit first-of-month attribution for new payments.
  For legacy payments without that field, the Cairo receipt timestamp determines
  the month. The overdue query uses the current month and only active groups and
  students. The salary query uses full monthly fees without consulting receipts.
- The ledger is a union of payments and the expense table, so each payment
  becomes one income entry and receipt rollbacks cannot leave an orphan ledger
  entry. Expenses distinguish operational costs and teacher payouts.
- Domain payment and attendance writes use transactions to persist the event and
  queued notification together. The attendance repository prevents duplicate
  check-ins for the same student and group on a Cairo calendar day.
- SQL errors propagate to callers. UI code should use domain contracts at the
  application boundary. The Teachers, Courses, and Students routes subscribe to
  the domain repository adapters through Koin.

## Validation

From repository root:

```sh
./gradlew :core:database:desktopTest ktlintCheck :desktopApp:desktopJar
```

Tests use real temporary SQLite files and cover migrations/reopen, CRUD, relations,
reactive updates, foreign keys, attendance uniqueness, monthly boundaries, stored
commission shares and pending-message filtering. The prior repository persistence
test remains in place. Use `.\\gradlew.bat` in Windows PowerShell.

Domain repository adapters and atomic payment/attendance message writes are described in [domain documentation](../../domain/README.md).
