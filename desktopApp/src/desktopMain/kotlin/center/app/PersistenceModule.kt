package center.app

import app.cash.sqldelight.db.SqlDriver
import center.database.CenterDaos
import center.database.CenterDatabase
import center.database.SqlAuthRepository
import center.database.SqlStudentRepository
import center.database.repository.SqlAttendanceRepository
import center.database.repository.SqlAttendanceSessionRepository
import center.database.repository.SqlClassGroupRepository
import center.database.repository.SqlMakeupSessionRepository
import center.database.repository.SqlMessageQueueRepository
import center.database.repository.SqlMonthlyBillingRepository
import center.database.repository.SqlPaymentRepository
import center.database.repository.SqlStaffRepository
import center.database.repository.SqlTeacherRepository
import center.database.repository.SqlTreasuryRepository
import center.domain.RegisterStudent
import center.domain.repository.AttendanceRepository
import center.domain.repository.AttendanceSessionRepository
import center.domain.repository.AuthRepository
import center.domain.repository.ClassGroupRepository
import center.domain.repository.MakeupSessionRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.MonthlyBillingRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.StaffRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import center.domain.repository.TreasuryRepository
import center.domain.usecase.AuthSession
import center.domain.usecase.EpochClock
import center.domain.usecase.GetOverdueStudentsUseCase
import center.domain.usecase.GetTeacherCollectionDeficitUseCase
import center.domain.usecase.GetTeacherMonthlyDuesUseCase
import center.domain.usecase.GetTeacherMonthlySalaryUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.PayTeacherSalaryUseCase
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.QueueReminderUseCase
import center.domain.usecase.RecordAttendanceUseCase
import center.domain.usecase.RegisterPresenceUseCase
import center.domain.usecase.SequentialPersonCodeGenerator
import center.domain.usecase.SyncPendingMessagesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import java.util.UUID

internal fun persistenceModule(
    driver: SqlDriver,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    clock: EpochClock = EpochClock { System.currentTimeMillis() },
) = module {
    single { CenterDatabase(driver) }
    single { SqlAuthRepository(get(), dispatcher) }
    single<AuthRepository> { get<SqlAuthRepository>() }
    single { AuthSession(get()) }
    single { CenterDaos(get(), dispatcher) }
    single { get<CenterDaos>().teachers }
    single { get<CenterDaos>().students }
    single { get<CenterDaos>().classGroups }
    single { get<CenterDaos>().attendance }
    single { get<CenterDaos>().payments }
    single { get<CenterDaos>().messages }
    single { SqlStudentRepository(get(), dispatcher) }
    single<StudentRepository> { get<SqlStudentRepository>() }
    single<TeacherRepository> { SqlTeacherRepository(get(), dispatcher) }
    single<ClassGroupRepository> { SqlClassGroupRepository(get(), dispatcher) }
    single<PaymentRepository> { SqlPaymentRepository(get(), dispatcher) }
    single<AttendanceRepository> { SqlAttendanceRepository(get(), dispatcher) }
    single<MessageQueueRepository> { SqlMessageQueueRepository(get(), dispatcher) }
    single<MonthlyBillingRepository> { SqlMonthlyBillingRepository(get(), dispatcher) }
    single<TreasuryRepository> { SqlTreasuryRepository(get(), dispatcher) }
    single<StaffRepository> { SqlStaffRepository(get(), dispatcher) }
    single<AttendanceSessionRepository> { SqlAttendanceSessionRepository(get(), dispatcher) }
    single<MakeupSessionRepository> { SqlMakeupSessionRepository(get(), dispatcher) }
    factory { RegisterPresenceUseCase(get(), get(), get(), get()) }
    single { clock }
    single { IdGenerator { UUID.randomUUID().toString() } }
    single { SequentialPersonCodeGenerator() }
    factory { RegisterStudent(get()) }
    factory { ProcessPaymentUseCase(get(), get(), get(), get(), get()) }
    factory { RecordAttendanceUseCase(get(), get(), get(), get(), get()) }
    factory { GetTeacherMonthlyDuesUseCase(get()) }
    factory { GetTeacherMonthlySalaryUseCase(get()) }
    factory { GetTeacherCollectionDeficitUseCase(get()) }
    factory { GetOverdueStudentsUseCase(get(), get()) }
    factory { PayTeacherSalaryUseCase(get(), get()) }
    single {
        DirectoryServices(
            students = get(),
            teachers = get(),
            groups = get(),
            billing = get(),
            payments = get(),
            treasury = get(),
            registerStudent = get(),
            overdue = get(),
            salary = get(),
            paySalary = get(),
            reminder = get(),
            clock = get(),
            ids = get(),
            attendance = get(),
            sessions = get(),
            makeupSessions = get(),
            staff = get(),
            presence = get(),
            processPayment = get(),
            personCodes = get(),
        )
    }
    factory { QueueReminderUseCase(get(), get(), get(), get()) }
    factory { SyncPendingMessagesUseCase(get()) }
}
