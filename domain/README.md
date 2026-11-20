# Domain layer

`src/commonMain/kotlin/center/domain/` contains:

- `model/` defines student, teacher, group, enrollment-facing, billing, ledger,
  attendance and reminder contracts.
- `repository/` exposes persistence interfaces independent of SQLDelight.
- `usecase/` contains student registration, payment, attendance, salary,
  overdue balances, collection deficits and reminder queuing.

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

Student codes are separate from stable IDs. Legacy rows use the ID as their
code. Full registration captures grade, stage, education system, parent phone and
enrolled group IDs. The legacy name-update entry point preserves those details.

## Business operations

- `ProcessPaymentUseCase` validates the amount, applies the group's commission,
  and saves the payment and receipt together. It uses a first-of-month billing
  date, defaulting to the current Cairo month. Paid-in-advance amounts apply to
  their designated month. Configured groups require active enrollment and reject
  amounts exceeding the remaining monthly balance.
- `RecordAttendanceUseCase` stores attendance and its notification together.
  SQL persistence allows one record per student and group per Cairo calendar day.
- `GetOverdueStudentsUseCase` checks the current Cairo month starting at midnight
  on the 5th. It returns one positive balance per active enrollment and applies
  partial payments to the remaining amount.
- `GetTeacherMonthlySalaryUseCase` sums contracted monthly commission over
  current active enrollments. Each enrollment contributes its full monthly fee
  times the group's commission, rounded to the nearest piastre.
- `GetTeacherCollectionDeficitUseCase` separately reports the count of distinct
  students with a balance and the total remaining across their groups.
  `GetTeacherMonthlyDuesUseCase` remains available for commissions on historical
  receipts, using their payment timestamps and stored shares.
- `TreasuryRepository` exposes student payment income and operational or teacher
  payout expenses together. The ledger uses actual transaction timestamps.
- `QueueReminderUseCase` stores pending WhatsApp and notification reminders.
  `SyncPendingMessagesUseCase` reads the pending snapshot, and
  `MessageQueueRepository.markSent` acknowledges delivery. Delivery transport is
  outside this step.

New subscription prices, salary totals and ledger amounts use integer piastres.
Group commission uses basis points, where 7000 represents 70%. Existing payment
rows retain their original decimal amount and share columns.

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
