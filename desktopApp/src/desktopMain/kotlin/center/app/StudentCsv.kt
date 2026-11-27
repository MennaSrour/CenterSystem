package center.app

import center.database.SqlStudentRepository
import center.domain.model.EducationSystem
import center.domain.model.GroupStatus
import center.domain.model.Student
import center.ui.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

private val requiredColumns = listOf("name", "grade", "studentPhone", "parentPhone", "groupName")
internal val studentCsvTemplate = "\uFEFF" + requiredColumns.joinToString(",") + "\r\n"

internal data class StudentCsvRow(
    val record: Int,
    val name: String,
    val grade: String,
    val studentPhone: String?,
    val parentPhone: String,
    val groupName: String,
    val code: String?,
)

internal class InvalidStudentCsv(
    val record: Int,
) : IllegalArgumentException("Invalid CSV record $record")

internal fun parseStudentCsv(text: String): List<StudentCsvRow> {
    val content = text.removePrefix("\uFEFF")
    val firstLine = content.lineSequence().first()
    val records = CsvRecords(content, if (';' in firstLine && ',' !in firstLine) ';' else ',')
    val header = records.next()?.map(String::trim).orEmpty()
    if (header.distinct().size != header.size ||
        !header.containsAll(requiredColumns) ||
        header.any { it !in requiredColumns && it != "code" }
    ) {
        throw InvalidStudentCsv(1)
    }
    return buildList {
        while (true) {
            val number = records.nextNumber
            val fields = records.next() ?: break
            if (fields.size == 1 && fields.single().isBlank()) continue
            if (fields.size != header.size) throw InvalidStudentCsv(number)
            val row = header.zip(fields.map(String::trim)).toMap()
            val name = row.getValue("name")
            val grade = row.getValue("grade")
            val parentPhone = row.getValue("parentPhone")
            if (name.isBlank() || grade.isBlank() || parentPhone.isBlank()) throw InvalidStudentCsv(number)
            add(
                StudentCsvRow(
                    number,
                    name,
                    grade,
                    row.getValue("studentPhone").ifEmpty { null },
                    parentPhone,
                    row.getValue("groupName"),
                    row["code"]?.ifEmpty { null },
                ),
            )
        }
    }
}

private class CsvRecords(
    private val content: String,
    private val delimiter: Char,
) {
    private var position = 0
    var nextNumber = 1
        private set

    fun next(): List<String>? {
        if (position == content.length) return null
        val fields = mutableListOf<String>()
        do {
            fields += readField()
        } while (consume(delimiter))
        if (!consume('\r') && !consume('\n') && position != content.length) {
            throw InvalidStudentCsv(nextNumber)
        }
        if (content.getOrNull(position - 1) == '\r') consume('\n')
        nextNumber++
        return fields
    }

    private fun readField(): String {
        if (consume('"')) {
            return buildString {
                while (position < content.length) {
                    val char = content[position++]
                    if (char == '"' && !consume('"')) return@buildString
                    append(char)
                }
                throw InvalidStudentCsv(nextNumber)
            }
        }
        val start = position
        while (position < content.length) {
            when (content[position]) {
                delimiter, '\r', '\n' -> break
                '"' -> throw InvalidStudentCsv(nextNumber)
                else -> position++
            }
        }
        return content.substring(start, position)
    }

    private fun consume(char: Char): Boolean {
        if (content.getOrNull(position) != char) return false
        position++
        return true
    }
}

internal suspend fun importStudentCsv(
    file: Path,
    services: DirectoryServices,
    repository: SqlStudentRepository,
): SqlStudentRepository.ImportSummary =
    withContext(Dispatchers.IO) {
        val rows = parseStudentCsv(Files.readString(file))
        val gradeAliases =
            AppLanguage.entries
                .flatMap(::gradeCatalog)
                .flatMap { grade ->
                    listOf(grade.id to grade, grade.label.normalizeSearch() to grade)
                }.toMap()
        val groups =
            services.groups
                .observeGroups()
                .first()
                .filter { it.status == GroupStatus.ACTIVE }
                .groupBy { it.name.trim() }
        val students =
            rows.map { row ->
                val grade = gradeAliases[row.grade.normalizeSearch()] ?: throw InvalidStudentCsv(row.record)
                val groupIds =
                    if (row.groupName.isEmpty()) {
                        emptySet()
                    } else {
                        setOf(groups[row.groupName]?.singleOrNull()?.id ?: throw InvalidStudentCsv(row.record))
                    }
                val id = services.ids.newId()
                Student(
                    id = id,
                    fullName = row.name,
                    code = row.code ?: id,
                    gradeLevel = grade.id,
                    stage = grade.stage,
                    system = EducationSystem.ARABIC,
                    studentPhone = row.studentPhone,
                    parentPhone = row.parentPhone,
                    enrolledGroupIds = groupIds,
                )
            }
        repository.importStudents(students, gradeAliases.mapValues { it.value.id })
    }
