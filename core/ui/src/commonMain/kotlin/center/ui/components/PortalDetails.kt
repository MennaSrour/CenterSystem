package center.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalStudentPortalDetails = staticCompositionLocalOf<@Composable (studentId: String, parentPhone: String) -> Unit> { { _, _ -> } }
val LocalTeacherPortalDetails = staticCompositionLocalOf<@Composable (teacherId: String) -> Unit> { {} }
