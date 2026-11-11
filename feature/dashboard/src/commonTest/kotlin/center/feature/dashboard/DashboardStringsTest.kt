package center.feature.dashboard

import center.ui.localization.AppLanguage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardStringsTest {
    @Test
    fun everyDashboardKeyHasBothTranslationsWithoutDualLanguageLabels() {
        DashboardText.entries.forEach { key ->
            val arabic = key.text(AppLanguage.Arabic, "نور")
            val english = key.text(AppLanguage.English, "Nour")
            assertTrue(arabic.isNotBlank(), key.name)
            assertTrue(english.isNotBlank(), key.name)
            if (key != DashboardText.LANGUAGE_AR) assertFalse(Regex("[\\u0600-\\u06ff]").containsMatchIn(english), key.name)
            if (key != DashboardText.LANGUAGE_EN) assertFalse(Regex("[A-Za-z]").containsMatchIn(arabic), key.name)
            assertFalse(" / " in arabic, key.name)
            assertFalse(" / " in english, key.name)
        }
    }
}
