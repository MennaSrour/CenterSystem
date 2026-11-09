package center.feature.financials

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import center.feature.financials.resources.Res
import center.feature.financials.resources.arabic
import center.feature.financials.resources.closed_treasury
import center.feature.financials.resources.english
import center.feature.financials.resources.expense_log
import center.feature.financials.resources.expense_transactions
import center.feature.financials.resources.expense_transactions_en
import center.feature.financials.resources.financials
import center.feature.financials.resources.financials_en
import center.feature.financials.resources.income_log
import center.feature.financials.resources.income_transactions
import center.feature.financials.resources.income_transactions_en
import center.feature.financials.resources.notifications
import center.feature.financials.resources.open_treasury
import center.feature.financials.resources.receive_payment
import center.feature.financials.resources.record_expense
import center.feature.financials.resources.search_records
import center.feature.financials.resources.summary
import center.feature.financials.resources.summary_subtitle
import center.feature.financials.resources.treasury_management
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTabs
import center.ui.components.CenterOneTextField
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import org.jetbrains.compose.resources.stringResource

/** Pure presentation: the owner supplies snapshots and handles every interaction. */
@Composable
fun FinancialsScreen(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier.fillMaxSize()) {
            FinancialsContent(state, onEvent)
            if (state.modal != FinancialsModal.Closed) {
                Box(Modifier.matchParentSize().background(Navy.copy(alpha = 0.55f)))
            }
            when (val modal = state.modal) {
                FinancialsModal.Closed -> Unit
                is FinancialsModal.ReceivePayment -> ReceivePaymentModal(modal.form, onEvent)
                is FinancialsModal.RecordExpense -> RecordExpenseModal(modal.form, onEvent)
            }
        }
    }
}

@Composable
internal fun FinancialsContent(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = maxOf(maxWidth, 1080.dp)
            val sidebarWidth = if (contentWidth >= 1600.dp) 300.dp else 250.dp
            Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
                Column(Modifier.weight(1f)) {
                    FinancialsHeader(state, onEvent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(CenterOneSpacing.Section),
                        verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Large),
                    ) {
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(Res.string.treasury_management),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(stringResource(Res.string.summary), style = MaterialTheme.typography.headlineLarge)
                                    Text(
                                        stringResource(Res.string.summary_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                StatusBadge(
                                    stringResource(if (state.isTreasuryOpen) Res.string.open_treasury else Res.string.closed_treasury),
                                    if (state.isTreasuryOpen) StatusTone.Success else StatusTone.Pending,
                                )
                            }
                        }
                        item { FinancialsStats(state.summary, Modifier.fillMaxWidth()) }
                        item {
                            CenterOneCard(Modifier.fillMaxWidth()) {
                                FinancialsActionBar(state, onEvent)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                Row(
                                    Modifier.fillMaxWidth().padding(CenterOneSpacing.Large),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    BilingualHeading(
                                        stringResource(
                                            if (state.selectedLog ==
                                                FinancialLog.Income
                                            ) {
                                                Res.string.income_transactions
                                            } else {
                                                Res.string.expense_transactions
                                            },
                                        ),
                                        stringResource(
                                            if (state.selectedLog ==
                                                FinancialLog.Income
                                            ) {
                                                Res.string.income_transactions_en
                                            } else {
                                                Res.string.expense_transactions_en
                                            },
                                        ),
                                        Modifier.weight(1f),
                                    )
                                    CenterOneTextField(state.recordQuery, {
                                        onEvent(FinancialsEvent.RecordQueryChanged(it))
                                    }, Modifier.width(260.dp), placeholder = {
                                        Text(stringResource(Res.string.search_records), style = MaterialTheme.typography.bodySmall)
                                    }, leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) })
                                }
                                RecentActivityTable(state.visibleRecords, state.selectedLog, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                FinancialsSidebar(state, onEvent, Modifier.width(sidebarWidth))
            }
        }
    }
}

@Composable
private fun FinancialsActionBar(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(CenterOneSpacing.Medium)) {
        val wide = maxWidth >= 1100.dp
        val tabs: @Composable (Modifier) -> Unit = { modifier ->
            CenterOneTabs(
                labels = listOf(stringResource(Res.string.income_log), stringResource(Res.string.expense_log)),
                selectedIndex = FinancialLog.entries.indexOf(state.selectedLog),
                onSelected = { onEvent(FinancialsEvent.TabSelected(FinancialLog.entries[it])) },
                modifier = modifier,
            )
        }
        val buttons: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CenterOnePrimaryButton(
                    onClick = { onEvent(FinancialsEvent.OpenReceivePayment) },
                ) { Text(stringResource(Res.string.receive_payment)) }
                CenterOneOutlinedButton(
                    onClick = { onEvent(FinancialsEvent.OpenRecordExpense) },
                ) { Text(stringResource(Res.string.record_expense)) }
            }
        }
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                tabs(Modifier.weight(1f))
                buttons()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                tabs(Modifier.fillMaxWidth())
                buttons()
            }
        }
    }
}

@Composable
private fun FinancialsHeader(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(state.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BilingualHeading(stringResource(Res.string.financials), stringResource(Res.string.financials_en))
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(stringResource(Res.string.arabic), Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = { onEvent(FinancialsEvent.RequestEnglish) }) { Text(stringResource(Res.string.english)) }
        IconButton(onClick = {
            onEvent(FinancialsEvent.OpenNotifications)
        }) { Icon(Icons.Outlined.Notifications, contentDescription = stringResource(Res.string.notifications)) }
        Text(state.userName, style = MaterialTheme.typography.titleSmall)
    }
}
