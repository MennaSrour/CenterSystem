package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import center.ui.theme.White

@Composable
fun CenterOneSidebar(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier, color = Navy, contentColor = White) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(CenterOneSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
        ) {
            header()
            content()
            Spacer(Modifier.weight(1f))
            footer()
        }
    }
}
