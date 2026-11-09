package center.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

fun openDatabase(directory: Path): JdbcSqliteDriver {
    Files.createDirectories(directory)
    return JdbcSqliteDriver(
        url = "jdbc:sqlite:${directory.resolve("center.db")}",
        properties = Properties().apply { setProperty("foreign_keys", "true") },
        schema = CenterDatabase.Schema,
    )
}
