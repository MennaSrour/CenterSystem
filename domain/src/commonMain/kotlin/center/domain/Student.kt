package center.domain

data class Student(
    val id: String,
    val name: String,
)

interface StudentRepository {
    suspend fun save(student: Student)

    suspend fun students(): List<Student>
}

class RegisterStudent(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(
        id: String,
        name: String,
    ) {
        require(id.isNotBlank()) { "Student ID is required" }
        require(name.isNotBlank()) { "Student name is required" }
        repository.save(Student(id.trim(), name.trim()))
    }
}
