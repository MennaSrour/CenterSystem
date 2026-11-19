package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.DashboardPreviewData
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardText
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import kotlinx.coroutines.launch

@Composable
internal fun DashboardRoute(
    root: RootComponent,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    var state by remember { mutableStateOf(DashboardPreviewData.state()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(language) { snackbar.currentSnackbarData?.dismiss() }

    CenterOneLocalization(language) {
        Box(Modifier.fillMaxSize()) {
            DashboardScreen(state.copy(language = language), onEvent = { event ->
                when (event) {
                    is DashboardEvent.QueryChanged -> state = state.copy(query = event.value, student = null, searchSubmitted = false)
                    DashboardEvent.Search ->
                        if (state.query.isNotBlank()) {
                            state = state.copy(student = DashboardPreviewData.findStudent(state.query), searchSubmitted = true)
                        }
                    is DashboardEvent.RegisterAttendance -> {
                        scope.launch { snackbar.showSnackbar(DashboardText.ATTENDANCE_DEMO.text(language)) }
                    }
                    is DashboardEvent.PaySubscription -> root.open(ReceptionShortcut.CollectSubscription(event.studentCode))
                    DashboardEvent.AddStudent -> root.open(ReceptionShortcut.AddStudent)
                    DashboardEvent.CollectSubscription -> root.open(ReceptionShortcut.CollectSubscription())
                    DashboardEvent.RecordExpense -> root.open(ReceptionShortcut.RecordExpense)
                    is DashboardEvent.Navigate -> root.navigate(event.destination)
                    is DashboardEvent.LanguageSelected -> onLanguageSelected(event.language)
                }
            })
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}
