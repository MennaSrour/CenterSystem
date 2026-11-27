package center.feature.reports

import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.print.PrinterJob
import java.nio.file.Files
import java.nio.file.Path
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.DialogTypeSelection
import javax.print.attribute.standard.JobName
import javax.print.attribute.standard.PageRanges
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/** Native save dialog plus direct PDFPageable printing for generated report bytes. */
class ReportPdfActions {
    suspend fun save(
        title: String,
        fileName: String,
        overwrite: String,
        bytes: ByteArray,
    ): Path? =
        onAwt {
            val dialog = FileDialog(activeFrame(), title, FileDialog.SAVE)
            try {
                dialog.file = fileName
                dialog.isVisible = true
                val selected = dialog.files.firstOrNull()?.toPath() ?: return@onAwt null
                val output =
                    if (selected.fileName.toString().endsWith(
                            ".pdf",
                            ignoreCase = true,
                        )
                    ) {
                        selected
                    } else {
                        selected.resolveSibling("${selected.fileName}.pdf")
                    }
                if (Files.exists(output) &&
                    JOptionPane.showConfirmDialog(activeFrame(), overwrite, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION
                ) {
                    null
                } else {
                    Files.write(output, bytes)
                    output
                }
            } finally {
                dialog.dispose()
            }
        }

    suspend fun print(
        title: String,
        bytes: ByteArray,
    ): Boolean {
        require(bytes.isNotEmpty())
        if (PrinterJob.lookupPrintServices().isEmpty()) throw IllegalStateException("No printer is available")
        return Loader.loadPDF(bytes).use { document ->
            check(document.currentAccessPermission.canPrint())
            val job =
                PrinterJob.getPrinterJob().apply {
                    jobName = title
                    setPageable(PDFPageable(document))
                }
            val attributes =
                HashPrintRequestAttributeSet().apply {
                    add(DialogTypeSelection.NATIVE)
                    add(JobName(title, null))
                    add(PageRanges(1, document.numberOfPages))
                }
            if (onAwt { job.printDialog(attributes) }) {
                job.print(attributes)
                true
            } else {
                false
            }
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
