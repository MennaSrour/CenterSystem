package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.app.resources.Res
import center.app.resources.feature_unavailable
import center.domain.model.ClassGroup
import center.domain.model.LedgerDirection
import center.domain.model.LedgerEntry
import center.domain.model.LedgerKind
import center.domain.model.Student
import center.domain.model.SubscriptionBalance
import center.domain.model.Teacher
import center.domain.model.TreasuryExpense
import center.domain.usecase.salaryPayoutId
import center.feature.financials.ClassOption
import center.feature.financials.ExpenseType
import center.feature.financials.FinancialRecord
import center.feature.financials.FinancialSplit
import center.feature.financials.FinancialSummary
import center.feature.financials.FinancialsDestination
import center.feature.financials.FinancialsEvent
import center.feature.financials.FinancialsModal
import center.feature.financials.FinancialsScreen
import center.feature.financials.FinancialsState
import center.feature.financials.ReceivePaymentState
import center.feature.financials.RecordExpenseState
import center.feature.financials.StudentOption
import center.feature.financials.TeacherOption
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FinancialsRoute(
    root: RootComponent,
    services: DirectoryServices,
    shortcut: ReceptionShortcut?,
    canViewProfit: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(language)
    val holder = remember(services, scope, shortcut, language) { FinancialsStateHolder(services, scope, language, shortcut, canViewProfit) }
    val state by holder.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val unavailable = stringResource(Res.string.feature_unavailable)

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        FinancialsScreen(state, onEvent = { event ->
            when (event) {
                is FinancialsEvent.Navigate ->
                    when (event.destination) {
                        FinancialsDestination.Courses -> root.navigate(Destination.Courses)
                        FinancialsDestination.Home -> root.navigate(Destination.Dashboard)
                        FinancialsDestination.Students -> root.navigate(Destination.Students)
                        FinancialsDestination.Reports -> root.navigate(Destination.Reports)
                        FinancialsDestination.Financials -> Unit
                        FinancialsDestination.Teachers -> root.navigate(Destination.Teachers)
                        FinancialsDestination.Settings -> root.navigate(Destination.Settings)
                        FinancialsDestination.Schedule -> notify(unavailable)
                    }
                is FinancialsEvent.LanguageSelected -> onLanguageSelected(event.language)
                FinancialsEvent.OpenNotifications, FinancialsEvent.LogOut -> notify(unavailable)
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class FinancialsStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    private val language: AppLanguage,
    shortcut: ReceptionShortcut? = null,
    private val canViewProfit: Boolean,
) {
    private val state =
        MutableStateFlow(
            FinancialsState(FinancialSummary("0", "0", "0", if (canViewProfit) "0" else null), emptyList(), emptyList(), "", ""),
        )
    val uiState = state.asStateFlow()
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = messages.asSharedFlow()
    private var students = emptyMap<String, Student>()
    private var groups = emptyMap<String, ClassGroup>()
    private var teachers = emptyMap<String, Teacher>()
    private var balances = emptyMap<String, Map<String, SubscriptionBalance>>()
    private var dues = emptyMap<String, Long>()
    private var pendingShortcut = shortcut

    init {
        scope.launch {
            val treasury =
                services.dates().flatMapLatest { day ->
                    val dayEnd = (day + DatePeriod(days = 1)).startMillis()
                    combine(
                        services.treasury.observeTotals(day),
                        services.treasury.observeLedger(Long.MIN_VALUE, dayEnd),
                        services.billing.observeChanges(),
                    ) { totals, ledger, _ -> Triple(day, totals, ledger) }
                }
            combine(
                services.students.observeStudents(),
                services.groups.observeGroups(),
                services.teachers.observeTeachers(),
                treasury,
            ) { studentRows, groupRows, teacherRows, (day, totals, ledger) ->
                loadDirectory(studentRows, groupRows, teacherRows, day, ledger)
                val records = ledger.asReversed().map { it.direction to it.toRecord() }
                val opening = pendingShortcut
                pendingShortcut = null
                state.update { previous ->
                    val modal = if (opening != null) openShortcut(opening) else refreshModal(previous.modal)
                    previous.copy(
                        summary =
                            FinancialSummary(
                                totals.todayIncomeMinor.formattedMinor(),
                                totals.todayExpenseMinor.formattedMinor(),
                                totals.drawerBalanceMinor.formattedMinor(),
                                if (canViewProfit) totals.monthNetMinor.formattedMinor() else null,
                            ),
                        incomeRecords = records.filter { it.first == LedgerDirection.INCOME }.map { it.second },
                        expenseRecords = records.filter { it.first == LedgerDirection.EXPENSE }.map { it.second },
                        dateLabel = day.toString(),
                        modal = modal,
                    )
                }
            }.catch { messages.emit(error()) }.collect {}
        }
    }

    private suspend fun loadDirectory(
        studentRows: List<Student>,
        groupRows: List<ClassGroup>,
        teacherRows: List<Teacher>,
        day: LocalDate,
        ledger: List<LedgerEntry>,
    ) {
        val month = day.monthStart()
        val paidIds =
            ledger
                .asSequence()
                .filter { it.kind == LedgerKind.TEACHER_PAYOUT }
                .map { it.id }
                .toSet()
        val monthBalances = services.billing.remainingBalances(month)
        val currentDues =
            teacherRows.associate { teacher ->
                val amount = if (salaryPayoutId(teacher.id, month) in paidIds) 0 else services.salary(teacher.id).getOrThrow()
                teacher.id to amount
            }
        students = studentRows.associateBy { it.id }
        groups = groupRows.associateBy { it.id }
        teachers = teacherRows.associateBy { it.id }
        balances = monthBalances.groupBy { it.studentId }.mapValues { (_, rows) -> rows.associateBy { it.groupId } }
        dues = currentDues
    }

    fun onEvent(event: FinancialsEvent) {
        when (event) {
            is FinancialsEvent.TabSelected -> state.update { it.copy(selectedLog = event.log) }
            is FinancialsEvent.RecordQueryChanged -> state.update { it.copy(recordQuery = event.value) }
            FinancialsEvent.OpenReceivePayment -> setModal(paymentModal())
            FinancialsEvent.OpenRecordExpense -> setModal(expenseModal())
            FinancialsEvent.DismissModal -> dismiss()
            is FinancialsEvent.StudentQueryChanged ->
                editPayment {
                    it.copy(
                        studentQuery = event.value,
                        selectedStudentId = null,
                        selectedClassId = null,
                        amount = "",
                        classMenuExpanded = false,
                    )
                }
            is FinancialsEvent.StudentSelected ->
                editPayment {
                    val student =
                        students[event.id]?.takeIf { row ->
                            row.isActive &&
                                it.studentResults.any { option -> option.id == row.id }
                        }
                    if (student ==
                        null
                    ) {
                        it
                    } else {
                        it.copy(studentQuery = student.fullName, selectedStudentId = student.id, selectedClassId = null, amount = "")
                    }
                }
            is FinancialsEvent.ClassMenuChanged -> editPayment { it.copy(classMenuExpanded = event.expanded) }
            is FinancialsEvent.ClassSelected ->
                editPayment { form ->
                    val due = balances[form.selectedStudentId]?.get(event.id) ?: return@editPayment form
                    form.copy(selectedClassId = event.id, amount = due.remainingMinor.decimalMinor(), classMenuExpanded = false)
                }
            is FinancialsEvent.PaymentAmountChanged -> editPayment { it.copy(amount = event.value) }
            FinancialsEvent.ConfirmReceivePayment -> savePayment()
            is FinancialsEvent.ExpenseTypeSelected ->
                editExpense { it.copy(type = event.type, selectedTeacherId = null, teacherMenuExpanded = false, amount = "") }
            is FinancialsEvent.TeacherMenuChanged -> editExpense { it.copy(teacherMenuExpanded = event.expanded) }
            is FinancialsEvent.TeacherSelected ->
                editExpense { if (event.id in teachers) it.copy(selectedTeacherId = event.id, teacherMenuExpanded = false) else it }
            is FinancialsEvent.ExpenseAmountChanged ->
                editExpense { if (it.type == ExpenseType.Operational) it.copy(amount = event.value) else it }
            is FinancialsEvent.ExpenseNotesChanged -> editExpense { it.copy(notes = event.value) }
            FinancialsEvent.AttachReceipt -> setExpenseMessage(label("إرفاق الإيصال غير متاح.", "Receipt attachment is unavailable."))
            FinancialsEvent.RemoveAttachment -> editExpense { it.copy(attachmentName = null) }
            FinancialsEvent.ConfirmExpense -> saveExpense()
            else -> Unit
        }
    }

    private fun openShortcut(shortcut: ReceptionShortcut): FinancialsModal =
        when (shortcut) {
            is ReceptionShortcut.CollectSubscription ->
                paymentModal(students.values.firstOrNull { it.isActive && it.code == shortcut.studentCode })
            is ReceptionShortcut.PayOverdue -> paymentModal(students[shortcut.student.id]?.takeIf { it.isActive }, shortcut.group.id)
            ReceptionShortcut.RecordExpense -> expenseModal()
            ReceptionShortcut.AddStudent -> FinancialsModal.Closed
        }

    private fun paymentModal(
        student: Student? = null,
        groupId: String? = null,
    ): FinancialsModal.ReceivePayment {
        val balance = balances[student?.id]?.get(groupId)
        val form =
            ReceivePaymentState(
                studentQuery = student?.fullName.orEmpty(),
                selectedStudentId = student?.id,
                selectedClassId = balance?.groupId,
                amount = balance?.remainingMinor?.decimalMinor().orEmpty(),
            )
        return FinancialsModal.ReceivePayment(refreshPayment(form))
    }

    private fun expenseModal(): FinancialsModal.RecordExpense =
        FinancialsModal.RecordExpense(refreshExpense(RecordExpenseState(type = ExpenseType.Operational)))

    private fun refreshModal(modal: FinancialsModal): FinancialsModal =
        when (modal) {
            is FinancialsModal.ReceivePayment ->
                if (modal.form.isSubmitting) {
                    modal
                } else {
                    FinancialsModal.ReceivePayment(
                        refreshPayment(modal.form),
                    )
                }
            is FinancialsModal.RecordExpense ->
                if (modal.form.isSubmitting) {
                    modal
                } else {
                    FinancialsModal.RecordExpense(
                        refreshExpense(modal.form),
                    )
                }
            FinancialsModal.Closed -> modal
        }

    private fun refreshPayment(form: ReceivePaymentState): ReceivePaymentState {
        val query = form.studentQuery.normalizeSearch()
        val activeStudent = students[form.selectedStudentId]?.takeIf { it.isActive }
        val results =
            students.values
                .filter { row ->
                    row.isActive &&
                        (row.code.normalizeSearch().contains(query, true) || row.fullName.normalizeSearch().contains(query, true))
                }.map { StudentOption(it.id, it.fullName, it.code) }
        val options = balances[activeStudent?.id].orEmpty().values.map { ClassOption(it.groupId, it.groupName) }
        val balance = balances[activeStudent?.id]?.get(form.selectedClassId)
        val commission = groups[balance?.groupId]?.commissionBasisPoints
        val amount = form.amount.parseMinor()
        val valid = amount != null && balance != null && commission != null && amount in 1..balance.remainingMinor
        val split =
            if (amount != null && commission != null && valid) {
                val teacher = (amount * commission + 5_000) / 10_000
                FinancialSplit(money(amount), money(teacher), money(amount - teacher), "${commission.toLong().decimalMinor()}%")
            } else {
                null
            }
        return form.copy(
            studentResults = results,
            selectedStudentId = activeStudent?.id,
            classes = options,
            selectedClassId = balance?.groupId,
            canConfirm = valid,
            split = split,
            classError =
                if (activeStudent != null &&
                    options.isEmpty()
                ) {
                    label("لا يوجد رصيد مستحق هذا الشهر.", "No balance due this month.")
                } else {
                    null
                },
            amountError =
                if (form.amount.isNotBlank() && balance != null && !valid) {
                    label(
                        "أدخل مبلغاً موجباً لا يتجاوز ${money(balance.remainingMinor)}.",
                        "Enter a positive amount up to ${money(balance.remainingMinor)}.",
                    )
                } else {
                    null
                },
        )
    }

    private fun refreshExpense(form: RecordExpenseState): RecordExpenseState {
        val selected = teachers[form.selectedTeacherId]
        val salary = dues[selected?.id] ?: 0
        val payout = form.type == ExpenseType.TeacherPayout
        val amount = if (payout) salary.takeIf { it > 0 }?.decimalMinor().orEmpty() else form.amount
        return form.copy(
            teachers = teachers.values.map { TeacherOption(it.id, it.fullName, money(dues[it.id] ?: 0)) },
            selectedTeacherId = selected?.id,
            amount = amount,
            canConfirm = if (payout) selected != null && salary > 0 else (amount.parseMinor() ?: 0) > 0,
            teacherError =
                if (payout &&
                    selected != null &&
                    salary == 0L
                ) {
                    label("لا يوجد مرتب مستحق لهذا الشهر.", "No salary due for this month.")
                } else {
                    null
                },
        )
    }

    private fun editPayment(transform: (ReceivePaymentState) -> ReceivePaymentState) {
        val form = (state.value.modal as? FinancialsModal.ReceivePayment)?.form ?: return
        if (!form.isSubmitting) setModal(FinancialsModal.ReceivePayment(refreshPayment(transform(form).copy(message = null))))
    }

    private fun editExpense(transform: (RecordExpenseState) -> RecordExpenseState) {
        val form = (state.value.modal as? FinancialsModal.RecordExpense)?.form ?: return
        if (!form.isSubmitting) setModal(FinancialsModal.RecordExpense(refreshExpense(transform(form).copy(message = null))))
    }

    private fun setExpenseMessage(message: String) {
        val form = (state.value.modal as? FinancialsModal.RecordExpense)?.form ?: return
        if (!form.isSubmitting) setModal(FinancialsModal.RecordExpense(form.copy(message = message)))
    }

    private fun setModal(modal: FinancialsModal) {
        state.update { it.copy(modal = modal) }
    }

    private fun dismiss() {
        when (val modal = state.value.modal) {
            is FinancialsModal.ReceivePayment -> if (modal.form.isSubmitting) return
            is FinancialsModal.RecordExpense -> if (modal.form.isSubmitting) return
            FinancialsModal.Closed -> return
        }
        setModal(FinancialsModal.Closed)
    }

    private fun savePayment() {
        val form = (state.value.modal as? FinancialsModal.ReceivePayment)?.form ?: return
        if (!form.canConfirm || form.isSubmitting) return
        val student = students[form.selectedStudentId] ?: return
        val groupId = form.selectedClassId ?: return
        val amount = form.amount.parseMinor() ?: return
        val month = services.today().monthStart()
        setModal(FinancialsModal.ReceivePayment(form.copy(isSubmitting = true, message = null)))
        scope.launch {
            val result = services.processPayment(student.code, groupId, amount / 100.0, month)
            if (result.isSuccess) {
                setModal(FinancialsModal.Closed)
                messages.emit(
                    label("تم تسجيل الدفع وإضافة التأكيد إلى قائمة واتساب.", "Payment recorded and WhatsApp confirmation queued."),
                )
            } else {
                setModal(FinancialsModal.ReceivePayment(refreshPayment(form).copy(message = error())))
            }
        }
    }

    private fun saveExpense() {
        val form = (state.value.modal as? FinancialsModal.RecordExpense)?.form ?: return
        if (!form.canConfirm || form.isSubmitting) return
        setModal(FinancialsModal.RecordExpense(form.copy(isSubmitting = true, message = null)))
        scope.launch {
            val result =
                directoryResult {
                    if (form.type == ExpenseType.TeacherPayout) {
                        services.paySalary(requireNotNull(form.selectedTeacherId), form.notes).getOrThrow()
                    } else {
                        services.treasury.recordExpense(
                            TreasuryExpense(
                                services.ids.newId(),
                                LedgerKind.OPERATIONAL,
                                requireNotNull(form.amount.parseMinor()),
                                services.clock.nowMillis(),
                                form.notes.trim().ifEmpty { label("مصروف تشغيل", "Operational expense") },
                            ),
                        )
                    }
                }
            if (result.isSuccess) {
                setModal(FinancialsModal.Closed)
                messages.emit(label("تم تسجيل المصروف.", "Expense recorded."))
            } else {
                setModal(FinancialsModal.RecordExpense(refreshExpense(form).copy(message = error())))
            }
        }
    }

    private fun LedgerEntry.toRecord(): FinancialRecord {
        val dateTime = Instant.fromEpochMilliseconds(recordedAt).toLocalDateTime(Cairo)
        val category =
            when (kind) {
                LedgerKind.STUDENT_PAYMENT -> groups[groupId]?.name ?: label("اشتراك شهري", "Monthly subscription")
                LedgerKind.OPERATIONAL -> label("مصروف تشغيل", "Operational expense")
                LedgerKind.TEACHER_PAYOUT -> label("صرف مرتب", "Teacher payout")
            }
        val party =
            when (kind) {
                LedgerKind.STUDENT_PAYMENT -> students[studentId]?.fullName ?: studentId.orEmpty()
                LedgerKind.OPERATIONAL -> description
                LedgerKind.TEACHER_PAYOUT -> teachers[teacherId]?.fullName ?: teacherId.orEmpty()
            }
        val details = if (kind == LedgerKind.TEACHER_PAYOUT && description.isNotBlank()) "$category · $description" else category
        val sign = if (direction == LedgerDirection.INCOME) "+" else "−"
        return FinancialRecord(
            "${direction.name}:$id",
            party,
            "",
            details,
            "$sign ${money(amountMinor)}",
            dateTime.date.toString(),
            "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}",
        )
    }

    private fun money(amountMinor: Long): String = "${amountMinor.formattedMinor()} ${label("ج.م", "EGP")}"

    private fun label(
        arabic: String,
        english: String,
    ): String = if (language == AppLanguage.Arabic) arabic else english

    private fun error(): String =
        label("تعذر تنفيذ العملية. تحقق من البيانات وأعد المحاولة.", "Could not complete the operation. Check the details and try again.")
}
