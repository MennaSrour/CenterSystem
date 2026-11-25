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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import center.feature.financials.resources.Res
import center.feature.financials.resources.expense_log
import center.feature.financials.resources.expense_transactions
import center.feature.financials.resources.expense_transactions_en
import center.feature.financials.resources.financials
import center.feature.financials.resources.financials_en
import center.feature.financials.resources.income_log
import center.feature.financials.resources.income_transactions
import center.feature.financials.resources.income_transactions_en
import center.feature.financials.resources.notifications
import center.feature.financials.resources.receive_payment
import center.feature.financials.resources.record_expense
import center.feature.financials.resources.search_records
import center.feature.financials.resources.summary
import center.feature.financials.resources.summary_subtitle
import center.feature.financials.resources.treasury_management
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTabs
import center.ui.components.CenterOneTextField
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.theme.CenterOneSpacing
import center.ui.theme.ModalScrim
import org.jetbrains.compose.resources.stringResource

/** Pure presentation: the owner supplies snapshots and handles every interaction. */
@Composable
fun FinancialsScreen(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LocalAppLanguage.current.layoutDirection) {
        Box(modifier.fillMaxSize()) {
            FinancialsContent(state, onEvent)
            if (state.modal != FinancialsModal.Closed) {
                Box(Modifier.matchParentSize().background(ModalScrim))
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
    CenterOneCanvas(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = maxOf(maxWidth, 1080.dp)
            val sidebarWidth = if (contentWidth >= 1600.dp) 300.dp else 250.dp
            Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
                if (LocalAppLanguage.current == AppLanguage.English) FinancialsSidebar(onEvent, Modifier.width(sidebarWidth))
                Column(Modifier.weight(1f)) {
                    FinancialsHeader(state, onEvent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(CenterOneSpacing.Section),
                        verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Large),
                    ) {
                        item {
                            Column(Modifier.fillMaxWidth()) {
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
                        }
                        item { FinancialsStats(state.summary, Modifier.fillMaxWidth()) }
                        if (state.canViewMonthlyReport) {
                            item {
                                CenterOneCard(Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(CenterOneSpacing.Large),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                center.ui.localization
                                                    .LocalizedText(
                                                        "التقرير الشهري الشامل",
                                                        "Monthly Center Report",
                                                    ).resolve(LocalAppLanguage.current),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                center.ui.localization
                                                    .LocalizedText(
                                                        "عرض وتصدير التقرير المالي والتشغيلي",
                                                        "Preview and export the financial and operational report",
                                                    ).resolve(LocalAppLanguage.current),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        CenterOneOutlinedButton(
                                            onClick = { onEvent(FinancialsEvent.Navigate(FinancialsDestination.Reports)) },
                                        ) {
                                            Text(
                                                center.ui.localization
                                                    .LocalizedText(
                                                        "فتح التقرير",
                                                        "Open report",
                                                    ).resolve(LocalAppLanguage.current),
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                if (LocalAppLanguage.current == AppLanguage.Arabic) FinancialsSidebar(onEvent, Modifier.width(sidebarWidth))
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
    CenterOnePageHeader(
        stringResource(Res.string.financials),
        stringResource(Res.string.financials_en),
        state.dateLabel,
    ) {
        CenterOneLanguageToggle(LocalAppLanguage.current, { onEvent(FinancialsEvent.LanguageSelected(it)) })
        CenterOneIconButton(
            Icons.Outlined.Notifications,
            stringResource(Res.string.notifications),
            { onEvent(FinancialsEvent.OpenNotifications) },
        )
        Text(state.userName, style = MaterialTheme.typography.titleSmall)
    }
}
