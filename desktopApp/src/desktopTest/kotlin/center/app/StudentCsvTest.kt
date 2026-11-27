package center.app

import center.database.SqlStudentRepository
import center.database.openDatabase
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupStatus
import center.domain.model.Student
import center.domain.model.Teacher
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StudentCsvTest {
    @Test
    fun parsesArabicAndQuotedExcelRecordsWithOrWithoutBom() {
        val row = "\"مريم، أحمد, \"\"علي\"\"\",الصف 1 الابتدائي,01000123456,01100123456,\"مجموعة\r\nأ\""
        val input = studentCsvTemplate + row
        val expected = StudentCsvRow(2, "مريم، أحمد, \"علي\"", "الصف 1 الابتدائي", "01000123456", "01100123456", "مجموعة\r\nأ", null)

        assertTrue(studentCsvTemplate.encodeToByteArray().take(3) == listOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()))
        assertEquals(listOf(expected), parseStudentCsv(input))
        assertEquals(listOf(expected), parseStudentCsv(input.removePrefix("\uFEFF")))
    }

    @Test
    fun acceptsSemicolonExportsAndAnOptionalCode() {
        val input = studentCsvTemplate.trimEnd().replace(',', ';') + ";code\nعلي;Grade 1;;0110;;ST-1"

        assertEquals(listOf(StudentCsvRow(2, "علي", "Grade 1", null, "0110", "", "ST-1")), parseStudentCsv(input))
    }

    @Test
    fun reportsMalformedRecords() {
        val error =
            assertFailsWith<InvalidStudentCsv> {
                parseStudentCsv(studentCsvTemplate + "\"اسم غير مكتمل,Grade 1,,0110,")
            }
        assertEquals(2, error.record)
    }

    @Test
    fun skipsExactDuplicatesAcrossFileAndDatabaseAndGeneratesCodes() =
        runTest {
            val directory = Files.createTempDirectory("center-csv-test")
            try {
                openDatabase(directory).use { driver ->
                    val app = koinApplication { modules(persistenceModule(driver)) }
                    try {
                        val services = app.koin.get<DirectoryServices>()
                        val repository = app.koin.get<SqlStudentRepository>()
                        services.teachers.save(Teacher("teacher-1", "أحمد", "الرياضيات"))
                        services.groups.save(
                            ClassGroup("group-1", "مجموعة أ", "teacher-1", 1000, 0, 1, EducationSystem.BOTH, GroupStatus.ACTIVE),
                        )
                        repository.save(Student("old", "علي", gradeLevel = "Grade 1", parentPhone = "0110"))
                        val file = directory.resolve("students.csv")
                        val newRow = "مريم,الصف ١ الابتدائي,0100,0110,مجموعة أ\r\n"
                        Files.writeString(file, studentCsvTemplate + "علي,primary-1,,0110,\r\n" + newRow + newRow)

                        assertEquals(SqlStudentRepository.ImportSummary(1, 2), importStudentCsv(file, services, repository))
                        assertEquals(SqlStudentRepository.ImportSummary(0, 3), importStudentCsv(file, services, repository))

                        val imported = repository.students().single { it.id != "old" }
                        assertEquals("مريم", imported.fullName)
                        assertEquals(imported.id, imported.code)
                        assertEquals("0100", imported.studentPhone)
                        assertEquals("primary-1", imported.gradeLevel)
                        assertEquals(setOf("group-1"), imported.enrolledGroupIds)

                        Files.writeString(
                            file,
                            studentCsvTemplate.trimEnd() + ",code\nسارة,Grade 1,,0111,,new\nمنى,Grade 1,,0112,,old",
                        )
                        assertFailsWith<java.sql.SQLException> { importStudentCsv(file, services, repository) }
                        assertEquals(2, repository.students().size)
                    } finally {
                        app.close()
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
