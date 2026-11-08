package center.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import center.feature.billing.BillingScreen
import center.feature.reception.ReceptionScreen
import center.feature.reports.ReportsScreen
import center.feature.students.StudentsScreen

@Composable
fun App(root: RootComponent) {
    val state = remember(root) { mutableStateOf(root.stack.value) }
    DisposableEffect(root) {
        val subscription = root.stack.subscribe { state.value = it }
        onDispose { subscription.cancel() }
    }
    val stack by state
    MaterialTheme {
        Column {
            Row {
                Destination.entries.forEach { destination ->
                    TextButton(onClick = { root.navigate(destination) }) { Text(destination.name) }
                }
            }
            when (stack.active.instance) {
                Destination.Reception -> ReceptionScreen()
                Destination.Billing -> BillingScreen()
                Destination.Students -> StudentsScreen()
                Destination.Reports -> ReportsScreen()
            }
        }
    }
}
