package center.app.portal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import center.app.authText
import center.app.directoryResult
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.text.isolateLtr
import center.ui.theme.GoldLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import java.time.Instant

@Composable
internal fun SyncStatusPill(manager: CloudSyncManager) {
    val status by manager.status.collectAsState()
    val settings by manager.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val label =
        when {
            status.busy -> authText("جارٍ المزامنة…", "Syncing…")
            settings.keyPath.isEmpty() -> authText("أوفلاين", "Offline")
            status.pending > 0 -> authText("في انتظار النت (${status.pending})", "Waiting to sync (${status.pending})")
            status.online -> authText("متزامن", "Synced")
            else -> authText("أوفلاين", "Offline")
        }
    CenterOneTextButton(onClick = { scope.launch { manager.sync() } }, enabled = !status.busy, color = GoldLight) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun CloudSettingsCard(manager: CloudSyncManager) {
    val settings by manager.settings.collectAsState()
    val status by manager.status.collectAsState()
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var connected by remember(settings.keyPath) { mutableStateOf<Boolean?>(null) }
    val canConfigure = !testing && !status.busy
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(authText("المزامنة السحابية (Firebase)", "Cloud Sync (Firebase)"), style = MaterialTheme.typography.titleMedium)
            Text(
                settings.keyPath.takeIf { it.isNotEmpty() }?.isolateLtr()
                    ?: authText("لم يُحدّد ملف حساب الخدمة", "No service account file selected"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val title = authText("اختر ملف حساب الخدمة", "Select service account file")
                CenterOneTextButton(onClick = {
                    selectPortalFile(title, "json")?.let { manager.configure(settings.copy(keyPath = it.toString())) }
                }, enabled = canConfigure) { Text(title) }
                if (settings.keyPath.isNotEmpty()) {
                    CenterOneTextButton(
                        onClick = { manager.configure(CloudSettings()) },
                        enabled = canConfigure,
                    ) { Text(authText("إزالة الملف", "Remove file")) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(authText("المزامنة التلقائية", "Auto-sync"))
                Switch(
                    settings.automatic,
                    { manager.configure(settings.copy(automatic = it)) },
                    enabled =
                        canConfigure && settings.keyPath.isNotEmpty(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CenterOnePrimaryButton(onClick = {
                    testing = true
                    scope.launch {
                        try {
                            connected = manager.testConnection()
                        } finally {
                            testing = false
                        }
                    }
                }, enabled = canConfigure && settings.keyPath.isNotEmpty()) { Text(authText("اختبار الاتصال", "Test connection")) }
                connected?.let {
                    Text(
                        if (it) {
                            authText(
                                "تم الاتصال",
                                "Connected",
                            )
                        } else {
                            authText("تعذر الاتصال بـ Firebase", "Could not connect to Firebase")
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TeacherPortalPin(
    store: PortalStore,
    teacherId: String,
) {
    var pin by remember(teacherId) { mutableStateOf<String?>(null) }
    LaunchedEffect(store, teacherId) {
        pin = directoryResult { withContext(Dispatchers.IO) { store.teacherPin(teacherId) } }.getOrNull()
    }
    pin?.let { PortalPin(it, authText("رمز بوابة المعلم", "Teacher portal PIN")) }
}

@Composable
internal fun StudentPortalDetails(
    store: PortalStore,
    studentId: String,
    parentPhone: String,
) {
    var pin by remember(parentPhone) { mutableStateOf<String?>(null) }
    LaunchedEffect(store, parentPhone) {
        pin = directoryResult { withContext(Dispatchers.IO) { store.parentPin(parentPhone) } }.getOrNull()
    }
    pin?.let { PortalPin(it, authText("رمز بوابة ولي الأمر", "Parent portal PIN")) }
    var open by remember(studentId) { mutableStateOf(false) }
    CenterOneTextButton(onClick = { open = !open }) { Text(authText("الامتحانات والدرجات", "Exams & Grades")) }
    if (open) StudentGradebook(store, studentId)
}

@Composable
private fun PortalPin(
    pin: String,
    label: String,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label · ${pin.isolateLtr()}", style = MaterialTheme.typography.titleSmall)
        CenterOneTextButton(onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(pin))) } }) {
            Text(authText("نسخ", "Copy"))
        }
    }
}

@Composable
private fun StudentGradebook(
    store: PortalStore,
    studentId: String,
) {
    val scope = rememberCoroutineScope()
    var book by remember(studentId) { mutableStateOf<Gradebook?>(null) }
    var revision by remember(studentId) { mutableStateOf(0) }
    var groupId by remember(studentId) { mutableStateOf("") }
    var title by remember(studentId) { mutableStateOf("") }
    var score by remember(studentId) { mutableStateOf("") }
    var maximum by remember(studentId) { mutableStateOf("") }
    var notes by remember(studentId) { mutableStateOf("") }
    var busy by remember(studentId) { mutableStateOf(false) }
    var error by remember(studentId) { mutableStateOf(false) }
    var saved by remember(studentId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(store, studentId, revision) {
        val result = directoryResult { withContext(Dispatchers.IO) { store.gradebook(studentId) } }
        book = result.getOrNull()
        if (result.isFailure) error = true
    }
    val gradebook = book
    val student = gradebook?.students?.find { it.id == studentId }
    val groups = gradebook?.groups.orEmpty().filter { it.id in student?.groupIds.orEmpty() }
    val selected = groups.firstOrNull { it.id == groupId } ?: groups.firstOrNull()
    var expanded by remember { mutableStateOf(false) }

    fun save(importFile: Path? = null) {
        val group = selected ?: return
        if (busy || gradebook == null) return
        val maxScore = asciiDigits(maximum.trim()).toDoubleOrNull() ?: return
        val currentTitle = title
        val currentNotes = notes
        val manualScore = asciiDigits(score.trim()).toDoubleOrNull()
        busy = true
        error = false
        saved = null
        scope.launch {
            try {
                val result =
                    directoryResult {
                        withContext(Dispatchers.IO) {
                            val grades =
                                if (importFile == null) {
                                    listOf(GradeInput(studentId, requireNotNull(manualScore)))
                                } else {
                                    readGradeImport(importFile, gradebook.students.filter { group.id in it.groupIds })
                                }
                            store.saveGrades(group.id, currentTitle, maxScore, currentNotes, grades)
                            grades.size
                        }
                    }
                saved = result.getOrNull()
                error = result.isFailure
                if (result.isSuccess) {
                    score = ""
                    revision++
                }
            } finally {
                busy = false
            }
        }
    }

    if (gradebook != null) {
        val history = gradebook.grades
        if (history.isEmpty()) Text(authText("لا توجد درجات مسجلة", "No grades recorded"))
        history.forEach { grade ->
            val date = Instant.ofEpochMilli(grade.number("date")).atZone(cairo).toLocalDate()
            val points = "${grade.amount("score")} / ${grade.amount("maxScore")}".isolateLtr()
            val titleAndGroup = listOf(grade.text("examTitle"), grade.text("groupName")).filter(String::isNotBlank).joinToString(" · ")
            Text("$titleAndGroup · $points · ${date.toString().isolateLtr()}")
            if (grade.text("notes").isNotBlank()) Text(grade.text("notes"), style = MaterialTheme.typography.bodySmall)
        }
        if (selected == null) {
            Text(authText("يلزم تسجيل الطالب في مجموعة نشطة لإضافة الدرجات.", "Enroll the student in an active group to enter grades."))
        } else {
            CenterOneDropdown(
                selected.name,
                authText("المجموعة", "Group"),
                groups.map { CenterOneDropdownOption(it.id, it.name) },
                expanded,
                { expanded = it },
                onOptionSelected = {
                    groupId = it
                    expanded = false
                },
                enabled = !busy,
            )
            GradeField(title, { title = it }, authText("اسم الامتحان", "Exam title"), busy)
            GradeField(maximum, { maximum = it }, authText("الدرجة النهائية", "Maximum score"), busy)
            GradeField(score, { score = it }, authText("درجة الطالب", "Student score"), busy)
            GradeField(notes, { notes = it }, authText("ملاحظات", "Notes"), busy)
            val canSave = !busy && title.isNotBlank() && (asciiDigits(maximum.trim()).toDoubleOrNull() ?: 0.0) > 0
            val importLabel = authText("استيراد درجات المجموعة (CSV / Excel)", "Import group grades (CSV / Excel)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CenterOnePrimaryButton(onClick = { save() }, enabled = canSave && score.isNotBlank()) {
                    Text(authText("حفظ الدرجة", "Save grade"))
                }
                CenterOneTextButton(
                    onClick = { selectPortalFile(importLabel, "csv", "xlsx")?.let { save(it) } },
                    enabled = canSave,
                ) { Text(importLabel) }
            }
        }
    }
    if (error) {
        Text(
            authText(
                "تعذر حفظ أو تحميل الدرجات. تحقق من البيانات والملف.",
                "Grades could not be loaded or saved. Check the scores and file.",
            ),
        )
    }
    saved?.let { Text(authText("تم حفظ الدرجات ($it)", "Grades saved ($it)")) }
}

@Composable
private fun GradeField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    busy: Boolean,
) {
    CenterOneTextField(value, onChange, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

private fun selectPortalFile(
    title: String,
    vararg extensions: String,
): Path? {
    fun accepted(name: String) = extensions.any { name.endsWith(".$it", ignoreCase = true) }
    val dialog =
        when (val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow) {
            is Dialog -> FileDialog(owner, title, FileDialog.LOAD)
            is Frame -> FileDialog(owner, title, FileDialog.LOAD)
            else -> FileDialog(null as Frame?, title, FileDialog.LOAD)
        }
    try {
        dialog.setFilenameFilter { _, name -> accepted(name) }
        dialog.isVisible = true
        return dialog.files
            .firstOrNull()
            ?.takeIf { accepted(it.name) }
            ?.toPath()
    } finally {
        dialog.dispose()
    }
}
