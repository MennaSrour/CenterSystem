package center.ui.text

/** Isolate codes, numeric ranges, and times embedded in an RTL label. */
fun String.isolateLtr(): String = "\u2066$this\u2069"
