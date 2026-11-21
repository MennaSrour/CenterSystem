package center.feature.print

internal enum class WatermarkMode { Text, LogoAndText }

internal enum class WatermarkLayout { Centered, Tiled }

internal data class Watermark(
    val text: String = "CENTER ONE",
    val mode: WatermarkMode = WatermarkMode.Text,
    val layout: WatermarkLayout = WatermarkLayout.Centered,
    val opacityPercent: Int = 15,
) {
    init {
        require(opacityPercent in 5..35)
        require(text.length <= MAX_TEXT_LENGTH)
    }

    val effectiveText: String get() = text.trim().ifEmpty { "CENTER ONE" }

    companion object {
        const val MAX_TEXT_LENGTH = 80
    }
}
