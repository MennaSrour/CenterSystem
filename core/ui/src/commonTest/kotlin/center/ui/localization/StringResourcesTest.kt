package center.ui.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun appDictionaryHasCompleteStrictlySeparatedArabicAndEnglishEntries() {
        assertEquals(AppStringKey.entries.toSet(), AppStrings.entries.keys)
        assertEquals(
            mapOf("students" to 65, "courses" to 94, "financials" to 77),
            AppStrings.featureEntries.keys
                .groupingBy {
                    it.substringBefore(':')
                }.eachCount(),
        )
        val allowedTokens = setOf("center", "one", "pdf", "csv", "pin", "id", "firebase", "egp", "jpg", "png")
        val arabicCharacter = Regex("[\\u0600-\\u06FF]")
        val englishWord = Regex("[A-Za-z]+")
        val placeholder = Regex("%[1-9][0-9]*" + '\\' + '$' + "[sd]")

        (AppStrings.entries.mapKeys { it.key.name } + AppStrings.featureEntries).forEach { (key, value) ->
            assertTrue(value.arabic.isNotBlank(), "$key Arabic value is blank")
            assertTrue(value.english.isNotBlank(), "$key English value is blank")
            assertFalse(arabicCharacter.containsMatchIn(value.english), "$key English value contains Arabic")
            val unexpectedEnglish =
                englishWord
                    .findAll(value.arabic.replace(placeholder, ""))
                    .map { it.value.lowercase() }
                    .filterNot { it in allowedTokens }
                    .toList()
            assertTrue(unexpectedEnglish.isEmpty(), "$key Arabic value contains English text: $unexpectedEnglish")
        }
    }
}
