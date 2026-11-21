package center.feature.print

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneChoiceGroup
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneMainNavigation
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import kotlin.math.roundToInt

internal data class PrintState(
    val watermark: Watermark = Watermark(),
    val fileName: String? = null,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val preview: ImageBitmap? = null,
    val working: Boolean = false,
    val rendering: Boolean = false,
)

internal sealed interface PrintEvent {
    data object Browse : PrintEvent

    data object Save : PrintEvent

    data object Print : PrintEvent

    data class ChangeWatermark(
        val watermark: Watermark,
    ) : PrintEvent

    data class GoToPage(
        val index: Int,
    ) : PrintEvent
}

@Composable
internal fun PrintScreen(
    state: PrintState,
    onEvent: (PrintEvent) -> Unit,
    onNavigate: (MainNavigation) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    dropModifier: Modifier,
    overlay: @Composable () -> Unit,
) {
    val language = LocalAppLanguage.current
    CenterOneCanvas {
        Row(Modifier.fillMaxSize()) {
            if (language == AppLanguage.English) Sidebar(onNavigate)
            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    CenterOnePageHeader("المطبعة والمذكرات", "Print Center", "") {
                        CenterOneLanguageToggle(language, onLanguageSelected)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                        val wide = maxWidth >= 880.dp
                        val body = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                        if (wide) {
                            Row(body, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                PrintControls(state, onEvent, dropModifier, Modifier.weight(1f))
                                Preview(state, onEvent, Modifier.weight(1.15f))
                            }
                        } else {
                            Column(body, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                PrintControls(state, onEvent, dropModifier)
                                Preview(state, onEvent)
                            }
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter)) { overlay() }
            }
            if (language == AppLanguage.Arabic) Sidebar(onNavigate)
        }
    }
}

@Composable
private fun Sidebar(onNavigate: (MainNavigation) -> Unit) {
    CenterOneMainNavigation(MainNavigation.Print, onNavigate, Modifier.width(216.dp))
}

@Composable
private fun PrintControls(
    state: PrintState,
    onEvent: (PrintEvent) -> Unit,
    dropModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val settings = state.watermark
    val enabled = !state.working
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CenterOneCard(Modifier.fillMaxWidth().then(dropModifier)) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Print, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(state.fileName ?: copy("اسحب ملف PDF أو Word (.docx) هنا", "Drop a PDF or Word (.docx) file here"))
                CenterOneOutlinedButton(onClick = { onEvent(PrintEvent.Browse) }, enabled = enabled) {
                    Text(copy("اختيار ملف", "Choose file"))
                }
                if (state.working) CircularProgressIndicator()
            }
        }
        CenterOneCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(copy("العلامة المائية", "Watermark"), style = MaterialTheme.typography.titleMedium)
                CenterOneTextField(
                    settings.text,
                    { onEvent(PrintEvent.ChangeWatermark(settings.copy(text = it.take(Watermark.MAX_TEXT_LENGTH)))) },
                    Modifier.fillMaxWidth(),
                    enabled = enabled,
                    label = { Text(copy("نص العلامة المائية", "Watermark text")) },
                )
                Text(copy("النمط", "Mode"), style = MaterialTheme.typography.labelLarge)
                CenterOneChoiceGroup(
                    listOf(copy("نص", "Text"), copy("شعار ونص", "Logo + text")),
                    settings.mode.ordinal,
                    { onEvent(PrintEvent.ChangeWatermark(settings.copy(mode = WatermarkMode.entries[it]))) },
                    enabled = enabled,
                )
                Text(copy("التوزيع", "Layout"), style = MaterialTheme.typography.labelLarge)
                CenterOneChoiceGroup(
                    listOf(copy("في المنتصف", "Centered"), copy("متكرر", "Tiled")),
                    settings.layout.ordinal,
                    { onEvent(PrintEvent.ChangeWatermark(settings.copy(layout = WatermarkLayout.entries[it]))) },
                    enabled = enabled,
                )
                Text(copy("الشفافية", "Opacity") + " · ${settings.opacityPercent}%", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = settings.opacityPercent.toFloat(),
                    onValueChange = { onEvent(PrintEvent.ChangeWatermark(settings.copy(opacityPercent = it.roundToInt()))) },
                    valueRange = 5f..35f,
                    enabled = enabled,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CenterOnePrimaryButton(onClick = { onEvent(PrintEvent.Save) }, enabled = enabled && state.pageCount > 0) {
                        Text(copy("حفظ ملف PDF", "Save watermarked PDF"))
                    }
                    CenterOneOutlinedButton(onClick = { onEvent(PrintEvent.Print) }, enabled = enabled && state.pageCount > 0) {
                        Text(copy("طباعة الآن", "Print now"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Preview(
    state: PrintState,
    onEvent: (PrintEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(copy("معاينة الطباعة", "Print preview"), style = MaterialTheme.typography.titleMedium)
            if (state.pageCount > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenterOneTextButton(
                        onClick = { onEvent(PrintEvent.GoToPage(state.pageIndex - 1)) },
                        enabled = state.pageIndex > 0 && !state.working,
                    ) { Text(copy("السابق", "Previous")) }
                    Text(copy("صفحة ${state.pageIndex + 1} من ${state.pageCount}", "Page ${state.pageIndex + 1} of ${state.pageCount}"))
                    CenterOneTextButton(
                        onClick = { onEvent(PrintEvent.GoToPage(state.pageIndex + 1)) },
                        enabled = state.pageIndex + 1 < state.pageCount && !state.working,
                    ) { Text(copy("التالي", "Next")) }
                }
            }
            Box(Modifier.fillMaxWidth().height(505.dp), contentAlignment = Alignment.Center) {
                val image = state.preview
                when {
                    state.rendering -> CircularProgressIndicator()
                    image != null ->
                        Image(
                            image,
                            copy("معاينة الصفحة", "Page preview"),
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    state.pageCount == 0 -> Text(copy("اختر ملفًا لمعاينة العلامة المائية.", "Choose a file to preview the watermark."))
                    else -> Text(copy("المعاينة غير متاحة.", "Preview unavailable."))
                }
            }
        }
    }
}

@Composable
private fun copy(
    arabic: String,
    english: String,
) = LocalizedText(arabic, english).resolve(LocalAppLanguage.current)
