package center.app.portal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StalePushTest {
    @Test
    fun suppressesCheckInAndCheckOutPushesOlderThanThreeHours() {
        val now = 50_000_000L
        val staleAt = now - ATTENDANCE_PUSH_MAX_AGE_MILLIS - 1

        assertFalse(shouldDispatchPush("CHECK_IN", staleAt, now))
        assertFalse(shouldDispatchPush("CHECK_OUT", staleAt, now))
        assertTrue(shouldDispatchPush("CHECK_IN", now - ATTENDANCE_PUSH_MAX_AGE_MILLIS, now))
        assertTrue(shouldDispatchPush("PAYMENT_CONFIRMED", staleAt, now))
    }
}
