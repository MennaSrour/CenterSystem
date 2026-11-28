package center.feature.reports

import center.ui.localization.AppLanguage
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

internal data class TimetablePdfLayout(
    val scale: Float,
    val rowHeights: List<Float>,
    val availableHeight: Float,
)

/** Single-column, paginated PDF renderer using the same bundled Almarai font and Java2D Arabic shaping. */
class ContextualPdfRenderer {
    fun renderTimetable(
        documentData: TimetablePdfDocument,
        language: AppLanguage,
    ): ByteArray {
        require(documentData.dayHeaders.size == 7)
        require(documentData.periods.all { it.dayLessons.size == 7 })
        val font = loadAwtFont()
        val layout = calculateTimetableLayout(documentData)
        val scale = layout.scale
        val heights = layout.rowHeights
        PDDocument().use { document ->
            val embeddedFont = loadPdfFont(document)
            val page = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                val pageWidth = page.mediaBox.width
                val pageHeight = page.mediaBox.height
                val margin = 24f
                val tableWidth = pageWidth - margin * 2
                val periodWidth = 104f
                val dayWidth = (tableWidth - periodWidth) / 7f
                val tableHeaderHeight = 23f
                val tableTop = pageHeight - 91f
                val frc = FontRenderContext(null, true, true)

                // Navy brand banner with a restrained gold rule, aligned to the theme palette.
                stream.setNonStrokingColor(Color(0x2B4162))
                stream.addRect(margin, pageHeight - 65f, tableWidth, 49f)
                stream.fill()
                drawText(document, stream, font, documentData.title, margin + 12f, pageHeight - 33f, 14f, Color.WHITE, true, frc)
                drawText(document, stream, font, "Center One", pageWidth - margin - 88f, pageHeight - 33f, 10f, Color.WHITE, true, frc)
                drawText(document, stream, font, documentData.subtitle, margin + 12f, pageHeight - 52f, 8.5f, Color(0xE7ECF3), false, frc)
                drawText(
                    document,
                    stream,
                    font,
                    documentData.generatedDate,
                    pageWidth - margin - 150f,
                    pageHeight - 52f,
                    8f,
                    Color(0xE7ECF3),
                    false,
                    frc,
                )
                stream.setNonStrokingColor(Color(0xD5A94F))
                stream.addRect(margin, pageHeight - 69f, tableWidth, 3f)
                stream.fill()

                var y = tableTop
                drawPdfCell(stream, margin, y - tableHeaderHeight, periodWidth, tableHeaderHeight, Color(0x344F76))
                drawWrappedText(
                    document,
                    stream,
                    font,
                    documentData.timeHeader,
                    margin + 5f,
                    y - 4f,
                    periodWidth - 10f,
                    7.4f,
                    Color.WHITE,
                    true,
                    frc,
                )
                documentData.dayHeaders.forEachIndexed { dayIndex, day ->
                    val x = margin + periodWidth + dayWidth * dayIndex
                    drawPdfCell(stream, x, y - tableHeaderHeight, dayWidth, tableHeaderHeight, Color(0x344F76))
                    drawWrappedText(document, stream, font, day, x + 4f, y - 4f, dayWidth - 8f, 7.4f, Color.WHITE, true, frc)
                }
                y -= tableHeaderHeight

                documentData.periods.forEachIndexed { index, period ->
                    val rowHeight = heights[index]
                    val rowBottom = y - rowHeight
                    val rowColor = if (index % 2 == 0) Color.WHITE else Color(0xF3F6FA)
                    drawPdfCell(stream, margin, rowBottom, periodWidth, rowHeight, Color(0xF0E8D4))
                    drawWrappedText(
                        document,
                        stream,
                        font,
                        "${period.periodLabel}\n${period.timeRange}",
                        margin + 5f,
                        y - 5f,
                        periodWidth - 10f,
                        8f * scale,
                        Color(0x2B4162),
                        true,
                        frc,
                    )
                    period.dayLessons.forEachIndexed { dayIndex, lessons ->
                        val x = margin + periodWidth + dayWidth * dayIndex
                        drawPdfCell(stream, x, rowBottom, dayWidth, rowHeight, rowColor)
                        var textTop = y - 5f * scale
                        lessons.forEachIndexed { lessonIndex, lesson ->
                            if (lessonIndex > 0) textTop -= 3f * scale
                            textTop =
                                drawWrappedText(
                                    document,
                                    stream,
                                    font,
                                    lesson.subject,
                                    x + 6f * scale,
                                    textTop,
                                    dayWidth - 12f * scale,
                                    9.2f * scale,
                                    Color(0x25354C),
                                    true,
                                    frc,
                                )
                            textTop =
                                drawWrappedText(
                                    document,
                                    stream,
                                    font,
                                    lesson.teacher,
                                    x + 6f * scale,
                                    textTop,
                                    dayWidth - 12f * scale,
                                    8.2f * scale,
                                    Color(0x536176),
                                    false,
                                    frc,
                                )
                            lesson.trackLabel?.let {
                                textTop =
                                    drawWrappedText(
                                        document,
                                        stream,
                                        font,
                                        it,
                                        x + 6f * scale,
                                        textTop,
                                        dayWidth - 12f * scale,
                                        7.6f * scale,
                                        Color(0x8A6A2D),
                                        false,
                                        frc,
                                    )
                            }
                        }
                    }
                    y = rowBottom
                }
                val footer = if (language == AppLanguage.Arabic) "جدول الحصص الأسبوعي" else "Weekly timetable"
                drawText(document, stream, font, footer, margin, 15f, 7f, Color(0x68758A), false, frc)
            }
            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }

    internal fun calculateTimetableLayout(documentData: TimetablePdfDocument): TimetablePdfLayout {
        val font = loadAwtFont()
        val frc = FontRenderContext(null, true, true)
        val pageWidth = PDRectangle.A4.height
        val pageHeight = PDRectangle.A4.width
        val margin = 24f
        val tableWidth = pageWidth - margin * 2
        val periodWidth = 104f
        val dayWidth = (tableWidth - periodWidth) / 7f
        val tableHeaderHeight = 23f
        val tableTop = pageHeight - 91f
        val tableBottom = 27f
        val availableHeight = tableTop - tableHeaderHeight - tableBottom

        fun rowHeights(scale: Float): List<Float> {
            val bodySize = 8.6f * scale
            val subjectSize = 9.2f * scale
            val lineHeight = bodySize * 1.28f
            return documentData.periods.map { period ->
                val maxCellHeight =
                    period.dayLessons.maxOfOrNull { lessons ->
                        var height = 8f * scale
                        lessons.forEachIndexed { index, lesson ->
                            if (index > 0) height += 4f * scale
                            height +=
                                wrappedLines(lesson.subject, font, subjectSize, dayWidth - 12f * scale, frc, bold = true).size * lineHeight
                            height += wrappedLines(lesson.teacher, font, bodySize, dayWidth - 12f * scale, frc).size * lineHeight
                            lesson.trackLabel?.let {
                                height += wrappedLines(it, font, bodySize * 0.92f, dayWidth - 12f * scale, frc).size * lineHeight
                            }
                        }
                        height
                    } ?: 0f
                val periodLineCount =
                    listOf(period.periodLabel, period.timeRange).sumOf {
                        wrappedLines(it, font, 8f * scale, periodWidth - 10f * scale, frc, bold = true).size
                    }
                val periodHeight = periodLineCount * lineHeight + 8f * scale
                maxOf(22f * scale, maxCellHeight, periodHeight)
            }
        }

        var scale = 1f
        var heights = rowHeights(scale)
        while (23f + heights.sum() > availableHeight && scale > 0.001f) {
            scale -= 0.005f
            heights = rowHeights(scale)
        }
        return TimetablePdfLayout(scale, heights, availableHeight)
    }

    private fun loadAwtFont(): Font =
        ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
            Font.createFont(Font.TRUETYPE_FONT, it)
        }

    private fun loadPdfFont(document: PDDocument): PDType0Font =
        ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
            PDType0Font.load(document, it, true)
        }

    private fun drawPdfCell(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fill: Color,
    ) {
        stream.setNonStrokingColor(fill)
        stream.addRect(x, y, width, height)
        stream.fill()
        stream.setStrokingColor(Color(0xB8C3D1))
        stream.setLineWidth(0.55f)
        stream.addRect(x, y, width, height)
        stream.stroke()
    }

    private fun drawWrappedText(
        document: PDDocument,
        stream: PDPageContentStream,
        font: Font,
        value: String,
        x: Float,
        top: Float,
        width: Float,
        size: Float,
        color: Color,
        bold: Boolean,
        frc: FontRenderContext,
    ): Float {
        var cursor = top
        val lineHeight = size * 1.28f
        value.split('\n').forEach { paragraph ->
            wrappedLines(paragraph, font, size, width, frc, bold).forEach { line ->
                drawText(document, stream, font, line, x, cursor - lineHeight / 2f, size, color, bold, frc)
                cursor -= lineHeight
            }
        }
        return cursor
    }

    private fun wrappedLines(
        value: String,
        font: Font,
        size: Float,
        width: Float,
        frc: FontRenderContext,
        bold: Boolean = false,
    ): List<String> {
        if (value.isEmpty()) return listOf("")
        val attributed = java.text.AttributedString(value)
        attributed.addAttribute(
            java.awt.font.TextAttribute.FONT,
            font.deriveFont(if (bold) Font.BOLD else Font.PLAIN, size * 2f),
        )
        val iterator = attributed.iterator
        val measurer = java.awt.font.LineBreakMeasurer(iterator, frc)
        val lines = mutableListOf<String>()
        while (measurer.position < iterator.endIndex) {
            val start = measurer.position
            val layout = measurer.nextLayout(width * 2f, iterator.endIndex, false) ?: break
            val end = measurer.position
            lines += value.substring(start, end).trim()
            if (end == start) break
        }
        return lines.ifEmpty { listOf(value.take(1)) }
    }

    fun render(
        title: String,
        lines: List<String>,
        language: AppLanguage,
    ): ByteArray {
        require(title.isNotBlank())
        val awtFont =
            ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
                Font.createFont(Font.TRUETYPE_FONT, it)
            }
        PDDocument().use { document ->
            val embeddedFont =
                ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
                    PDType0Font.load(document, it, true)
                }
            val pages = lines.ifEmpty { listOf("") }.chunked(36)
            pages.forEachIndexed { index, pageLines ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    val pageWidth = page.mediaBox.width
                    val margin = 42f
                    stream.setNonStrokingColor(Color(0x2B4162))
                    stream.addRect(margin, page.mediaBox.height - 74f, 26f, 26f)
                    stream.fill()
                    stream.beginText()
                    stream.setFont(embeddedFont, 1f)
                    stream.setRenderingMode(RenderingMode.NEITHER)
                    stream.newLineAtOffset(0f, 0f)
                    stream.showText("Center One")
                    stream.endText()
                    val frc = FontRenderContext(null, true, true)
                    drawText(document, stream, awtFont, title, margin + 36f, page.mediaBox.height - 57f, 14f, Color(0x2B4162), true, frc)
                    drawText(
                        document,
                        stream,
                        awtFont,
                        if (language == AppLanguage.Arabic) "Center One" else "Center One",
                        margin + 36f,
                        page.mediaBox.height - 74f,
                        9f,
                        Color(0x68758A),
                        false,
                        frc,
                    )
                    stream.setStrokingColor(Color(0xD8DFE9))
                    stream.moveTo(margin, page.mediaBox.height - 90f)
                    stream.lineTo(pageWidth - margin, page.mediaBox.height - 90f)
                    stream.stroke()
                    var baseline = page.mediaBox.height - 118f
                    pageLines.forEach { line ->
                        drawText(document, stream, awtFont, line, margin, baseline, 9f, Color(0x25354C), false, frc)
                        baseline -= 18f
                    }
                    val footer =
                        if (language == AppLanguage.Arabic) {
                            "صفحة ${index + 1} من ${pages.size}"
                        } else {
                            "Page ${index + 1} of ${pages.size}"
                        }
                    drawText(document, stream, awtFont, footer, margin, 32f, 8f, Color(0x68758A), false, frc)
                }
            }
            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }

    private fun drawText(
        document: PDDocument,
        stream: PDPageContentStream,
        font: Font,
        value: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Color,
        bold: Boolean,
        fontRenderContext: FontRenderContext,
    ) {
        if (value.isEmpty()) return
        val layout = TextLayout(value, font.deriveFont(if (bold) Font.BOLD else Font.PLAIN, size * 2f), fontRenderContext)
        val image =
            BufferedImage(
                ceil(layout.advance).toInt().coerceAtLeast(1) + 8,
                ceil(layout.ascent + layout.descent + layout.leading).toInt().coerceAtLeast(1) + 8,
                BufferedImage.TYPE_INT_ARGB,
            )
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.color = color
            layout.draw(graphics, 4f, 4f + layout.ascent)
        } finally {
            graphics.dispose()
        }
        val rendered = LosslessFactory.createFromImage(document, image)
        stream.drawImage(rendered, x, baseline - rendered.height / 2f + 2f, rendered.width / 2f, rendered.height / 2f)
    }
}
