# Local database

SQLDelight 2.1 generates `CenterDatabase`, row types and SQL queries from
`src/commonMain/sqldelight/center/database/*.sq`. The public persistence models
are in `entity/Entities.kt`; concrete reactive DAOs are in `dao/`. These database
models are infrastructure contracts, separate from business models in `domain`.

`DatabaseFactory.kt` opens the SQLite file, creates or migrates the schema and enables
foreign keys on every JDBC connection through SQLite driver properties. Database
version 2 is produced by migration `1.sqm`. The desktop composition root registers
`CenterDaos` and each DAO in Koin; the driver is closed at application shutdown.
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

- New IDs should be caller-generated UUID strings. Existing text student IDs are
  preserved. Foreign keys and their lookup indices connect groups to teachers,
  and attendance/payments to students and groups.
- Attendance is unique by student, class and timestamp. All referenced rows use
  restrictive deletion: remove dependent records explicitly or deactivate a student.
- Changing a class's teacher through `ClassGroupDao` is rejected when payments
  exist, preserving historical teacher attribution. Changing a teacher's commission
  does not recalculate previously stored shares.
- Amounts use the requested Double/SQLite REAL representation. Rates must be finite
  and between 0 and 1; amounts must be finite and nonnegative. Stored shares must
  sum to amountPaid within 0.000001. Business code calculates/rounds the allocation
  before saving; do not treat floating point values as exact accounting decimals.
- Timestamps are Unix epoch milliseconds. Monthly dues accept a half-open interval:
  start inclusive, next month's start exclusive. Compute both boundaries in the
  business timezone before calling the DAO, rather than assuming a fixed month duration.
- Message and attendance enums are stored as checked strings. Students default to
  active and teachers default to a 0.70 commission rate in Kotlin and SQLite.
- Version-1 students retain their IDs and names. Newly added grade/parent-phone
  fields are empty strings because the original schema did not contain those values;
  complete those details during editing. No contact information is invented. The
  legacy domain `StudentRepository` updates names without erasing the new fields.
- SQL constraint failures propagate to the caller. Cancellation is not swallowed.
  Do not import generated queries into UI; use domain repository adapters when
  connecting feature workflows to persistence.

## Validation

From repository root:

```sh
./gradlew :core:database:desktopTest ktlintCheck :desktopApp:desktopJar
```

Tests use real temporary SQLite files and cover migrations/reopen, CRUD, relations,
reactive updates, foreign keys, attendance uniqueness, monthly boundaries, stored
commission shares and pending-message filtering. The prior repository persistence
test remains in place. Use `.\\gradlew.bat` in Windows PowerShell.
