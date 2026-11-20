package center.app

import center.database.openDatabase
import center.domain.RegisterStudent
import center.domain.repository.AttendanceRepository
import center.domain.repository.ClassGroupRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.MonthlyBillingRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import center.domain.repository.TreasuryRepository
import center.domain.usecase.GetOverdueStudentsUseCase
import center.domain.usecase.GetTeacherCollectionDeficitUseCase
import center.domain.usecase.GetTeacherMonthlyDuesUseCase
import center.domain.usecase.GetTeacherMonthlySalaryUseCase
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.QueueReminderUseCase
import center.domain.usecase.RecordAttendanceUseCase
import center.domain.usecase.SyncPendingMessagesUseCase
import org.koin.dsl.koinApplication
import java.nio.file.Files
import kotlin.test.Test

class PersistenceModuleTest {
    @Test
    fun desktopKoinGraphResolvesEveryRepositoryAndUseCase() {
        val directory = Files.createTempDirectory("center-koin")
        try {
            openDatabase(directory).use { driver ->
                val app = koinApplication { modules(persistenceModule(driver)) }
                try {
                    val koin = app.koin
                    koin.get<StudentRepository>()
                    koin.get<TeacherRepository>()
                    koin.get<ClassGroupRepository>()
                    koin.get<PaymentRepository>()
                    koin.get<AttendanceRepository>()
                    koin.get<MessageQueueRepository>()
                    koin.get<MonthlyBillingRepository>()
                    koin.get<TreasuryRepository>()
                    koin.get<RegisterStudent>()
                    koin.get<ProcessPaymentUseCase>()
                    koin.get<RecordAttendanceUseCase>()
                    koin.get<GetTeacherMonthlyDuesUseCase>()
                    koin.get<GetTeacherMonthlySalaryUseCase>()
                    koin.get<GetTeacherCollectionDeficitUseCase>()
                    koin.get<GetOverdueStudentsUseCase>()
                    koin.get<QueueReminderUseCase>()
                    koin.get<SyncPendingMessagesUseCase>()
                } finally {
                    app.close()
                }
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
