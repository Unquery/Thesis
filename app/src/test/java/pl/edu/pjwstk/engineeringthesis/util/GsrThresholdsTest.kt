package pl.edu.pjwstk.engineeringthesis.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GsrThresholdsTest {

    @Test
    fun validGsrMeasurement_isStrictlyBetweenZeroAndOverRange() {
        assertFalse(isValidGsrMeasurement(0f))
        assertTrue(isValidGsrMeasurement(0.0001f))
        assertTrue(isValidGsrMeasurement(12.3456f))
        assertTrue(isValidGsrMeasurement(99.9999f))
        assertFalse(isValidGsrMeasurement(100f))
        assertFalse(isValidGsrMeasurement(101f))
    }

    @Test
    fun normalizeGsrForDisplay_mapsInvalidAndOverRangeValues() {
        assertNull(normalizeGsrForDisplay(0f))
        assertEquals(12.3456f, normalizeGsrForDisplay(12.3456f) ?: -1f, 0.0001f)
        assertEquals(GSR_OVER_RANGE_US, normalizeGsrForDisplay(100f) ?: -1f, 0f)
        assertEquals(GSR_OVER_RANGE_US, normalizeGsrForDisplay(101f) ?: -1f, 0f)
    }
}
