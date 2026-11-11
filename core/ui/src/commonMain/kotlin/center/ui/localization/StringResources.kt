package center.ui.localization

data class LocalizedText(
    val arabic: String,
    val english: String,
) {
    fun resolve(language: AppLanguage): String =
        when (language) {
            AppLanguage.Arabic -> arabic
            AppLanguage.English -> english
        }
}

/** Portable map catalog. Missing keys fail visibly; arguments are inserted without interpreting their contents. */
class StringResources<Key>(
    private val entries: Map<Key, LocalizedText>,
) {
    fun get(
        key: Key,
        language: AppLanguage,
        vararg arguments: String,
    ): String {
        val template = entries.getValue(key).resolve(language)
        return Regex("%([1-9][0-9]*)[${'$'}]s").replace(template) { match ->
            val index = match.groupValues[1].toInt() - 1
            require(index in arguments.indices) { "Missing string argument ${index + 1} for $key" }
            arguments[index]
        }
    }
}
