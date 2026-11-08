package center.utils

fun String.normalizedName(): String = trim().replace(Regex("\\s+"), " ")
