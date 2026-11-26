package center.app.portal

import center.database.CenterDatabase
import center.database.openDatabase
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PortalTest {
    @Test
    fun pinIsSixDigitsAndSharedByNormalizedPhone() {
        val directory = Files.createTempDirectory("center-portal-test")
        try {
            openDatabase(directory).use {
                val store = PortalStore(directory.resolve("center.db"))
                val pin = assertNotNull(store.parentPin("+٢٠ ١٠١٢٣٤٥٦٧٨"))
                assertTrue(pin.matches(Regex("[0-9]{6}")))
                assertEquals(pin, store.parentPin("010-1234-5678"))
                assertTrue((1..20).all { generatePortalPin().matches(Regex("[0-9]{6}")) })
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun csvMatchesCodesAndQuotedNamesAndRejectsAmbiguousOrDuplicateRows() {
        val students =
            listOf(
                PortalStudent("one", "Mona, Adel", "A001", emptySet()),
                PortalStudent("two", "نور", "A002", emptySet()),
                PortalStudent("three", "نور", "A003", emptySet()),
            )
        assertEquals(
            listOf(GradeInput("one", 9.5), GradeInput("two", 8.0)),
            parseGradeCsv("\uFEFFcode,name,score\r\n,\"Mona, Adel\",9.5\r\nA002,,٨\r\n", students),
        )
        assertFailsWith<IllegalArgumentException> { parseGradeCsv("name,score\nنور,9", students) }
        assertFailsWith<IllegalArgumentException> { parseGradeCsv("code,score\nA001,7\nA001,8", students) }
        assertFailsWith<IllegalArgumentException> { parseGradeCsv("code,score\nA001,Infinity", students) }
    }

    @Test
    fun offlineFailureRetainsPendingRowsAndSuccessfulRetrySyncsBothSiblings() =
        runTest {
            val directory = Files.createTempDirectory("center-portal-test")
            try {
                openDatabase(directory).use { connection ->
                    val db = CenterDatabase(connection)
                    db.studentQueries.insertRow("s1", "Student One", "Grade 5", null, "01012345678", true)
                    db.studentQueries.insertRow("s2", "Student Two", "Grade 6", null, "+20 1012345678", true)
                    val store = PortalStore(directory.resolve("center.db"))
                    var attempts = 0
                    var uploaded: PortalProjection? = null
                    val cloud =
                        object : PortalCloud {
                            override fun testConnection(installationId: String) = Unit

                            override fun downloadGrades(installationId: String): Map<String, Fields> = emptyMap()

                            override fun publish(projection: PortalProjection) {
                                if (++attempts == 1) error("Disconnected")
                                uploaded = projection
                            }

                            override fun close() = Unit
                        }
                    val manager = CloudSyncManager(store, { cloud })
                    manager.sync()
                    assertEquals(0, attempts)
                    assertEquals(2L, store.pendingCount())
                    manager.configure(CloudSettings("fake-key.json"))
                    manager.sync()
                    assertFalse(manager.status.value.online)
                    assertEquals(2L, store.pendingCount())
                    manager.sync()
                    assertTrue(manager.status.value.online)
                    assertEquals(0L, store.pendingCount())
                    val projection = assertNotNull(uploaded)
                    assertEquals(setOf("01012345678"), projection.parents.keys)
                    assertEquals(
                        setOf("parents/01012345678/children/s1", "parents/01012345678/children/s2"),
                        projection.documents.keys,
                    )
                    assertEquals(store.parentPin("01012345678"), projection.parents.getValue("01012345678").pin)
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
