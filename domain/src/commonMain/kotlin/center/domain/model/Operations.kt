package center.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

enum class EducationStage { PRIMARY, PREP, SECONDARY }

enum class EducationSystem { ARABIC, LANGUAGES, BOTH }

enum class LessonType { ARABIC, LANGUAGES, MIXED, PRIVATE }

enum class GroupStatus { ACTIVE, ARCHIVED }

data class GroupScheduleSlot(
    val day: DayOfWeek,
    val from: LocalTime,
    val to: LocalTime,
) {
    init {
        require(from < to && from.second == 0 && to.second == 0 && from.nanosecond == 0 && to.nanosecond == 0) {
            "Schedule slots need increasing, minute-precision times"
        }
    }
}

data class ClassGroup(
    val id: String,
    val name: String,
    val teacherId: String,
    /** Minor units (piastres). Null for legacy groups awaiting monthly setup. */
    val monthlyPriceMinor: Long?,
    /** 7000 basis points is 70%. Null for legacy groups awaiting setup. */
    val commissionBasisPoints: Int?,
    val roomNumber: Int?,
    val system: EducationSystem,
    val status: GroupStatus,
    val schedule: List<GroupScheduleSlot> = emptyList(),
    val gradeId: String? = null,
    val maxCapacity: Int? = null,
    val lessonType: LessonType = when (system) {
        EducationSystem.ARABIC -> LessonType.ARABIC
        EducationSystem.LANGUAGES -> LessonType.LANGUAGES
        EducationSystem.BOTH -> LessonType.MIXED
    },
    /** Languages track price in minor units, only used by mixed lessons. */
    val languagesPriceMinor: Long? = null,
)

data class SubscriptionBalance(
    val studentId: String,
    val studentCode: String,
    val studentName: String,
    val parentPhone: String,
    val groupId: String,
    val groupName: String,
    val teacherId: String,
    val teacherName: String,
    val remainingMinor: Long,
)

data class CollectionDeficit(
    val studentCount: Int,
    val totalMinor: Long,
)

enum class LedgerDirection { INCOME, EXPENSE }

enum class LedgerKind { STUDENT_PAYMENT, OPERATIONAL, TEACHER_PAYOUT }

data class LedgerEntry(
    val id: String,
    val direction: LedgerDirection,
    val kind: LedgerKind,
    val amountMinor: Long,
    val recordedAt: Long,
    val description: String,
    val studentId: String? = null,
    val groupId: String? = null,
    val teacherId: String? = null,
)

data class TreasuryExpense(
    val id: String,
    val kind: LedgerKind,
    val amountMinor: Long,
    val recordedAt: Long,
    val description: String,
    val teacherId: String? = null,
) {
    init {
        require(id.isNotBlank() && description.isNotBlank() && amountMinor > 0)
        require(kind != LedgerKind.STUDENT_PAYMENT)
        require(if (kind == LedgerKind.TEACHER_PAYOUT) !teacherId.isNullOrBlank() else teacherId == null)
    }
}

data class TreasuryTotals(
    val todayIncomeMinor: Long,
    val todayExpenseMinor: Long,
    val drawerBalanceMinor: Long,
    val monthNetMinor: Long,
)
