package center.domain.usecase

/** Produces readable person codes; database primary keys and unique indexes enforce uniqueness. */
class SequentialPersonCodeGenerator {
    fun next(prefix: String, existingCodes: Collection<String>): String {
        require(prefix in setOf("ST", "TR", "SF")) { "Unsupported person code prefix" }
        val next =
            (existingCodes.mapNotNull { code ->
                code.takeIf { it.startsWith("$prefix-") }
                    ?.substringAfterLast('-')
                    ?.toIntOrNull()
            }.maxOrNull() ?: 1000) + 1
        return "$prefix-$next"
    }
}
