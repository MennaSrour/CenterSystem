package center.database

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

internal const val DATABASE_FILE = "center.db"

class DatabaseBackup(
    directory: Path,
) {
    private val live = directory.resolve(DATABASE_FILE)
    private val pending = directory.resolve("center.restore-pending")
    private val previous = directory.resolve("center.before-restore.db")

    /** Captures one consistent SQLite snapshot per local calendar day and retains seven days. */
    fun createDailyBackup(date: LocalDate): Path {
        val folder = live.parent.resolve("backups")
        Files.createDirectories(folder)
        val target = folder.resolve("centerone_backup_$date.db")
        if (Files.notExists(target) && Files.isRegularFile(live)) {
            withSnapshot(live, folder) { Files.move(it, target, ATOMIC_MOVE, REPLACE_EXISTING) }
        }
        pruneDailyBackups(folder, date)
        return target
    }

    private fun pruneDailyBackups(
        folder: Path,
        today: LocalDate,
    ) {
        val oldest = today.minusDays(6)
        Files.list(folder).use { files ->
            files.forEach { path ->
                val match = Regex("centerone_backup_(\\d{4}-\\d{2}-\\d{2})\\.db").matchEntire(path.fileName.toString())
                val date = match?.groupValues?.get(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (date != null && date < oldest) Files.deleteIfExists(path)
            }
        }
    }

    fun export(
        folder: Path,
        date: LocalDate,
    ): Path {
        val basename = "center_one_backup_$date"
        val destination =
            generateSequence(1) { it + 1 }
                .map { number -> folder.resolve(if (number == 1) "$basename.db" else "$basename-$number.db") }
                .first { Files.notExists(it) }
        withSnapshot(live, folder) { Files.move(it, destination) }
        return destination
    }

    fun stageRestore(source: Path) {
        require(source.fileName.toString().endsWith(".db", ignoreCase = true))
        require(Files.isRegularFile(source) && !Files.isSameFile(source, live))
        check(Files.notExists(pending)) { "A restore is already pending" }
        withSnapshot(source, live.parent) { snapshot ->
            connect(snapshot).use { candidate ->
                check(candidate.scalar("PRAGMA integrity_check") == "ok")
                candidate.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA foreign_key_check").use { check(!it.next()) }
                }
                connect(live).use { current ->
                    check(candidate.scalar("PRAGMA user_version") == current.scalar("PRAGMA user_version"))
                    check(candidate.schemaColumns() == current.schemaColumns())
                }
            }
            Files.move(snapshot, pending, ATOMIC_MOVE)
        }
    }

    // Called before opening SQLDelight, with the application database lock held.
    fun applyPendingRestore() {
        if (Files.notExists(pending)) return
        if (Files.exists(live)) {
            connect(live, readOnly = false).use { connection ->
                // Recover any journal and detach WAL sidecars before replacing the main file.
                check(connection.scalar("PRAGMA journal_mode=DELETE") == "delete")
            }
            check(listOf("-wal", "-shm", "-journal").none { Files.exists(live.resolveSibling("$DATABASE_FILE$it")) })
            withSnapshot(live, live.parent) { Files.move(it, previous, ATOMIC_MOVE, REPLACE_EXISTING) }
        }
        Files.move(pending, live, ATOMIC_MOVE, REPLACE_EXISTING)
    }
}

fun lockDatabaseDirectory(directory: Path): AutoCloseable {
    Files.createDirectories(directory)
    val channel = FileChannel.open(directory.resolve("center.lock"), CREATE, WRITE)
    try {
        checkNotNull(channel.tryLock()) { "CenterOne database is already open" }
        return channel
    } catch (failure: Exception) {
        channel.close()
        throw failure
    }
}

private fun connect(
    file: Path,
    readOnly: Boolean = true,
): Connection {
    val mode = if (readOnly) "ro" else "rw"
    return DriverManager.getConnection("jdbc:sqlite:${file.toUri()}?mode=$mode")
}

private fun withSnapshot(
    source: Path,
    folder: Path,
    useSnapshot: (Path) -> Unit,
) {
    val staging = Files.createTempDirectory(folder, ".center-backup-")
    val snapshot = staging.resolve("snapshot.db")
    try {
        connect(source).use { connection ->
            connection.createStatement().use { it.execute("PRAGMA trusted_schema=OFF") }
            connection.prepareStatement("VACUUM INTO ?").use {
                it.setString(1, snapshot.toString())
                it.execute()
            }
        }
        FileChannel.open(snapshot, WRITE).use { it.force(true) }
        useSnapshot(snapshot)
    } finally {
        Files.deleteIfExists(snapshot)
        Files.deleteIfExists(staging)
    }
}

private fun Connection.scalar(sql: String): String =
    createStatement().use { statement ->
        statement.executeQuery(sql).use { rows ->
            check(rows.next())
            rows.getString(1)
        }
    }

private data class SchemaColumn(
    val table: String,
    val name: String,
    val type: String,
    val primaryKeyPosition: Int,
)

private fun Connection.schemaColumns(): List<SchemaColumn> =
    createStatement().use { statement ->
        statement
            .executeQuery(
                """
                SELECT s.name, c.name, c.type, c.pk
                FROM sqlite_schema s JOIN pragma_table_xinfo(s.name) c
                WHERE s.type = 'table' AND s.name NOT LIKE 'sqlite_%'
                ORDER BY s.name, c.name
                """.trimIndent(),
            ).use { rows ->
                buildList {
                    while (rows.next()) add(SchemaColumn(rows.getString(1), rows.getString(2), rows.getString(3), rows.getInt(4)))
                }
            }
    }
