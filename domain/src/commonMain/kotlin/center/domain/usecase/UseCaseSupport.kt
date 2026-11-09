package center.domain.usecase

import center.domain.model.Student
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

fun interface EpochClock {
    fun nowMillis(): Long
}

fun interface IdGenerator {
    fun newId(): String
}

class StudentNotFoundException(
    code: String,
) : IllegalArgumentException("Student not found: $code")

class ClassTeacherNotFoundException(
    classId: String,
) : IllegalArgumentException("Class teacher not found: $classId")

internal suspend fun <T> domainResult(block: suspend () -> T): Result<T> =
    try {
        currentCoroutineContext().ensureActive()
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        Result.failure(exception)
    }

internal fun Student.requireNotificationDetails() {
    require(id.isNotBlank()) { "Student ID is required" }
    require(isActive) { "Student is inactive" }
    require(fullName.isNotBlank()) { "Student name is required" }
    require(parentPhone.isNotBlank()) { "Parent phone is required" }
}
