package center.feature.reports

import center.domain.usecase.MonthlyCenterReport
import center.ui.localization.AppLanguage
import kotlinx.datetime.LocalDate
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

/** Renders Arabic using Java2D TextLayout (joining and bidi shaping), with the same TTF embedded in the PDF. */
class MonthlyReportPdfRenderer {
    fun render(
        report: MonthlyCenterReport,
        language: AppLanguage,
        generatedAt: LocalDate,
    ): ByteArray {
        val reportFont = bundledFont()
        PDDocument().use { document ->
            val embeddedFont =
                MonthlyReportPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
                    PDType0Font.load(document, it, true)
                }
            val pages = PageWriter(document, reportFont, embeddedFont, report, language, generatedAt)
            pages.write()
            pages.finish()
            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }

    private fun bundledFont(): Font =
        MonthlyReportPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
            Font.createFont(Font.TRUETYPE_FONT, it)
        }

    private class PageWriter(
        private val document: PDDocument,
        private val baseFont: Font,
        private val embeddedFont: PDType0Font,
        private val report: MonthlyCenterReport,
        private val language: AppLanguage,
        private val generatedAt: LocalDate,
    ) {
        private val pageWidth = PDRectangle.A4.width
        private val pageHeight = PDRectangle.A4.height
        private val margin = 42f
        private val footerLimit = 60f
        private var page: PDPage? = null
        private var y = 0f
        private var currentTableHeader: String? = null
        private val fontRenderContext = FontRenderContext(null, true, true)

        fun write() {
            newPage()
            section(text("treasury"))
            addLine(text("collections", report.treasury.collectionsMinor.money()))
            addLine(text("expenses", report.treasury.expensesMinor.money()))
            addLine(text("teacherPayouts", report.treasury.teacherPayoutsMinor.money()))
            addLine(text("staffSalaries", report.treasury.staffSalariesMinor.money()))
            addLine(text("netProfit", report.treasury.netProfitMinor.money()), bold = true)
            addLine(text("overdue", report.treasury.overdueMinor.money()))

            section(text("teachers"))
            tableHeader(text("teacherColumns"))
            report.teachers.forEach { teacher ->
                ensureTableRow()
                addLine(
                    "${teacher.id} · ${teacher.name} · ${text("lessons", teacher.lessonCount)} · " +
                        "${text("tracks", teacher.arabicStudentCount, teacher.languagesStudentCount, teacher.privateStudentCount)} · " +
                        "${text("attendanceHours", teacher.attendanceDays, teacher.workedMinutes.hours())} · " +
                        "${text("salaryPaid", teacher.expectedSalaryMinor.money(), teacher.paidMinor.money())}",
                    size = 8f,
                )
            }

            section(text("studentSummary"))
            addLine(text("activeStudents", report.studentsSummary.activeCount))
            addLine(text("attendanceCount", report.studentsSummary.attendanceCount))
            addLine(text("paidUnpaid", report.studentsSummary.paidCount, report.studentsSummary.unpaidCount))
            section(text("students"))
            tableHeader(text("studentColumns"))
            report.students.forEach { student ->
                ensureTableRow()
                addLine(
                    "${student.code} · ${student.name} · ${text("gradeLessons", student.grade, student.lessonCount)} · " +
                        "${text(
                            "studentBalances",
                            student.monthlyDueMinor.money(),
                            student.paidMinor.money(),
                            student.remainingMinor.money(),
                        )}",
                    size = 8f,
                )
            }

            section(text("staff"))
            tableHeader(text("staffColumns"))
            report.staff.forEach { member ->
                ensureTableRow()
                addLine(
                    "${member.id} · ${member.name} · ${member.role} · ${text(
                        "attendanceHours",
                        member.attendanceDays,
                        member.workedMinutes.hours(),
                    )} · " +
                        text("salary", member.monthlySalaryMinor.money()),
                    size = 8f,
                )
            }
        }

        fun finish() {
            document.pages.forEachIndexed { index, item ->
                PDPageContentStream(document, item, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                    val label = text("page", index + 1, document.numberOfPages)
                    drawText(stream, label, margin, 32f, 9f, Color(0x68758A), false)
                }
            }
        }

        private fun section(title: String) {
            if (y < 100f) newPage()
            addLine(title, size = 13f, bold = true, color = Color(0x2B4162), gapAfter = 3f)
        }

        private fun tableHeader(title: String) {
            if (y < footerLimit + 48f) newPage()
            currentTableHeader = title
            addLine(title, size = 9f, bold = true, color = Color(0x2B4162), gapAfter = 2f)
            rule()
        }

        private fun ensureTableRow() {
            if (y < footerLimit + 48f) {
                newPage()
                currentTableHeader?.let { header ->
                    addLine(header, size = 9f, bold = true, color = Color(0x2B4162), gapAfter = 2f)
                    rule()
                }
            }
        }

        private fun addLine(
            value: String,
            size: Float = 10f,
            bold: Boolean = false,
            color: Color = Color(0x25354C),
            gapAfter: Float = 0f,
        ) {
            val lineHeight = size + 8f
            if (y - lineHeight < footerLimit) newPage()
            val target = requireNotNull(page)
            PDPageContentStream(document, target, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                if (bold) {
                    stream.setNonStrokingColor(Color(0xF2C66D))
                    stream.addRect(margin, y - 5f, 3f, lineHeight)
                    stream.fill()
                }
                drawText(stream, value, margin + if (bold) 10f else 0f, y, size, color, bold)
            }
            y -= lineHeight + gapAfter
        }

        private fun drawText(
            stream: PDPageContentStream,
            value: String,
            x: Float,
            baseline: Float,
            size: Float,
            color: Color,
            bold: Boolean,
        ) {
            val awtFont = baseFont.deriveFont(if (bold) Font.BOLD else Font.PLAIN, size * 2f)
            val layout = TextLayout(value, awtFont, fontRenderContext)
            val imageWidth = ceil(layout.advance).toInt().coerceAtLeast(1) + 8
            val imageHeight = ceil(layout.ascent + layout.descent + layout.leading).toInt().coerceAtLeast(1) + 8
            val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                graphics.color = color
                layout.draw(graphics, 4f, 4f + layout.ascent)
            } finally {
                graphics.dispose()
            }
            val pdImage = LosslessFactory.createFromImage(document, image)
            val visualWidth = pdImage.width / 2f
            val visualHeight = pdImage.height / 2f
            stream.drawImage(pdImage, x, baseline - visualHeight + 2f, visualWidth, visualHeight)
        }

        private fun rule() {
            val target = requireNotNull(page)
            PDPageContentStream(document, target, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                stream.setStrokingColor(Color(0xD8DFE9))
                stream.moveTo(margin, y + 2f)
                stream.lineTo(pageWidth - margin, y + 2f)
                stream.stroke()
            }
        }

        private fun newPage() {
            val newPage = PDPage(PDRectangle.A4)
            document.addPage(newPage)
            page = newPage
            y = pageHeight - margin
            PDPageContentStream(document, newPage).use { stream ->
                stream.setNonStrokingColor(Color(0x2B4162))
                stream.addRect(margin, pageHeight - 76f, 28f, 28f)
                stream.fill()
                // Keeping a searchable embedded font entry lets PDF readers map Latin labels while the visible
                // Arabic glyphs are shaped through TextLayout and embedded as lossless glyph images below.
                stream.beginText()
                stream.setFont(embeddedFont, 1f)
                stream.setRenderingMode(RenderingMode.NEITHER)
                stream.newLineAtOffset(0f, 0f)
                stream.showText("Center One")
                stream.endText()
                drawText(stream, "1", margin + 10f, pageHeight - 66f, 12f, Color.WHITE, true)
                drawText(stream, "Center One", margin + 38f, pageHeight - 55f, 13f, Color(0x2B4162), true)
                drawText(stream, text("reportTitle"), margin + 38f, pageHeight - 72f, 9f, Color(0x68758A), false)
                val monthTitle = text("month", report.month.year, report.month.monthNumber)
                drawText(stream, monthTitle, pageWidth - margin - 140f, pageHeight - 55f, 11f, Color(0x25354C), true)
                drawText(
                    stream,
                    text("generated", generatedAt.toString()),
                    pageWidth - margin - 140f,
                    pageHeight - 72f,
                    8f,
                    Color(0x68758A),
                    false,
                )
                stream.setStrokingColor(Color(0xD8DFE9))
                stream.moveTo(margin, pageHeight - 90f)
                stream.lineTo(pageWidth - margin, pageHeight - 90f)
                stream.stroke()
            }
            y -= 63f
        }

        private fun text(
            key: String,
            vararg args: Any,
        ): String {
            val arabic = language == AppLanguage.Arabic
            return when (key) {
                "reportTitle" -> if (arabic) "التقرير الشهري الشامل" else "Monthly Center Report"
                "month" ->
                    if (arabic) {
                        "${args[1]} / ${args[0]}"
                    } else {
                        "${monthName(args[1] as Int)} ${args[0]}"
                    }
                "generated" -> if (arabic) "تاريخ الإنشاء ${args[0]}" else "Generated ${args[0]}"
                "treasury" -> if (arabic) "ملخص الخزنة والسنتر" else "Center and Treasury Summary"
                "collections" -> if (arabic) "إجمالي التحصيل: ${args[0]}" else "Student collections: ${args[0]}"
                "expenses" -> if (arabic) "المصروفات التشغيلية: ${args[0]}" else "Operating expenses: ${args[0]}"
                "teacherPayouts" -> if (arabic) "مستحقات المدرسين المصروفة: ${args[0]}" else "Teacher payouts: ${args[0]}"
                "staffSalaries" -> if (arabic) "رواتب الموظفين: ${args[0]}" else "Staff salaries: ${args[0]}"
                "netProfit" -> if (arabic) "صافي الربح: ${args[0]}" else "Net profit: ${args[0]}"
                "overdue" -> if (arabic) "المتأخرات: ${args[0]}" else "Overdue balance: ${args[0]}"
                "teachers" -> if (arabic) "تقرير المدرسين" else "Teachers breakdown"
                "teacherColumns" ->
                    if (arabic) {
                        "كود المدرس · الاسم · الدروس · الطلاب حسب المسار · الحضور · المرتب"
                    } else {
                        "Teacher ID · Name · Lessons · Students by track · Attendance · Salary"
                    }
                "lessons" -> if (arabic) "الدروس ${args[0]}" else "Lessons ${args[0]}"
                "tracks" ->
                    if (arabic) {
                        "عربي ${args[0]} · لغات ${args[1]} · برايفت ${args[2]}"
                    } else {
                        "Arabic ${args[0]} · Languages ${args[1]} · Private ${args[2]}"
                    }
                "attendanceHours" ->
                    if (arabic) {
                        "الحضور ${args[0]} يوم · ${args[1]} ساعة"
                    } else {
                        "Attendance ${args[0]} days · ${args[1]} hours"
                    }
                "salaryPaid" ->
                    if (arabic) {
                        "الراتب ${args[0]} · المصروف ${args[1]}"
                    } else {
                        "Expected ${args[0]} · Paid ${args[1]}"
                    }
                "studentSummary" -> if (arabic) "ملخص الطلاب" else "Student Summary"
                "activeStudents" -> if (arabic) "الطلاب النشطون: ${args[0]}" else "Active students: ${args[0]}"
                "attendanceCount" -> if (arabic) "سجلات الحضور: ${args[0]}" else "Attendance records: ${args[0]}"
                "paidUnpaid" ->
                    if (arabic) {
                        "المدفوع بالكامل ${args[0]} · عليهم متأخرات ${args[1]}"
                    } else {
                        "Fully paid ${args[0]} · outstanding ${args[1]}"
                    }
                "students" -> if (arabic) "تقرير الطلاب" else "Students breakdown"
                "studentColumns" ->
                    if (arabic) {
                        "كود الطالب · الاسم · الصف · الدروس · المستحق · المدفوع · المتبقي"
                    } else {
                        "Student ID · Name · Grade · Lessons · Due · Paid · Remaining"
                    }
                "gradeLessons" -> if (arabic) "الصف ${args[0]} · الدروس ${args[1]}" else "Grade ${args[0]} · Lessons ${args[1]}"
                "studentBalances" ->
                    if (arabic) {
                        "المستحق ${args[0]} · المدفوع ${args[1]} · المتبقي ${args[2]}"
                    } else {
                        "Due ${args[0]} · Paid ${args[1]} · Remaining ${args[2]}"
                    }
                "staff" -> if (arabic) "تقرير الموظفين" else "Staff breakdown"
                "staffColumns" ->
                    if (arabic) {
                        "كود الموظف · الاسم · الوظيفة · الحضور · الراتب"
                    } else {
                        "Staff ID · Name · Role · Attendance · Salary"
                    }
                "salary" -> if (arabic) "الراتب ${args[0]}" else "Salary ${args[0]}"
                "page" -> if (arabic) "صفحة ${args[0]} من ${args[1]}" else "Page ${args[0]} of ${args[1]}"
                else -> key
            }
        }

        private fun String.money(): String = if (language == AppLanguage.Arabic) "$this ج.م" else "$this EGP"

        private fun Long.money(): String {
            val whole = this / 100
            val fraction = (this % 100).toString().padStart(2, '0')
            return if (fraction == "00") whole.toString() else "$whole.$fraction"
        }

        private fun Long.hours(): String = "${this / 60}.${(this % 60) * 10 / 60}"

        private fun monthName(month: Int): String =
            listOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December",
            )[
                month -
                    1,
            ]
    }
}
