package center.domain

import center.domain.model.EducationSystem

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
        val key = id.trim()
        val existing = repository.getStudentByCode(key) ?: repository.getStudentById(key)
        repository.save(existing?.copy(fullName = name.trim()) ?: Student(key, name.trim()))
    }

    suspend operator fun invoke(student: Student) {
        require(student.code.isNotBlank() && student.fullName.isNotBlank()) { "Student code and name are required" }
        require(
            student.gradeLevel.isNotBlank() && student.stage != null && student.system != null,
        ) { "Grade, stage and system are required" }
        require(student.system != EducationSystem.BOTH) { "Students must use one education system" }
        require(student.parentPhone.isNotBlank()) { "Parent phone is required" }
        repository.save(
            student.copy(
                code = student.code.trim(),
                fullName = student.fullName.trim(),
                parentPhone = student.parentPhone.trim(),
                studentPhone = student.studentPhone?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }
}
