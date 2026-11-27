package center.domain

import center.domain.usecase.SequentialPersonCodeGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SequentialPersonCodeGeneratorTest {
    @Test
    fun startsAtOneThousandAndAdvancesFromHighestMatchingCode() {
        val generator = SequentialPersonCodeGenerator()
        assertEquals("ST-1001", generator.next("ST", emptyList()))
        assertEquals("TR-1009", generator.next("TR", listOf("TR-1002", "TR-1008", "legacy-id")))
        assertEquals("SF-1001", generator.next("SF", listOf("ST-9000", "SF-invalid")))
    }

    @Test
    fun rejectsUnknownPersonCodePrefixes() {
        assertFailsWith<IllegalArgumentException> {
            SequentialPersonCodeGenerator().next("XX", emptyList())
        }
    }
}
