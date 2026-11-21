package center.feature.print

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfWatermarkServiceTest {
    private val service = PdfWatermarkService()

    @Test
    fun writesWatermarksOnAllPagesAndPreservesOriginalPageText() {
        val source = samplePdf()
        val output = ByteArrayOutputStream()
        service.write(source, Watermark(), null, output)
        val written = output.toByteArray()

        assertTrue(written.isNotEmpty())
        assertEquals(2, service.pageCount(written))
        Loader.loadPDF(written).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
            val original = Loader.loadPDF(source).use { PDFTextStripper().getText(it) }
            assertEquals(original, text)
            assertContains(text, "Exam page 1")
            assertContains(text.filterNot { it.isWhitespace() }, "Exampage2")
            pdf.pages.forEach { page ->
                assertTrue(
                    page.resources.xObjectNames
                        .iterator()
                        .hasNext(),
                )
                assertTrue(
                    page.resources.extGStateNames.any {
                        page.resources.getExtGState(it).nonStrokingAlphaConstant == 0.15f
                    },
                )
            }
            assertEquals(90, pdf.getPage(1).rotation)
        }
    }

    @Test
    fun rendersArabicLogoTilesOnRotatedCroppedPageWithSubtleOpacity() {
        val source = samplePdf()
        val config = Watermark("سنتر وان", WatermarkMode.LogoAndText, WatermarkLayout.Tiled, 5)
        val logo =
            BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().also { graphics ->
                    graphics.color = Color.BLUE
                    graphics.fillOval(0, 0, width, height)
                    graphics.dispose()
                }
            }

        val bitmap = service.preview(source, config, logo, 1)
        assertTrue(bitmap.width > bitmap.height)
        val sample =
            (bitmap.height / 4 until bitmap.height * 3 / 4).asSequence().flatMap { y ->
                (bitmap.width / 8 until bitmap.width * 7 / 8).asSequence().map { x -> Color(bitmap.getRGB(x, y)) }
            }
        assertTrue(sample.any { it.blue == 255 && it.red in 235..250 && it.green == it.red })
    }

    private fun samplePdf(): ByteArray =
        PDDocument().use { document ->
            repeat(2) { index ->
                val page = PDPage(PDRectangle.A4)
                if (index == 1) {
                    page.cropBox = PDRectangle(30f, 40f, 480f, 730f)
                    page.rotation = 90
                }
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                    stream.newLineAtOffset(65f, 710f)
                    stream.showText("Exam page ${index + 1}")
                    stream.endText()
                }
            }
            ByteArrayOutputStream().also(document::save).toByteArray()
        }
}
