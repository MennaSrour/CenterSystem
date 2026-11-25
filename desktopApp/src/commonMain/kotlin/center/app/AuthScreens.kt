package center.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import center.domain.repository.AppUser
import center.domain.repository.UserRole
import center.domain.usecase.AuthSession
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePillTabs
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.components.LocalCenterOneLogo
import center.ui.localization.AppLanguage
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.launch

internal val brandTitle = LocalizedText("سنتر وان", "Center One")

internal fun UserRole.localizedRole() =
    when (this) {
        UserRole.ADMIN -> AppStrings.text(AppStringKey.Admin)
        UserRole.RECEPTION -> AppStrings.text(AppStringKey.Reception)
    }

@Composable
internal fun authText(key: AppStringKey) = AppStrings.get(key, LocalAppLanguage.current)

@Composable
internal fun authText(
    arabic: String,
    english: String,
) = LocalizedText(arabic, english).resolve(LocalAppLanguage.current)

@Composable
internal fun BrandLogo(modifier: Modifier) {
    LocalCenterOneLogo.current?.let { logo ->
        Image(logo, contentDescription = null, modifier = modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun LoginScreen(
    session: AuthSession,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<LocalizedText?>(null) }
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val canSubmit = username.isNotBlank() && password.isNotEmpty() && !busy

    fun submit() {
        if (!canSubmit || busy) return
        busy = true
        scope.launch {
            try {
                val result = directoryResult { session.signIn(username, password) }
                error =
                    when {
                        result.getOrNull() == true -> null
                        result.isSuccess -> LocalizedText("اسم المستخدم أو كلمة المرور غير صحيحة.", "Incorrect username or password.")
                        else -> LocalizedText("تعذر تسجيل الدخول. حاول مرة أخرى.", "Could not sign in. Try again.")
                    }
            } finally {
                busy = false
            }
        }
    }

    CenterOneCanvas {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            CenterOneCard(Modifier.widthIn(max = 410.dp).fillMaxWidth()) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BrandLogo(Modifier.size(90.dp))
                    Text(brandTitle.resolve(language), style = MaterialTheme.typography.headlineMedium)
                    CenterOneLanguageToggle(language, onLanguageSelected)
                    CenterOneTextField(
                        username,
                        {
                            username = it
                            error = null
                        },
                        Modifier.fillMaxWidth(),
                        label = { Text(authText(AppStringKey.Username)) },
                        enabled = !busy,
                    )
                    PasswordField(password, {
                        password = it
                        error = null
                    }, enabled = !busy, onSubmit = ::submit)
                    error?.let { Text(it.resolve(language), color = MaterialTheme.colorScheme.error) }
                    CenterOnePrimaryButton(::submit, Modifier.fillMaxWidth(), enabled = canSubmit) {
                        Text(if (busy) authText(AppStringKey.LoggingIn) else authText(AppStringKey.Login))
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    CenterOneTextField(
        value,
        onChange,
        Modifier.fillMaxWidth(),
        label = { Text(authText(AppStringKey.Password)) },
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            CenterOneTextButton(onClick = { visible = !visible }, enabled = enabled) {
                Text(if (visible) authText(AppStringKey.Hide) else authText(AppStringKey.Show))
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}

internal val LocalCloudSettings = androidx.compose.runtime.staticCompositionLocalOf<@Composable () -> Unit> { {} }

private sealed interface AccountForm {
    data object Add : AccountForm

    data class ChangePassword(
        val user: AppUser,
    ) : AccountForm
}

@Composable
internal fun SettingsScreen(
    session: AuthSession,
    canManageUsers: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val language = LocalAppLanguage.current
    CenterOnePageHeader("الإعدادات", "Settings", "") {
        CenterOneLanguageToggle(language, onLanguageSelected)
    }
    if (canManageUsers) {
        LocalCloudSettings.current()
        ManageUsers(session)
    }
}

@Composable
private fun ManageUsers(session: AuthSession) {
    val directory = remember(session) { session.users() }
    val users by directory.collectAsState(emptyList())
    var form by remember { mutableStateOf<AccountForm?>(null) }
    val language = LocalAppLanguage.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(authText(AppStringKey.ManageAccounts), style = MaterialTheme.typography.titleLarge)
        CenterOnePrimaryButton(onClick = { form = AccountForm.Add }) { Text(authText(AppStringKey.AddUser)) }
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        users.forEach { user ->
            CenterOneCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${user.username} · ${user.role.localizedRole().resolve(language)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    CenterOneTextButton(onClick = { form = AccountForm.ChangePassword(user) }) {
                        Text(authText(AppStringKey.ChangePassword))
                    }
                }
            }
        }
    }
    form?.let { AccountDialog(it, session) { form = null } }
}

@Composable
private fun AccountDialog(
    form: AccountForm,
    session: AuthSession,
    onClose: () -> Unit,
) {
    var name by remember(form) { mutableStateOf("") }
    var username by remember(form) { mutableStateOf("") }
    var password by remember(form) { mutableStateOf("") }
    var role by remember(form) { mutableStateOf(UserRole.RECEPTION) }
    var busy by remember(form) { mutableStateOf(false) }
    var failed by remember(form) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val adding = form is AccountForm.Add
    val canSave = !busy && password.length in 4..256 && (!adding || (name.isNotBlank() && username.isNotBlank()))

    fun save() {
        if (!canSave || busy) return
        busy = true
        scope.launch {
            try {
                val result =
                    directoryResult {
                        when (form) {
                            AccountForm.Add -> session.addUser(username, password, name, role)
                            is AccountForm.ChangePassword -> session.changePassword(form.user.id, password)
                        }
                    }
                if (result.isSuccess) onClose() else failed = true
            } finally {
                busy = false
            }
        }
    }
    val close = { if (!busy) onClose() }
    CenterOneModal(close) {
        CenterOneModalContent(
            title = if (adding) authText(AppStringKey.AddUser) else authText(AppStringKey.ChangePassword),
            subtitle = (form as? AccountForm.ChangePassword)?.user?.displayName.orEmpty(),
            closeLabel = authText(AppStringKey.Close),
            onClose = close,
            enabled = !busy,
        ) {
            if (adding) {
                CenterOneTextField(
                    name,
                    { name = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(authText(AppStringKey.DisplayName)) },
                    enabled = !busy,
                )
                CenterOneTextField(
                    username,
                    { username = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(authText(AppStringKey.Username)) },
                    enabled = !busy,
                )
                CenterOnePillTabs(
                    UserRole.entries.map { it.localizedRole().resolve(language) },
                    role.ordinal,
                    { role = UserRole.entries[it] },
                    Modifier.fillMaxWidth(),
                )
            }
            PasswordField(password, {
                password = it
                failed = false
            }, enabled = !busy, onSubmit = ::save)
            Text(authText(AppStringKey.PasswordLengthHint), style = MaterialTheme.typography.bodySmall)
            if (failed) {
                Text(
                    authText(AppStringKey.AccountSaveError),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            CenterOnePrimaryButton(::save, enabled = canSave) { Text(authText(AppStringKey.Save)) }
        }
    }
}
