package center.feature.print

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.OutputStream
import kotlin.math.ceil

internal class PdfWatermarkService {
    fun pageCount(pdf: ByteArray): Int =
        Loader.loadPDF(pdf).use { document ->
            check(document.numberOfPages > 0)
            check(document.currentAccessPermission.canModify())
            document.numberOfPages
        }

    fun preview(
        pdf: ByteArray,
        settings: Watermark,
        logo: BufferedImage?,
        pageIndex: Int,
    ): BufferedImage =
        Loader.loadPDF(pdf).use { document ->
            val page = document.getPage(pageIndex)
            val stamp = Stamp(document, settings, logo)
            stamp.draw(document, page)
            val crop = page.cropBox
            val scale = minOf(1.75f, 1400f / maxOf(crop.width, crop.height))
            PDFRenderer(document).renderImage(pageIndex, scale)
        }

    fun write(
        pdf: ByteArray,
        settings: Watermark,
        logo: BufferedImage?,
        output: OutputStream,
        beforePage: () -> Unit = {},
    ) = Loader.loadPDF(pdf).use { document ->
        check(document.numberOfPages > 0 && document.currentAccessPermission.canModify())
        val stamp = Stamp(document, settings, logo)
        for (page in document.pages) {
            beforePage()
            stamp.draw(document, page)
        }
        document.save(output)
    }

    private class Stamp(
        document: PDDocument,
        private val settings: Watermark,
        logo: BufferedImage?,
    ) {
        private val text = LosslessFactory.createFromImage(document, renderText(settings.effectiveText))
        private val image =
            if (settings.mode == WatermarkMode.LogoAndText) {
                LosslessFactory.createFromImage(document, requireNotNull(logo))
            } else {
                null
            }
        private val opacity =
            PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = settings.opacityPercent / 100f
            }

        fun draw(
            document: PDDocument,
            page: PDPage,
        ) {
            val box = page.cropBox
            require(box.width.isFinite() && box.height.isFinite() && box.width > 0 && box.height > 0)
            val quarterTurn = page.rotation == 90 || page.rotation == 270
            val visualWidth = if (quarterTurn) box.height else box.width
            val visualHeight = if (quarterTurn) box.width else box.height
            val shortEdge = minOf(visualWidth, visualHeight)
            PDPageContentStream(document, page, AppendMode.APPEND, true, true).use { stream ->
                stream.saveGraphicsState()
                stream.transform(Matrix.getTranslateInstance(box.lowerLeftX, box.lowerLeftY))
                val rotation =
                    when (page.rotation) {
                        90 -> Matrix(0f, 1f, -1f, 0f, box.width, 0f)
                        180 -> Matrix(-1f, 0f, 0f, -1f, box.width, box.height)
                        270 -> Matrix(0f, -1f, 1f, 0f, 0f, box.height)
                        else -> Matrix()
                    }
                stream.transform(rotation)
                stream.setGraphicsStateParameters(opacity)
                when (settings.layout) {
                    WatermarkLayout.Centered -> drawAt(stream, visualWidth / 2, visualHeight / 2, shortEdge * 0.69f)
                    WatermarkLayout.Tiled -> {
                        for (row in 1..3) {
                            for (column in 1..2) {
                                drawAt(stream, visualWidth * (column * 2 - 1) / 4, visualHeight * row / 4, shortEdge * 0.29f)
                            }
                        }
                    }
                }
                stream.restoreGraphicsState()
            }
        }

        private fun drawAt(
            stream: PDPageContentStream,
            x: Float,
            y: Float,
            maxWidth: Float,
        ) {
            val textHeight = minOf(maxWidth * 0.24f, maxWidth * text.height / text.width)
            val textWidth = textHeight * text.width / text.height
            val logoHeight = if (image != null) maxWidth * 0.26f else 0f
            val gap = if (image != null) maxWidth * 0.04f else 0f
            val bottom = -(textHeight + logoHeight + gap) / 2
            stream.saveGraphicsState()
            stream.transform(Matrix.getRotateInstance(Math.PI / 4, x, y))
            stream.drawImage(text, -textWidth / 2, bottom, textWidth, textHeight)
            image?.let { drawLogo(stream, it, bottom + textHeight + gap, logoHeight) }
            stream.restoreGraphicsState()
        }

        private fun drawLogo(
            stream: PDPageContentStream,
            image: PDImageXObject,
            bottom: Float,
            size: Float,
        ) {
            val ratio = image.width.toFloat() / image.height
            val height = minOf(size, size / ratio)
            val width = height * ratio
            stream.drawImage(image, -width / 2, bottom + (size - height) / 2, width, height)
        }
    }
}

/** Java2D shapes bidirectional text before embedding it, independent of PDF fonts on the receiver. */
private fun renderText(text: String): BufferedImage {
    val layout = TextLayout(text, Font(Font.SANS_SERIF, Font.BOLD, 72), FontRenderContext(null, true, true))
    val bounds = layout.bounds
    val image = BufferedImage(ceil(bounds.width).toInt() + 8, ceil(bounds.height).toInt() + 8, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.color = Color(0x2B4162)
        layout.draw(graphics, 4f - bounds.x.toFloat(), 4f - bounds.y.toFloat())
    } finally {
        graphics.dispose()
    }
    return image
}
