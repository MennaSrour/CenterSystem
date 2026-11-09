package center.domain

typealias Student = center.domain.model.Student
typealias StudentRepository = center.domain.repository.StudentRepository

class RegisterStudent(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(
        id: String,
        name: String,
    ) {
        require(id.isNotBlank()) { "Student ID is required" }
        require(name.isNotBlank()) { "Student name is required" }
        val existing = repository.getStudentByCode(id.trim())
        repository.save(existing?.copy(fullName = name.trim()) ?: Student(id.trim(), name.trim()))
    }
}
