package center.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import center.app.resources.Res
import center.app.resources.home
import center.feature.billing.BillingScreen
import center.feature.reception.ReceptionScreen
import center.feature.reports.ReportsScreen
import center.ui.theme.CenterOneTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(root: RootComponent) {
    val state = remember(root) { mutableStateOf(root.stack.value) }
    DisposableEffect(root) {
        val subscription = root.stack.subscribe { state.value = it }
        onDispose { subscription.cancel() }
    }
    val stack by state
    CenterOneTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (stack.active.instance == Destination.Dashboard) {
                DashboardRoute(root)
            } else if (stack.active.instance == Destination.Financials) {
                FinancialsRoute(root)
            } else if (stack.active.instance == Destination.Students) {
                StudentsRoute(root)
            } else if (stack.active.instance == Destination.Courses) {
                CoursesRoute(root)
            } else {
                Column {
                    Row {
                        Destination.entries.forEach { destination ->
                            TextButton(onClick = { root.navigate(destination) }) {
                                Text(
                                    if (destination ==
                                        Destination.Dashboard
                                    ) {
                                        stringResource(Res.string.home)
                                    } else {
                                        destination.name
                                    },
                                )
                            }
                        }
                    }
                    when (stack.active.instance) {
                        Destination.Dashboard, Destination.Financials, Destination.Students, Destination.Courses -> Unit
                        Destination.Reception -> ReceptionScreen()
                        Destination.Billing -> BillingScreen()
                        Destination.Reports -> ReportsScreen()
                    }
                }
            }
        }
    }
}
