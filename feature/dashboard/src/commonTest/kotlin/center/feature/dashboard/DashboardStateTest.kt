package center.feature.dashboard

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardStateTest {
    @Test
    fun blankCodesCannotBeSubmitted() {
        listOf("", " ", "\t\n").forEach { code ->
            assertFalse(DashboardState(userName = "", dateLabel = "", studentCode = code).canCheckIn)
        }
    }

    @Test
    fun validCodeCanBeSubmittedAfterAnError() {
        val state =
            DashboardState(
                userName = "",
                dateLabel = "",
                studentCode = "10234",
                checkIn = CheckInState.Failure("Try again"),
            )
        assertTrue(state.canCheckIn)
    }

    @Test
    fun inFlightSubmissionDisablesDuplicateRequests() {
        val state =
            DashboardState(
                userName = "",
                dateLabel = "",
                studentCode = "10234",
                checkIn = CheckInState.Submitting,
            )
        assertFalse(state.canCheckIn)
    }
}
