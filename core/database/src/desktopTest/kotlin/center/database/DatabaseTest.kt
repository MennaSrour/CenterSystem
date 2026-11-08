package center.database

import center.domain.Student
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
    @Test
    fun persistsAndUpdatesStudentAcrossConnections() =
        runTest {
            val directory = Files.createTempDirectory("center-test")
            try {
                openDatabase(directory).use { driver ->
                    val repository = SqlStudentRepository(CenterDatabase(driver))
                    repository.save(Student("42", "Menna"))
                    repository.save(Student("42", "Menna Srour"))
                }
                openDatabase(directory).use { driver ->
                    val repository = SqlStudentRepository(CenterDatabase(driver))
                    assertEquals(listOf(Student("42", "Menna Srour")), repository.students())
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
