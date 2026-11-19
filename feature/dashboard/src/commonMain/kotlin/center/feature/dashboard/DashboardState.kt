package center.feature.dashboard

import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText

data class DashboardState(
    val attendance: String,
    val treasury: String,
    val query: String = "",
    val student: QuickStudent? = null,
    val searchSubmitted: Boolean = false,
    val language: AppLanguage = AppLanguage.Arabic,
)

data class QuickStudent(
    val code: String,
    val name: LocalizedText,
    val grade: LocalizedText,
    val paymentStatus: PaymentStatus,
)

enum class PaymentStatus { Paid, Overdue }

object DashboardPreviewData {
    val student =
        QuickStudent(
            code = "10234",
            name = LocalizedText("يوسف أحمد", "Youssef Ahmed"),
            grade = LocalizedText("الصف الثالث الثانوي", "Grade 12"),
            paymentStatus = PaymentStatus.Overdue,
        )

    fun state(language: AppLanguage = AppLanguage.Arabic) =
        DashboardState(attendance = "42", treasury = "8,300", student = student, language = language)

    fun findStudent(query: String): QuickStudent? {
        if (query.isBlank()) return null
        val normalized = query.trim().map { char -> char.digitToIntOrNull()?.digitToChar() ?: char }.joinToString("")
        return student.takeIf {
            it.code == normalized ||
                it.name.arabic.contains(normalized) ||
                it.name.english.contains(normalized, ignoreCase = true)
        }
    }
}
