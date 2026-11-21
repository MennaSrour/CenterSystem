package center.feature.print

import androidx.compose.ui.graphics.toComposeImageBitmap
import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.nio.file.Path

internal class PrintStateHolder(
    private val scope: CoroutineScope,
    private val desktop: DesktopPrintFiles,
    private val documents: PrintDocuments,
) {
    private val mutableState = MutableStateFlow(PrintState())
    val state = mutableState.asStateFlow()
    private val feedback = Channel<LocalizedText>(capacity = Channel.BUFFERED)
    val messages = feedback.receiveAsFlow()
    private val previewMutex = Mutex()
    private var document: PrintDocument? = null
    private var previewJob: Job? = null

    fun onEvent(
        event: PrintEvent,
        language: AppLanguage,
    ) {
        if (state.value.working) return
        when (event) {
            PrintEvent.Browse ->
                operate(openFailure) {
                    val file = desktop.chooseSource(LocalizedText("اختيار ملف PDF أو Word", "Choose a PDF or Word file").resolve(language))
                    if (file != null) open(file)
                }
            PrintEvent.Save -> {
                val source = document ?: return
                val snapshot = state.value
                operate(saveFailure) {
                    val target =
                        desktop.chooseOutput(
                            LocalizedText("حفظ ملف PDF", "Save watermarked PDF").resolve(language),
                            source.name,
                            LocalizedText("الملف موجود بالفعل. هل تريد استبداله؟", "This file exists. Replace it?").resolve(language),
                        ) ?: return@operate
                    documents.save(source, snapshot.watermark, target)
                    feedback.send(LocalizedText("تم حفظ ملف PDF.", "Watermarked PDF saved."))
                }
            }
            PrintEvent.Print -> {
                val source = document ?: return
                val snapshot = state.value
                operate(printFailure) {
                    val printed = documents.print(source, snapshot.watermark)
                    if (printed) feedback.send(LocalizedText("أُرسل المستند إلى الطابعة.", "Document sent to the printer."))
                }
            }
            is PrintEvent.GoToPage -> {
                if (event.index !in 0 until state.value.pageCount || event.index == state.value.pageIndex) return
                mutableState.update { it.copy(pageIndex = event.index) }
                refreshPreview()
            }
            is PrintEvent.ChangeWatermark -> {
                if (event.watermark == state.value.watermark) return
                mutableState.update { it.copy(watermark = event.watermark) }
                refreshPreview()
            }
        }
    }

    fun drop(file: Path?) {
        if (state.value.working) return
        if (file == null) {
            feedback.trySend(LocalizedText("اختر ملفًا واحدًا فقط.", "Choose one file at a time."))
        } else {
            operate(openFailure) { open(file) }
        }
    }

    private suspend fun open(file: Path) {
        val source = documents.open(file)
        document = source
        mutableState.update {
            it.copy(fileName = source.name, pageCount = source.pageCount, pageIndex = 0, preview = null)
        }
        refreshPreview()
    }

    private fun refreshPreview() {
        val source = document ?: return
        val snapshot = state.value
        previewJob?.cancel()
        mutableState.update { it.copy(rendering = true, preview = null) }
        previewJob =
            scope.launch {
                delay(120)
                try {
                    val bitmap =
                        previewMutex.withLock {
                            val page = documents.preview(source, snapshot.watermark, snapshot.pageIndex)
                            withContext(Dispatchers.Default) { page.toComposeImageBitmap() }
                        }
                    mutableState.update { it.copy(rendering = false, preview = bitmap) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.update { it.copy(rendering = false) }
                    feedback.send(LocalizedText("تعذّر عرض الصفحة.", "Could not render this page."))
                }
            }
    }

    private fun operate(
        failure: LocalizedText,
        action: suspend () -> Unit,
    ) {
        mutableState.update { it.copy(working = true) }
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                feedback.send(messageFor(error, failure))
            } finally {
                mutableState.update { it.copy(working = false) }
            }
        }
    }

    private fun messageFor(
        error: Exception,
        fallback: LocalizedText,
    ): LocalizedText =
        when (error) {
            is WordUnavailable ->
                LocalizedText(
                    "لا يتوفر LibreOffice أو Microsoft Word. افتح المستند واختر «حفظ باسم PDF»، ثم اختر ملف PDF هنا.",
                    "LibreOffice or Microsoft Word is required. Open the document, choose Save As PDF, then select that PDF here.",
                )
            is WordConversionFailed ->
                LocalizedText(
                    "تعذّر تحويل المستند. احفظه بصيغة PDF من Word أو LibreOffice وحاول مجددًا.",
                    "Conversion failed. Save the document as PDF in Word or LibreOffice and try again.",
                )
            is NoPrinter ->
                LocalizedText(
                    "لا توجد طابعة متاحة. يمكنك حفظ الملف بصيغة PDF.",
                    "No printer is available. You can save the PDF instead.",
                )
            is InvalidPasswordException -> LocalizedText("يتطلب الملف كلمة مرور.", "This PDF requires a password.")
            is UnsupportedDocument -> LocalizedText("اختر ملف PDF أو DOCX.", "Choose a PDF or DOCX file.")
            is FileTooLarge -> LocalizedText("الحد الأقصى لحجم ملف PDF هو 128 ميغابايت.", "The PDF size limit is 128 MB.")
            else -> fallback
        }

    private companion object {
        val openFailure =
            LocalizedText(
                "تعذّر فتح الملف. تحقق من تنسيقه وأذونات تعديله.",
                "Could not open the file. Check its format and edit permissions.",
            )
        val saveFailure = LocalizedText("تعذّر حفظ ملف PDF.", "Could not save the PDF.")
        val printFailure =
            LocalizedText("تعذّرت الطباعة. تحقق من الطابعة وأذونات الملف.", "Could not print. Check the printer and file permissions.")
    }
}
