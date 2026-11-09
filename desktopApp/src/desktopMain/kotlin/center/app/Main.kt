package center.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import center.database.CenterDaos
import center.database.CenterDatabase
import center.database.SqlStudentRepository
import center.database.openDatabase
import center.database.repository.SqlAttendanceRepository
import center.database.repository.SqlMessageQueueRepository
import center.database.repository.SqlPaymentRepository
import center.database.repository.SqlTeacherRepository
import center.domain.RegisterStudent
import center.domain.StudentRepository
import center.domain.repository.AttendanceRepository
import center.domain.repository.MessageQueueRepository
import center.domain.repository.PaymentRepository
import center.domain.repository.TeacherRepository
import center.domain.usecase.EpochClock
import center.domain.usecase.GetTeacherMonthlyDuesUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.RecordAttendanceUseCase
import center.domain.usecase.SyncPendingMessagesUseCase
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.nio.file.Path
import java.util.UUID

fun main() {
    val directory = Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "CenterSystem")
    val driver = openDatabase(directory)
    val koin =
        startKoin {
            modules(
                module {
                    single { CenterDatabase(driver) }
                    single { CenterDaos(get(), Dispatchers.IO) }
                    single { get<CenterDaos>().teachers }
                    single { get<CenterDaos>().students }
                    single { get<CenterDaos>().classGroups }
                    single { get<CenterDaos>().attendance }
                    single { get<CenterDaos>().payments }
                    single { get<CenterDaos>().messages }
                    single<StudentRepository> { SqlStudentRepository(get()) }
                    single<TeacherRepository> { SqlTeacherRepository(get(), Dispatchers.IO) }
                    single<PaymentRepository> { SqlPaymentRepository(get(), Dispatchers.IO) }
                    single<AttendanceRepository> { SqlAttendanceRepository(get(), Dispatchers.IO) }
                    single<MessageQueueRepository> { SqlMessageQueueRepository(get(), Dispatchers.IO) }
                    single { EpochClock { System.currentTimeMillis() } }
                    single { IdGenerator { UUID.randomUUID().toString() } }
                    factory { ProcessPaymentUseCase(get(), get(), get(), get(), get()) }
                    factory { RecordAttendanceUseCase(get(), get(), get(), get(), get()) }
                    factory { GetTeacherMonthlyDuesUseCase(get()) }
                    factory { SyncPendingMessagesUseCase(get()) }
                    factory { RegisterStudent(get()) }
                },
            )
        }
    try {
        application {
            val lifecycle = remember { LifecycleRegistry() }
            val root = remember { RootComponent(DefaultComponentContext(lifecycle)) }
            DisposableEffect(lifecycle) {
                lifecycle.resume()
                onDispose { lifecycle.destroy() }
            }
            Window(
                onCloseRequest = ::exitApplication,
                title = "Center One",
                state = rememberWindowState(width = 1440.dp, height = 960.dp),
            ) {
                App(root)
            }
        }
    } finally {
        koin.close()
        driver.close()
    }
}
