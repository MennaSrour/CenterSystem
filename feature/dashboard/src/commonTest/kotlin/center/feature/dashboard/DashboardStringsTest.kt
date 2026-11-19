package center.feature.dashboard

import center.ui.localization.AppLanguage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardStringsTest {
    @Test
    fun everyDashboardKeyHasBothTranslationsWithoutDualLanguageLabels() {
        DashboardText.entries.forEach { key ->
            val arabic = key.text(AppLanguage.Arabic)
            val english = key.text(AppLanguage.English)
            assertTrue(arabic.isNotBlank(), key.name)
            assertTrue(english.isNotBlank(), key.name)
            assertFalse(Regex("[\\u0600-\\u06ff]").containsMatchIn(english), key.name)
            assertFalse(Regex("[A-Za-z]").containsMatchIn(arabic), key.name)
            assertFalse(" / " in arabic, key.name)
            assertFalse(" / " in english, key.name)
        }
    }
}
