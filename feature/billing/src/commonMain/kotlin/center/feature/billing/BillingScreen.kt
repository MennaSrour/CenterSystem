package center.feature.billing

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage

@Composable
fun BillingScreen() {
    Text(AppStrings.get(AppStringKey.BillingWorkspace, LocalAppLanguage.current))
}
