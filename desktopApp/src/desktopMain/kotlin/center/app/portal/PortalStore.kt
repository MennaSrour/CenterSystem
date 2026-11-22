package center.app.portal

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

internal fun asciiDigits(value: String): String =
    value.map { if (it in '٠'..'٩' || it in '۰'..'۹') ('0'.code + (it.code and 15)).toChar() else it }.joinToString("")

internal fun normalizePortalPhone(value: String): String {
    val digits = asciiDigits(value).replace(Regex("[ ().+\\-]"), "")
    return when {
        digits.matches(Regex("(?:0020|20)1[0125][0-9]{8}")) -> "0" + digits.takeLast(10)
        digits.matches(Regex("[0-9]{7,15}")) -> digits
        else -> ""
    }
}

private val pinRandom = SecureRandom()

internal fun generatePortalPin(): String = pinRandom.nextInt(1_000_000).toString().padStart(6, '0')

internal typealias Fields = Map<String, Any?>

internal fun Fields.text(key: String): String = this[key]?.toString().orEmpty()

internal fun Fields.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0

internal fun Fields.amount(key: String): Double = (this[key] as? Number)?.toDouble() ?: 0.0

internal data class PortalStudent(
    val id: String,
    val name: String,
    val code: String,
    val groupIds: Set<String>,
)

internal data class PortalGroup(
    val id: String,
    val name: String,
)

internal data class GradeInput(
    val studentId: String,
    val score: Double,
)

internal data class Gradebook(
    val students: List<PortalStudent>,
    val groups: List<PortalGroup>,
    val grades: List<Fields>,
)

internal fun parseGradeCsv(
    content: String,
    students: List<PortalStudent>,
): List<GradeInput> {
    val records =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .get()
            .parse(content.removePrefix("\uFEFF").reader())
    return records.use { csv ->
        val headers = csv.headerNames.associateBy { it.trim().lowercase() }

        fun header(vararg options: String) = options.firstNotNullOfOrNull(headers::get)
        val codeHeader = header("code", "studentcode", "الكود", "كود")
        val nameHeader = header("name", "studentname", "الاسم", "اسم")
        val scoreHeader = requireNotNull(header("score", "الدرجة", "درجة"))
        require(codeHeader != null || nameHeader != null)
        val seen = mutableSetOf<String>()
        csv
            .map { row ->
                require(row.isConsistent)
                val code = codeHeader?.let { row[it].trim() }.orEmpty()
                val name = nameHeader?.let { row[it].trim() }.orEmpty()
                val student =
                    requireNotNull(
                        students.singleOrNull {
                            if (code.isNotBlank()) {
                                it.code == code
                            } else {
                                name.isNotBlank() &&
                                    it.name == name
                            }
                        },
                    )
                require(seen.add(student.id))
                val score = requireNotNull(asciiDigits(row[scoreHeader].trim()).toDoubleOrNull())
                require(score.isFinite() && score >= 0)
                GradeInput(student.id, score)
            }.also { require(it.isNotEmpty()) }
    }
}

internal fun readGradeImport(
    file: Path,
    students: List<PortalStudent>,
): List<GradeInput> {
    require(Files.size(file) <= 2_000_000)
    if (!file.fileName.toString().endsWith(".xlsx", ignoreCase = true)) {
        return parseGradeCsv(Files.readString(file), students)
    }
    val csv = StringBuilder()
    Files.newInputStream(file).use { input ->
        XSSFWorkbook(input).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            require(sheet.lastRowNum <= 10_000)
            val columns = sheet.getRow(0).lastCellNum.toInt()
            require(columns in 2..50)
            val formatter = DataFormatter()
            CSVPrinter(csv, CSVFormat.DEFAULT).use { printer ->
                sheet.forEach { row ->
                    val values =
                        (0 until columns).map { index ->
                            formatter.formatCellValue(row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))
                        }
                    if (values.any(String::isNotBlank)) printer.printRecord(values)
                }
            }
        }
    }
    return parseGradeCsv(csv.toString(), students)
}

internal class PortalStore(
    private val database: Path,
) {
    private fun connect(): Connection =
        DriverManager.getConnection("jdbc:sqlite:$database").apply {
            createStatement().use { it.execute("PRAGMA busy_timeout = 5000") }
        }

    fun installationId(): String =
        connect().use { db ->
            db.change("INSERT OR IGNORE INTO portal_installation(singleton, id) VALUES (1, ?)", UUID.randomUUID().toString())
            db.rows("SELECT id FROM portal_installation WHERE singleton = 1").single().text("id")
        }

    fun pendingCount(): Long =
        connect().use { db ->
            trackedTables.sumOf { table ->
                db.rows("SELECT count(*) AS total FROM $table WHERE syncStatus = 'PENDING'").single().number("total")
            }
        }

    fun parentPin(parentPhone: String): String? {
        val phone = normalizePortalPhone(parentPhone).ifEmpty { return null }
        return connect().use { db ->
            db.change("INSERT OR IGNORE INTO parent_account(normalizedParentPhone, accessPin) VALUES (?, ?)", phone, generatePortalPin())
            db.rows("SELECT accessPin FROM parent_account WHERE normalizedParentPhone = ?", phone).single().text("accessPin")
        }
    }

    fun teacherPin(teacherId: String): String? =
        connect().use { db ->
            db.change("UPDATE teacher SET portalPin = ? WHERE id = ? AND portalPin = ''", generatePortalPin(), teacherId)
            db.rows("SELECT portalPin FROM teacher WHERE id = ?", teacherId).singleOrNull()?.text("portalPin")
        }

    fun gradebook(studentId: String): Gradebook =
        connect().use { db ->
            val rosters = db.rows("SELECT studentId, classId FROM enrollment").groupBy { it.text("studentId") }
            Gradebook(
                students =
                    db
                        .rows("SELECT id, name, coalesce(code, id) AS code FROM student WHERE isActive = 1 ORDER BY name")
                        .map { row ->
                            PortalStudent(
                                row.text("id"),
                                row.text("name"),
                                row.text("code"),
                                rosters[row.text("id")].orEmpty().map { it.text("classId") }.toSet(),
                            )
                        },
                groups =
                    db
                        .rows("SELECT id, name FROM class_group WHERE status = 'ACTIVE' ORDER BY name")
                        .map { PortalGroup(it.text("id"), it.text("name")) },
                grades =
                    db.rows(
                        """SELECT exam_grade.*, class_group.name AS groupName
                           FROM exam_grade LEFT JOIN class_group ON class_group.id = exam_grade.groupId
                           WHERE exam_grade.studentId = ? ORDER BY date DESC, exam_grade.id""",
                        studentId,
                    ),
            )
        }

    fun saveGrades(
        groupId: String,
        title: String,
        maximum: Double,
        notes: String,
        grades: List<GradeInput>,
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.length in 1..160 && maximum.isFinite() && maximum > 0)
        require(grades.isNotEmpty() && grades.map { it.studentId }.distinct().size == grades.size)
        require(grades.all { it.score.isFinite() && it.score in 0.0..maximum })
        connect().use { db ->
            db.transaction {
                val enrolled =
                    rows(
                        """SELECT student.id FROM enrollment JOIN student ON student.id = enrollment.studentId
                       JOIN class_group ON class_group.id = enrollment.classId
                       WHERE class_group.id = ? AND class_group.status = 'ACTIVE' AND student.isActive = 1""",
                        groupId,
                    ).map { it.text("id") }.toSet()
                require(grades.all { it.studentId in enrolled })
                val now = System.currentTimeMillis()
                grades.forEach { grade ->
                    change(
                        """INSERT INTO exam_grade(id, studentId, groupId, examTitle, score, maxScore, date, notes)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                        UUID.randomUUID().toString(),
                        grade.studentId,
                        groupId,
                        cleanTitle,
                        grade.score,
                        maximum,
                        now,
                        notes.trim(),
                    )
                }
            }
        }
    }

    fun importPortalGrades(grades: Map<String, Fields>) =
        connect().use { db ->
            db.transaction {
                grades.forEach { (id, grade) ->
                    change(
                        """INSERT OR IGNORE INTO exam_grade(id, studentId, groupId, examTitle, score, maxScore, date, notes)
                       SELECT ?, ?, ?, ?, ?, ?, ?, ? WHERE EXISTS (SELECT 1 FROM student WHERE id = ?)""",
                        id,
                        grade.text("studentId"),
                        grade.text("groupId"),
                        grade.text("examTitle"),
                        grade.amount("score"),
                        grade.amount("maxScore"),
                        grade.number("date"),
                        grade.text("notes"),
                        grade.text("studentId"),
                    )
                    // Imported grades already have a cloud copy.
                    change("UPDATE exam_grade SET syncStatus = 'SYNCED' WHERE id = ?", id)
                }
            }
        }

    fun initializePins() =
        connect().use { db ->
            db.transaction {
                val phones =
                    rows("SELECT parentPhone FROM student WHERE isActive = 1")
                        .map { normalizePortalPhone(it.text("parentPhone")) }
                        .filter(String::isNotEmpty)
                        .toSet()
                val teachers = rows("SELECT id, portalPin FROM teacher WHERE portalPin = ''")
                ensurePins(phones, teachers)
            }
        }

    private fun Connection.ensurePins(
        phones: Set<String>,
        teachers: List<Fields>,
    ) {
        val existing = rows("SELECT normalizedParentPhone FROM parent_account").map { it.text("normalizedParentPhone") }.toSet()
        (phones - existing).forEach { phone ->
            change("INSERT OR IGNORE INTO parent_account(normalizedParentPhone, accessPin) VALUES (?, ?)", phone, generatePortalPin())
        }
        teachers.filter { it.text("portalPin").isEmpty() }.forEach {
            change("UPDATE teacher SET portalPin = ? WHERE id = ? AND portalPin = ''", generatePortalPin(), it.text("id"))
        }
    }

    fun snapshot(now: Long): PortalSnapshot =
        connect().use { db ->
            db.transaction {
                val local = trackedTables.associateWith { rows("SELECT * FROM $it") }
                val students = local.getValue("student").filter { it.number("isActive") == 1L }
                val families = students.groupBy { normalizePortalPhone(it.text("parentPhone")) }.filterKeys { it.isNotEmpty() }
                val groups = rows("SELECT * FROM class_group WHERE status = 'ACTIVE'").associateBy { it.text("id") }
                val enrollments = rows("SELECT * FROM enrollment").filter { it.text("classId") in groups }.groupBy { it.text("studentId") }
                val teachers = rows("SELECT * FROM teacher")
                ensurePins(families.keys, teachers)
                val passwords = rows("SELECT * FROM parent_account").associate { it.text("normalizedParentPhone") to it.text("accessPin") }
                val teacherPins = rows("SELECT id, portalPin FROM teacher").associate { it.text("id") to it.text("portalPin") }
                val month = YearMonth.from(Instant.ofEpochMilli(now).atZone(cairo))
                val remaining = balances(month).groupBy { it.text("studentId") }
                val projection = buildProjection(local, families, passwords, groups, enrollments, teachers, teacherPins, remaining)
                PortalSnapshot(projection, local.mapValues { (_, entries) -> entries.filter { it.text("syncStatus") == "PENDING" } })
            }
        }

    private fun Connection.balances(month: YearMonth): List<Fields> =
        rows(
            """SELECT enrollment.studentId,
           max(0, class_group.monthlyPriceMinor / 100.0 - enrollment.discountAmount - coalesce(sum(payment.amountPaid), 0)) AS remaining
           FROM enrollment JOIN class_group ON class_group.id = enrollment.classId
           LEFT JOIN payment ON payment.studentId = enrollment.studentId AND payment.classId = enrollment.classId
               AND (payment.billingMonth = ? OR (payment.billingMonth IS NULL AND payment.paymentDate >= ? AND payment.paymentDate < ?))
           WHERE class_group.status = 'ACTIVE' AND class_group.monthlyPriceMinor IS NOT NULL
           GROUP BY enrollment.studentId, enrollment.classId""",
            month.atDay(1).toString(),
            month
                .atDay(1)
                .atStartOfDay(cairo)
                .toInstant()
                .toEpochMilli(),
            month
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(cairo)
                .toInstant()
                .toEpochMilli(),
        )

    fun acknowledge(snapshot: PortalSnapshot) =
        connect().use { db ->
            db.transaction {
                snapshot.pending.forEach { (table, rows) ->
                    rows.forEach { row ->
                        if (table == "attendance_session") {
                            change(
                                """UPDATE attendance_session SET syncStatus = 'SYNCED'
                               WHERE personType = ? AND personId = ? AND date = ? AND groupId = ? AND updatedAt = ?""",
                                row.text("personType"),
                                row.text("personId"),
                                row.text("date"),
                                row.text("groupId"),
                                row.number("updatedAt"),
                            )
                        } else {
                            change(
                                "UPDATE $table SET syncStatus = 'SYNCED' WHERE id = ? AND updatedAt = ?",
                                row.text("id"),
                                row.number("updatedAt"),
                            )
                        }
                    }
                }
            }
        }
}

internal val cairo: ZoneId = ZoneId.of("Africa/Cairo")
private val trackedTables =
    listOf("student", "attendance", "attendance_session", "payment", "treasury_expense", "message_queue", "exam_grade")

private fun Connection.rows(
    sql: String,
    vararg args: Any,
): List<Fields> =
    prepareStatement(sql).use { statement ->
        args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
        statement.executeQuery().use { results ->
            buildList { while (results.next()) add(results.fields()) }
        }
    }

private fun ResultSet.fields(): Fields = (1..metaData.columnCount).associate { metaData.getColumnLabel(it) to getObject(it) }

private fun Connection.change(
    sql: String,
    vararg args: Any,
) {
    prepareStatement(sql).use { statement ->
        args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
        statement.executeUpdate()
    }
}

private fun <T> Connection.transaction(block: Connection.() -> T): T {
    autoCommit = false
    try {
        return block().also { commit() }
    } catch (failure: Exception) {
        rollback()
        throw failure
    } finally {
        autoCommit = true
    }
}
