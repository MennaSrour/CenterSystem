package center.database

import center.database.dao.AttendanceDao
import center.database.dao.ClassGroupDao
import center.database.dao.MessageQueueDao
import center.database.dao.PaymentDao
import center.database.dao.StudentDao
import center.database.dao.TeacherDao
import kotlinx.coroutines.CoroutineDispatcher

class CenterDaos(
    database: CenterDatabase,
    dispatcher: CoroutineDispatcher,
) {
    val teachers = TeacherDao(database, dispatcher)
    val students = StudentDao(database, dispatcher)
    val classGroups = ClassGroupDao(database, dispatcher)
    val attendance = AttendanceDao(database, dispatcher)
    val payments = PaymentDao(database, dispatcher)
    val messages = MessageQueueDao(database, dispatcher)
}
