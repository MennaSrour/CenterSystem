package center.ui.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringResourcesTest {
    private val strings = StringResources(mapOf("welcome" to LocalizedText("مرحباً، %1\$s", "Welcome, %1\$s")))

    @Test
    fun selectsLanguageAndInterpolatesArgumentsWithoutReinterpretingTheirContents() {
        assertEquals("مرحباً، نور", strings.get("welcome", AppLanguage.Arabic, "نور"))
        assertEquals("Welcome, Nour", strings.get("welcome", AppLanguage.English, "Nour"))
        assertEquals("Welcome, %1\$s", strings.get("welcome", AppLanguage.English, "%1\$s"))
    }

    @Test
    fun missingKeysAndArgumentsFailInsteadOfShowingAnUntranslatedFallback() {
        assertFailsWith<NoSuchElementException> { strings.get("missing", AppLanguage.English) }
        assertFailsWith<IllegalArgumentException> { strings.get("welcome", AppLanguage.Arabic) }
    }
}
