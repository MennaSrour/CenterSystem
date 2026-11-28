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

/** Single-column, paginated PDF renderer using the same bundled Almarai font and Java2D Arabic shaping. */
class ContextualPdfRenderer {
    fun renderTimetable(
        documentData: TimetablePdfDocument,
        language: AppLanguage,
    ): ByteArray {
        require(documentData.dayHeaders.size == 7)
        val font =
            ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
                Font.createFont(Font.TRUETYPE_FONT, it)
            }
        PDDocument().use { document ->
            val embeddedFont =
                ContextualPdfRenderer::class.java.getResourceAsStream("/fonts/almarai_regular.ttf")!!.use {
                    PDType0Font.load(document, it, true)
                }
            val chunks =
                documentData.times.indices
                    .chunked(14)
                    .ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { pageIndex, rowIndices ->
                val page = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height
                    val margin = 24f
                    val frc = FontRenderContext(null, true, true)
                    drawText(document, stream, font, documentData.title, margin, pageHeight - 32f, 14f, Color(0x2B4162), true, frc)
                    drawText(document, stream, font, "Center One", pageWidth - 84f, pageHeight - 32f, 9f, Color(0x2B4162), true, frc)
                    drawText(document, stream, font, documentData.subtitle, margin, pageHeight - 48f, 9f, Color(0x47566D), false, frc)
                    drawText(
                        document,
                        stream,
                        font,
                        documentData.generatedDate,
                        pageWidth - 130f,
                        pageHeight - 48f,
                        8f,
                        Color(0x68758A),
                        false,
                        frc,
                    )
                    val top = pageHeight - 66f
                    val labelWidth = 48f
                    val colWidth = (pageWidth - margin * 2 - labelWidth) / 7f
                    val headerHeight = 24f
                    val rowHeight = 26f
                    stream.setStrokingColor(Color(0x9CAAC0))
                    stream.setLineWidth(0.55f)
                    stream.addRect(margin, top - headerHeight, labelWidth, headerHeight)
                    stream.stroke()
                    documentData.dayHeaders.forEachIndexed { dayIndex, day ->
                        val x = margin + labelWidth + colWidth * dayIndex
                        stream.addRect(x, top - headerHeight, colWidth, headerHeight)
                        stream.stroke()
                        drawText(document, stream, font, day, x + 3f, top - 15f, 7f, Color(0x2B4162), true, frc)
                    }
                    rowIndices.forEachIndexed { rowIndex, timeIndex ->
                        val y = top - headerHeight - rowHeight * (rowIndex + 1)
                        stream.addRect(margin, y, labelWidth, rowHeight)
                        stream.stroke()
                        drawText(
                            document,
                            stream,
                            font,
                            documentData.times[timeIndex],
                            margin + 3f,
                            y + rowHeight / 2f,
                            6f,
                            Color(0x2B4162),
                            false,
                            frc,
                        )
                        documentData.dayHeaders.indices.forEach { dayIndex ->
                            val x = margin + labelWidth + colWidth * dayIndex
                            stream.addRect(x, y, colWidth, rowHeight)
                            stream.stroke()
                            val cell =
                                documentData.cells
                                    .getOrNull(timeIndex)
                                    ?.getOrNull(dayIndex)
                                    .orEmpty()
                            if (cell.isNotBlank()) {
                                drawText(document, stream, font, cell.take(48), x + 3f, y + rowHeight / 2f, 6f, Color(0x25354C), false, frc)
                            }
                        }
                    }
                    val footer =
                        if (language ==
                            AppLanguage.Arabic
                        ) {
                            "صفحة ${pageIndex + 1} من ${chunks.size}"
                        } else {
                            "Page ${pageIndex + 1} of ${chunks.size}"
                        }
                    drawText(document, stream, font, footer, margin, 18f, 7f, Color(0x68758A), false, frc)
                }
            }
            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
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
