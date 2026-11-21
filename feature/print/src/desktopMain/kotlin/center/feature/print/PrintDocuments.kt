package center.feature.print

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import kotlin.io.path.extension

internal class PrintDocument(
    val name: String,
    val bytes: ByteArray,
    val pageCount: Int,
)

internal class PrintDocuments(
    private val desktop: DesktopPrintFiles,
    private val loadLogo: suspend () -> ByteArray,
) {
    private val pdf = PdfWatermarkService()
    private val converter = WordPdfConverter()
    private val logoMutex = Mutex()
    private var logo: BufferedImage? = null

    suspend fun open(file: Path): PrintDocument =
        runInterruptible(Dispatchers.IO) {
            if (!isSupported(file) || !Files.isRegularFile(file)) throw UnsupportedDocument()
            val bytes = if (file.extension.equals("docx", ignoreCase = true)) converter.convert(file) else readPdfBytes(file)
            PrintDocument(file.fileName.toString(), bytes, pdf.pageCount(bytes))
        }

    suspend fun preview(
        document: PrintDocument,
        watermark: Watermark,
        pageIndex: Int,
    ): BufferedImage =
        withContext(Dispatchers.IO) {
            pdf.preview(document.bytes, watermark, logoFor(watermark), pageIndex)
        }

    suspend fun print(
        document: PrintDocument,
        watermark: Watermark,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val jobFile = Files.createTempFile("center-print-job-", ".pdf")
            try {
                write(document, watermark, jobFile)
                desktop.print(jobFile, document.name)
            } finally {
                Files.deleteIfExists(jobFile)
            }
        }

    private suspend fun logoFor(watermark: Watermark): BufferedImage? {
        if (watermark.mode != WatermarkMode.LogoAndText) return null
        return logoMutex.withLock {
            logo ?: Image.makeFromEncoded(loadLogo()).use { encoded ->
                val png = requireNotNull(encoded.encodeToData(EncodedImageFormat.PNG))
                png.use {
                    val original = requireNotNull(ImageIO.read(ByteArrayInputStream(it.bytes)))
                    val scale = minOf(1.0, 512.0 / maxOf(original.width, original.height))
                    val resized =
                        BufferedImage(
                            (original.width * scale).toInt().coerceAtLeast(1),
                            (original.height * scale).toInt().coerceAtLeast(1),
                            BufferedImage.TYPE_INT_ARGB,
                        )
                    val graphics = resized.createGraphics()
                    try {
                        graphics.drawImage(original, 0, 0, resized.width, resized.height, null)
                    } finally {
                        graphics.dispose()
                    }
                    resized.also { logo = it }
                }
            }
        }
    }

    suspend fun save(
        source: PrintDocument,
        watermark: Watermark,
        target: Path,
    ) = withContext(Dispatchers.IO) {
        val absolute = target.toAbsolutePath()
        val temp = Files.createTempFile(absolute.parent, ".center-print-", ".pdf")
        try {
            write(source, watermark, temp)
            currentCoroutineContext().ensureActive()
            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private suspend fun write(
        source: PrintDocument,
        watermark: Watermark,
        output: Path,
    ) {
        val image = logoFor(watermark)
        val context = currentCoroutineContext()
        Files.newOutputStream(output).use { pdf.write(source.bytes, watermark, image, it) { context.ensureActive() } }
        context.ensureActive()
    }
}
