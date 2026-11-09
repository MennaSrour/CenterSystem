# Domain layer

`src/commonMain/kotlin/center/domain/` contains:

- `model/`: Student, Teacher, Payment, Attendance, QueuedMessage and domain enums.
- `repository/`: StudentRepository, TeacherRepository, PaymentRepository,
  AttendanceRepository and MessageQueueRepository.
- `usecase/`: ProcessPaymentUseCase, RecordAttendanceUseCase,
  GetTeacherMonthlyDuesUseCase and SyncPendingMessagesUseCase.

The module has no dependency on SQLite, Java, Android, Compose or Koin. Coroutines
and kotlinx-datetime are KMP dependencies. All business code and tests use common
source sets; the repository currently configures the desktop JVM target only.

## Contracts

Use cases have suspending `operator fun invoke` methods returning `Result<T>`.
Validation, missing-record and storage errors become failed Results with their
original causes. Coroutine cancellation is rethrown and never returned as an
ordinary business failure. Clocks and ID generators are injected; the desktop
composition root supplies epoch milliseconds and UUID strings. Tests use fixed
clock/ID implementations.

The existing database has no separate barcode/student-code column. A student's
code is their existing primary ID, and `getStudentByCode` uses exact ID lookup.
The use cases trim scanned student codes. Existing student identifiers and the
legacy `center.domain.Student`/`StudentRepository` aliases remain supported.
`RegisterStudent` updates a student's name without erasing grade/contact details.

## Business operations

`ProcessPaymentUseCase(studentCode, classId, amountPaid)`:

1. Validates a positive finite amount with at most two decimal places, up to
   1,000,000,000 to keep the existing SQLite REAL split check reliable.
2. Fetches an active student with a parent phone and the teacher for the class.
   A Teacher defaults to a 0.70 rate; invalid rates fail rather than silently
   substituting a rate or accepting a missing teacher.
3. Calculates the teacher share in integer cents with Kotlin's nearest/ties-to-even
   rounding. The center receives the remaining cents, preserving the full amount.
4. Creates a domain Payment and an Arabic receipt QueuedMessage with PENDING
   status and the same timestamp, then persists both atomically.

`RecordAttendanceUseCase(studentCode, classId, status)` validates the student and
class, creates the attendance record and an Arabic notification distinguishing
حضور from غياب, and persists both atomically. It does not infer enrollment because
there is no student/group enrollment table in the current schema.

PaymentRepository.saveWithReceipt and AttendanceRepository.saveWithNotification
are transactional outbox contracts. Infrastructure maps the domain records to
PaymentEntity/AttendanceEntity/MessageQueueEntity and inserts both records in one
SQLite transaction. This avoids a committed payment/attendance record followed by
a failed independent queue save. MessageQueueRepository handles queue observation,
explicit message saves and sent acknowledgements. Duplicate event or message IDs
fail without overwriting history; use cases generate fresh IDs, so a second user
invocation is a new operation, not an automatic duplicate-payment retry.

`GetTeacherMonthlyDuesUseCase(teacherId, month, year)` computes the month start
inclusive and next-month start exclusive in an injected TimeZone (Africa/Cairo by
default). It sums persisted teacherShare values from the teacher's payment stream
and returns the payout rounded to cents; it never recalculates old payments using
a new commission rate. Month lengths, leap years and year rollover use calendar
arithmetic. An empty month returns 0.0.

`SyncPendingMessagesUseCase()` returns a snapshot of pending messages only. It
neither sends messages nor changes their status. Infrastructure/UI may later send
them and acknowledge successful delivery with `MessageQueueRepository.markSent`.
No WhatsApp script or network synchronization is implemented here.

## Wiring and verification

SQL adapters live in `core:database`, map persistence types explicitly, and run
blocking queries/transactions on an injected dispatcher. Koin bindings stay in the
desktop composition root. No use case resolves dependencies from a service locator.

From repository root:

```sh
./gradlew ktlintCheck allTests :desktopApp:desktopJar
```

Common tests exercise allocation, validation, receipts, attendance wording,
cancellation and calendar boundaries. SQLite integration tests verify real
transaction rollback on queue failures, duplicate IDs, domain query mapping and
preservation of student contact information.
