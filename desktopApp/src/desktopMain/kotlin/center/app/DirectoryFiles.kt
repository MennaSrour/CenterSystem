package center.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.database.DatabaseBackup
import center.database.SqlStudentRepository
import center.domain.repository.UserRole
import center.domain.usecase.AuthSession
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneConfirmationContent
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.CenterOneSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.ComponentOrientation
import java.awt.KeyboardFocusManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
internal fun StudentImportActions(
    services: DirectoryServices,
    repository: SqlStudentRepository,
) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<LocalizedText?>(null) }
    val importLabel = authText("استيراد طلاب (CSV)", "Import CSV")
    val templateLabel = authText("تحميل نموذج جاهز", "Download Template")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Section, vertical = CenterOneSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
    ) {
        CenterOnePrimaryButton(
            onClick = {
                val file = chooseFile(language, importLabel, "csv")
                if (file != null) {
                    busy = true
                    scope.launch {
                        try {
                            message =
                                directoryResult { importStudentCsv(file, services, repository) }.fold(
                                    onSuccess = { summary ->
                                        LocalizedText(
                                            "تم استيراد ${summary.imported} طالب، وتخطي ${summary.duplicates} مكرر.",
                                            "Imported ${summary.imported} students and skipped ${summary.duplicates} duplicates.",
                                        )
                                    },
                                    onFailure = { failure ->
                                        if (failure is InvalidStudentCsv) {
                                            LocalizedText(
                                                "راجع السجل رقم ${failure.record}. استخدم النموذج وصفًا دراسيًا صالحًا واسم مجموعة نشطة عند تحديدها.",
                                                "Check CSV record ${failure.record}. Use the template, a valid grade, and an active group name if specified.",
                                            )
                                        } else {
                                            LocalizedText(
                                                "تعذر الاستيراد. تحقق من ترميز UTF-8 ومن عدم استخدام الأكواد مسبقًا.",
                                                "Import failed. Check the UTF-8 encoding and whether any supplied codes are already in use.",
                                            )
                                        }
                                    },
                                )
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            enabled = !busy,
        ) { Text(importLabel) }
        CenterOneOutlinedButton(
            onClick = {
                val folder = chooseFile(language, templateLabel)
                if (folder != null) {
                    busy = true
                    scope.launch {
                        try {
                            val result =
                                directoryResult {
                                    withContext(Dispatchers.IO) {
                                        Files.writeString(
                                            folder.resolve("center_one_students_template.csv"),
                                            studentCsvTemplate,
                                            java.nio.file.StandardOpenOption.CREATE_NEW,
                                        )
                                    }
                                }
                            message =
                                if (result.isSuccess) {
                                    LocalizedText(
                                        "تم حفظ center_one_students_template.csv",
                                        "Saved center_one_students_template.csv",
                                    )
                                } else {
                                    LocalizedText(
                                        "تعذر حفظ النموذج. اختر مجلدًا قابلاً للكتابة لا يحتوي على النموذج.",
                                        "Could not save the template. Choose a writable folder without an existing template.",
                                    )
                                }
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            enabled = !busy,
        ) { Text(templateLabel) }
    }
    message?.let {
        FileOperationDialog(authText("استيراد الطلاب", "Student Import"), it.resolve(language)) { message = null }
    }
}

@Composable
internal fun DatabaseBackupCard(
    backup: DatabaseBackup,
    session: AuthSession,
    onCloseApplication: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val title = authText("النسخ الاحتياطي", "Database Backup")
    val exportLabel = authText("حفظ نسخة احتياطية", "Export Backup")
    val restoreLabel = authText("استرجاع نسخة", "Restore Backup")
    var busy by remember { mutableStateOf(false) }
    var selectedBackup by remember { mutableStateOf<Path?>(null) }
    var message by remember { mutableStateOf<LocalizedText?>(null) }
    var restoreFailed by remember { mutableStateOf(false) }

    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(CenterOneSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Small),
            ) {
                CenterOnePrimaryButton(
                    onClick = {
                        val folder = chooseFile(language, exportLabel)
                        if (folder != null && session.user.value?.role == UserRole.ADMIN) {
                            busy = true
                            scope.launch {
                                try {
                                    message =
                                        directoryResult {
                                            withContext(Dispatchers.IO) {
                                                backup.export(folder, LocalDate.now(ZoneId.of(Cairo.id)))
                                            }
                                        }.fold(
                                            onSuccess = { LocalizedText("تم حفظ ${it.fileName}", "Saved ${it.fileName}") },
                                            onFailure = { LocalizedText("تعذر حفظ النسخة الاحتياطية.", "Could not export the backup.") },
                                        )
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                    enabled = !busy,
                ) { Text(exportLabel) }
                CenterOneOutlinedButton(
                    onClick = {
                        restoreFailed = false
                        selectedBackup = chooseFile(language, restoreLabel, "db")
                    },
                    enabled = !busy,
                ) { Text(restoreLabel) }
            }
        }
    }
    selectedBackup?.let { source ->
        val dismiss = { if (!busy) selectedBackup = null }
        CenterOneModal(dismiss) {
            CenterOneConfirmationContent(
                title = restoreLabel,
                message =
                    authText(
                        "سيغلق التطبيق الآن، وستحل نسخة ${source.fileName} محل جميع البيانات والحسابات عند فتحه مجددًا.",
                        "The app will close. When reopened, ${source.fileName} will replace all current data and accounts.",
                    ),
                confirmLabel = authText("استرجاع وإغلاق", "Restore and close"),
                cancelLabel = authText("إلغاء", "Cancel"),
                onConfirm = {
                    if (!busy && session.user.value?.role == UserRole.ADMIN) {
                        busy = true
                        restoreFailed = false
                        scope.launch {
                            try {
                                val result = directoryResult { withContext(Dispatchers.IO) { backup.stageRestore(source) } }
                                if (result.isSuccess) onCloseApplication() else restoreFailed = true
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                onCancel = dismiss,
                icon = {},
                enabled = !busy,
                error =
                    if (restoreFailed) {
                        authText(
                            "تعذر تجهيز الملف. اختر نسخة صالحة ومتوافقة.",
                            "Could not prepare the file. Choose a valid, compatible backup.",
                        )
                    } else {
                        null
                    },
            )
        }
    }
    message?.let { FileOperationDialog(title, it.resolve(language)) { message = null } }
}

@Composable
private fun FileOperationDialog(
    title: String,
    message: String,
    onClose: () -> Unit,
) {
    CenterOneModal(onClose) {
        CenterOneModalContent(title, "", authText("إغلاق", "Close"), onClose, maxWidth = 480.dp) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun chooseFile(
    language: AppLanguage,
    title: String,
    extension: String? = null,
): Path? {
    val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    if (extension == null) {
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    } else {
        chooser.fileFilter = FileNameExtensionFilter("*.$extension", extension)
        chooser.isAcceptAllFileFilterUsed = false
    }
    chooser.applyComponentOrientation(
        if (language == AppLanguage.Arabic) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT,
    )
    val select = LocalizedText("اختيار", "Select").resolve(language)
    if (chooser.showDialog(parent, select) != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFile.toPath()
}
