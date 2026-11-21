package center.feature.print

import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.print.PrinterJob
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.DialogTypeSelection
import javax.print.attribute.standard.JobName
import javax.print.attribute.standard.PageRanges
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

internal class FileTooLarge : IOException()

internal class UnsupportedDocument : IOException()

internal class NoPrinter : IOException()

internal const val MAX_DOCUMENT_BYTES = 128 * 1024 * 1024

internal fun readPdfBytes(path: Path): ByteArray {
    if (Files.size(path) > MAX_DOCUMENT_BYTES) throw FileTooLarge()
    return Files.newInputStream(path).use { input ->
        input.readNBytes(MAX_DOCUMENT_BYTES + 1).also {
            if (it.size > MAX_DOCUMENT_BYTES) throw FileTooLarge()
        }
    }
}

internal fun isSupported(path: Path): Boolean =
    path.extension.equals("pdf", ignoreCase = true) || path.extension.equals("docx", ignoreCase = true)

internal class DesktopPrintFiles {
    suspend fun chooseSource(title: String): Path? =
        onAwt {
            val dialog = FileDialog(activeFrame(), title, FileDialog.LOAD)
            try {
                dialog.setFilenameFilter { _, file ->
                    file.endsWith(".pdf", ignoreCase = true) || file.endsWith(".docx", ignoreCase = true)
                }
                dialog.isVisible = true
                dialog.files.firstOrNull()?.toPath()
            } finally {
                dialog.dispose()
            }
        }

    suspend fun chooseOutput(
        title: String,
        name: String,
        overwrite: String,
    ): Path? =
        onAwt {
            val dialog = FileDialog(activeFrame(), title, FileDialog.SAVE)
            try {
                dialog.file = "${Path.of(name).nameWithoutExtension}-center-one.pdf"
                dialog.isVisible = true
                val selected = dialog.files.firstOrNull()?.toPath() ?: return@onAwt null
                val output =
                    if (selected.extension.equals("pdf", ignoreCase = true)) {
                        selected
                    } else {
                        selected.resolveSibling("${selected.fileName}.pdf")
                    }
                // A platform may only confirm the exact name typed before the PDF extension is appended.
                if (Files.exists(output) &&
                    (output != selected || System.getProperty("os.name").startsWith("Linux")) &&
                    JOptionPane.showConfirmDialog(activeFrame(), overwrite, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION
                ) {
                    null
                } else {
                    output
                }
            } finally {
                dialog.dispose()
            }
        }

    suspend fun print(
        pdf: Path,
        name: String,
    ): Boolean {
        if (PrinterJob.lookupPrintServices().isEmpty()) throw NoPrinter()
        return Loader.loadPDF(pdf.toFile()).use { document ->
            check(document.currentAccessPermission.canPrint())
            val job = PrinterJob.getPrinterJob()
            job.jobName = name
            job.setPageable(PDFPageable(document))
            val attributes =
                HashPrintRequestAttributeSet().apply {
                    add(DialogTypeSelection.NATIVE)
                    add(JobName(name, null))
                    add(PageRanges(1, document.numberOfPages))
                }
            val accepted = onAwt { job.printDialog(attributes) }
            if (accepted) job.print(attributes)
            accepted
        }
    }
}

private fun activeFrame(): Frame? = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame

private suspend fun <T> onAwt(action: () -> T): T =
    suspendCancellableCoroutine { continuation ->
        SwingUtilities.invokeLater {
            if (continuation.isActive) continuation.resumeWith(runCatching(action))
        }
    }
