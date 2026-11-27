package center.app.portal

import java.security.MessageDigest
import java.time.Instant
import java.time.YearMonth

internal data class PortalAccount(
    val pin: String,
    val fields: Fields,
)

internal data class PortalDocument(
    val fields: Fields,
    val push: Boolean = false,
)

internal data class PortalProjection(
    val parents: Map<String, PortalAccount>,
    val teachers: Map<String, PortalAccount>,
    val documents: Map<String, PortalDocument>,
)

internal data class PortalSnapshot(
    val projection: PortalProjection,
    val pending: Map<String, List<Fields>>,
)

internal fun portalHash(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

internal fun buildProjection(
    local: Map<String, List<Fields>>,
    families: Map<String, List<Fields>>,
    passwords: Map<String, String>,
    groups: Map<String, Fields>,
    enrollments: Map<String, List<Fields>>,
    teachers: List<Fields>,
    teacherPins: Map<String, String>,
    remaining: Map<String, List<Fields>>,
): PortalProjection {
    val students = families.flatMap { (phone, children) -> children.map { it.text("id") to (phone to it) } }.toMap()
    val children = mutableMapOf<String, PortalDocument>()
    val rosters = mutableMapOf<String, MutableList<Fields>>()
    students.forEach { (id, family) ->
        val (phone, student) = family
        val enrolled = enrollments[id].orEmpty()
        val code = student.text("code").ifBlank { id }
        val name = student.text("name")
        val activeGroupIds = enrolled.map { it.text("classId") }
        val monthlyDue =
            enrolled.sumOf { entry ->
                (
                    groups
                        .getValue(
                            entry.text("classId"),
                        ).amount("monthlyPriceMinor") / 100 - entry.amount("discountAmount")
                ).coerceAtLeast(0.0)
            }
        val childPath = documentPath(phone, "children", id)
        children[childPath] =
            PortalDocument(
                mapOf(
                    "studentId" to id,
                    "name" to name,
                    "code" to code,
                    "grade" to student.text("gradeLevel"),
                    "activeGroups" to activeGroupIds.map { groups.getValue(it).text("name") },
                    "activeGroupIds" to activeGroupIds,
                    "monthlyDue" to monthlyDue,
                    "overdueAmount" to remaining[id].orEmpty().sumOf { it.amount("remaining") },
                ),
            )
        activeGroupIds.forEach { groupId ->
            rosters.getOrPut(groupId) { mutableListOf() }.add(
                mapOf(
                    "id" to id,
                    "name" to name,
                    "code" to code,
                    "parentPhone" to phone,
                    "childDocumentId" to childPath.substringAfterLast('/'),
                ),
            )
        }
    }

    val teacherAccounts =
        teachers
            .groupBy { normalizePortalPhone(it.text("phone")) }
            .filter { (phone, matches) -> phone.isNotEmpty() && matches.size == 1 }
            .mapValues { (_, matches) ->
                val teacher = matches.single()
                val id = teacher.text("id")
                val assigned =
                    groups.values.filter { it.text("teacherId") == id && it.text("status") == "ACTIVE" }.map { group ->
                        val groupId = group.text("id")
                        mapOf("id" to groupId, "name" to group.text("name"), "students" to rosters[groupId].orEmpty())
                    }
                PortalAccount(teacherPins.getValue(id), mapOf("teacherId" to id, "name" to teacher.text("fullName"), "groups" to assigned))
            }

    return PortalProjection(
        parents = families.mapValues { (phone, _) -> PortalAccount(passwords.getValue(phone), emptyMap()) },
        teachers = teacherAccounts,
        documents = children + activityDocuments(local, students, groups),
    )
}

private fun activityDocuments(
    local: Map<String, List<Fields>>,
    students: Map<String, Pair<String, Fields>>,
    groups: Map<String, Fields>,
): Map<String, PortalDocument> =
    buildMap {
        fun add(
            section: String,
            id: String,
            studentId: String,
            fields: Fields,
        ) {
            val (phone, student) = students[studentId] ?: return
            put(
                documentPath(phone, section, id),
                PortalDocument(fields + mapOf("studentId" to studentId, "studentName" to student.text("name"))),
            )
        }

        fun notify(
            id: String,
            type: String,
            studentId: String,
            at: Long,
            source: Fields,
            text: String = "",
        ) {
            val (phone, student) = students[studentId] ?: return
            val title =
                when (type) {
                    "CHECK_IN" -> "تم تسجيل الحضور"
                    "CHECK_OUT" -> "تم تسجيل الانصراف"
                    "PAYMENT_CONFIRMED" -> "تم تأكيد الدفع"
                    "OVERDUE_REMINDER" -> "تذكير بالرسوم"
                    else -> "درجة امتحان جديدة"
                }
            put(
                documentPath(phone, "notifications", id),
                PortalDocument(
                    mapOf(
                        "title" to title,
                        "body" to text.ifBlank { title },
                        "type" to type,
                        "studentId" to studentId,
                        "studentName" to student.text("name"),
                        "timestamp" to at,
                    ),
                    push =
                        source.number("updatedAt") > 0 &&
                            source.text("syncStatus") == "PENDING" &&
                            !(type == "PAYMENT_CONFIRMED" && source.number("voidedAt") > 0),
                ),
            )
        }
        local["attendance_session"].orEmpty().filter { it.text("personType") == "STUDENT" }.forEach { row ->
            val studentId = row.text("personId")
            val id = portalHash(listOf(studentId, row.text("groupId"), row.text("date")).joinToString("\u0000"))
            val checkIn = row.number("checkInTime")
            val checkOut = (row["checkOutTime"] as? Number)?.toLong()
            add("attendance", id, studentId, mapOf("date" to row.text("date"), "checkInTime" to checkIn, "checkOutTime" to checkOut))
            notify("check-in-$id", "CHECK_IN", studentId, checkIn, row)
            if (checkOut != null) notify("check-out-$id", "CHECK_OUT", studentId, checkOut, row)
        }
        local["attendance"].orEmpty().filter { it.text("status") == "PRESENT" }.forEach { row ->
            val id = row.text("id")
            val studentId = row.text("studentId")
            val checkIn = row.number("timestamp")
            val date =
                Instant
                    .ofEpochMilli(checkIn)
                    .atZone(cairo)
                    .toLocalDate()
                    .toString()
            add("attendance", id, studentId, mapOf("date" to date, "checkInTime" to checkIn, "checkOutTime" to null))
            notify("attendance-${portalHash(id)}", "CHECK_IN", studentId, checkIn, row)
        }
        local["payment"].orEmpty().forEach { row ->
            val id = row.text("id")
            val studentId = row.text("studentId")
            val date = row.number("paymentDate")
            val month = row.text("billingMonth").take(7).ifBlank { YearMonth.from(Instant.ofEpochMilli(date).atZone(cairo)).toString() }
            add(
                "payments",
                id,
                studentId,
                mapOf(
                    "amount" to row.amount("amountPaid"),
                    "date" to date,
                    "month" to month,
                    "status" to if (row.number("voidedAt") > 0) "VOIDED" else "CONFIRMED",
                    "voidedAt" to row.number("voidedAt").takeIf { it > 0 },
                    "voidReason" to row.text("voidReason").takeIf { it.isNotBlank() },
                ),
            )
            notify("payment-${portalHash(id)}", "PAYMENT_CONFIRMED", studentId, date, row)
        }
        local["exam_grade"].orEmpty().forEach { row ->
            val id = row.text("id")
            val studentId = row.text("studentId")
            val groupId = row.text("groupId")
            val date = row.number("date")
            add(
                "grades",
                id,
                studentId,
                mapOf(
                    "groupId" to groupId,
                    "groupName" to groups[groupId]?.text("name").orEmpty(),
                    "examTitle" to row.text("examTitle"),
                    "score" to row.amount("score"),
                    "maxScore" to row.amount("maxScore"),
                    "date" to date,
                    "notes" to row.text("notes"),
                    "updatedAt" to row.number("updatedAt"),
                ),
            )
            notify("grade-$id", "EXAM_GRADE", studentId, date, row)
        }
        local["message_queue"]
            .orEmpty()
            .filter { it.text("reminderStudentId").isNotEmpty() || it.text("channel") == "NOTIFICATION" }
            .forEach { row ->
                notify(
                    "reminder-${portalHash(row.text("id"))}",
                    "OVERDUE_REMINDER",
                    row.text("reminderStudentId").ifBlank {
                        row.text("targetPhone")
                    },
                    row.number("createdAt"),
                    row,
                    row.text("messageBody"),
                )
            }
    }

private fun documentPath(
    phone: String,
    section: String,
    id: String,
): String {
    val safeId =
        id.takeIf {
            it.toByteArray().size <= 1200 &&
                it.isNotBlank() &&
                '/' !in it &&
                it != "." &&
                it != ".." &&
                !it.matches(Regex("__.*__"))
        }
    return "parents/$phone/$section/${safeId ?: portalHash(id)}"
}
