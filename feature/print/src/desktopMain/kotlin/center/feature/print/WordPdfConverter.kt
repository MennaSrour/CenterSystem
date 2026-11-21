package center.feature.print

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

internal class WordUnavailable : IOException()

internal class WordConversionFailed : IOException()

internal class WordPdfConverter {
    fun convert(source: Path): ByteArray {
        val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val office = officeExecutable(windows)
        val word by lazy { if (windows) wordExecutable() else null }
        if (office == null && word == null) throw WordUnavailable()

        val directory = Files.createTempDirectory("center-print-word-")
        try {
            val input = Files.copy(source, directory.resolve("document.docx"))
            val output = directory.resolve("document.pdf")
            if (office != null &&
                run(
                    directory,
                    listOf(
                        office.absolutePathString(),
                        "-env:UserInstallation=${directory.resolve("profile").toUri().toASCIIString()}",
                        "--headless",
                        "--convert-to",
                        "pdf:writer_pdf_Export",
                        "--outdir",
                        directory.absolutePathString(),
                        input.absolutePathString(),
                    ),
                ) &&
                output.isRegularFile()
            ) {
                return readPdfBytes(output)
            }

            word?.let { powerShell ->
                Files.deleteIfExists(output)
                if (run(
                        directory,
                        listOf(powerShell.absolutePathString(), "-NoProfile", "-NonInteractive", "-EncodedCommand", WORD_COMMAND),
                        mapOf("CENTER_PRINT_INPUT" to input.absolutePathString(), "CENTER_PRINT_OUTPUT" to output.absolutePathString()),
                    ) &&
                    output.isRegularFile()
                ) {
                    return readPdfBytes(output)
                }
            }
            throw WordConversionFailed()
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun officeExecutable(windows: Boolean): Path? {
        val names = if (windows) listOf("soffice.com", "soffice.exe") else listOf("soffice", "libreoffice")
        val installed =
            if (windows) {
                listOfNotNull(System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"))
                    .flatMap { base -> names.map { Path.of(base, "LibreOffice", "program", it) } }
            } else {
                listOf(Path.of("/Applications/LibreOffice.app/Contents/MacOS/soffice"))
            }
        return (findOnPath(names) + installed).firstOrNull { Files.isExecutable(it) && it.isRegularFile() }
    }

    private fun wordExecutable(): Path? {
        val powerShell =
            System
                .getenv("SystemRoot")
                ?.let { Path.of(it, "System32", "WindowsPowerShell", "v1.0", "powershell.exe") }
                ?.takeIf { it.isRegularFile() }
                ?: findOnPath(listOf("powershell.exe")).firstOrNull { it.isRegularFile() }
                ?: return null
        val registered =
            run(
                powerShell.parent,
                listOf(
                    powerShell.absolutePathString(),
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "if (-not [type]::GetTypeFromProgID('Word.Application')) { exit 1 }",
                ),
            )
        return powerShell.takeIf { registered }
    }

    private fun findOnPath(names: List<String>): List<Path> =
        System
            .getenv("PATH")
            .orEmpty()
            .split(java.io.File.pathSeparatorChar)
            .filter { it.isNotBlank() }
            .flatMap { directory -> names.map { Path.of(directory.trim('"'), it) } }

    private fun run(
        directory: Path,
        args: List<String>,
        variables: Map<String, String> = emptyMap(),
    ): Boolean {
        val process =
            try {
                ProcessBuilder(args)
                    .directory(directory.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .apply { environment().putAll(variables) }
                    .start()
            } catch (_: IOException) {
                return false
            }
        return try {
            process.outputStream.close()
            process.waitFor(90, TimeUnit.SECONDS) && process.exitValue() == 0
        } finally {
            if (process.isAlive) {
                process.descendants().use { children -> children.forEach { it.destroyForcibly() } }
                process.destroyForcibly()
            }
        }
    }

    private companion object {
        val WORD_SCRIPT =
            """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}word = New-Object -ComObject Word.Application
            ${'$'}doc = ${'$'}null
            try {
                ${'$'}word.Visible = ${'$'}false
                ${'$'}word.DisplayAlerts = 0
                ${'$'}word.AutomationSecurity = 3
                ${'$'}doc = ${'$'}word.Documents.Open(${'$'}env:CENTER_PRINT_INPUT, ${'$'}false, ${'$'}true, ${'$'}false)
                ${'$'}doc.ExportAsFixedFormat(${'$'}env:CENTER_PRINT_OUTPUT, 17)
            } finally {
                try { if (${'$'}doc) { ${'$'}doc.Close(0) } }
                finally { ${'$'}word.Quit(0) }
            }
            """.trimIndent()
        val WORD_COMMAND: String = Base64.getEncoder().encodeToString(WORD_SCRIPT.toByteArray(Charsets.UTF_16LE))
    }
}
