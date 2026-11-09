package center.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegisterStudentTest {
    private val saved = mutableListOf<Student>()
    private val repository =
        object : StudentRepository {
            override suspend fun save(student: Student) {
                saved.add(student)
            }

            override suspend fun students(): List<Student> = saved.toList()

            override fun observeStudents() = kotlinx.coroutines.flow.flowOf(saved.toList())

            override suspend fun getStudentByCode(code: String) = saved.firstOrNull { it.id == code }
        }

    @Test
    fun trimsStudentDetailsBeforeSaving() =
        runTest {
            RegisterStudent(repository)(" 42 ", " Menna ")
            assertEquals(listOf(Student("42", "Menna")), saved)
        }

    @Test
    fun rejectsBlankDetailsWithoutWriting() =
        runTest {
            assertFailsWith<IllegalArgumentException> { RegisterStudent(repository)("", "Menna") }
            assertFailsWith<IllegalArgumentException> { RegisterStudent(repository)("42", " ") }
            assertEquals(emptyList(), saved)
        }
}
