package com.tk.spycam

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class LensTest {
    @ParameterizedTest
    @CsvSource("BACK,BACK", "FRONT,FRONT", "unknown,BACK")
    fun `should resolve the lens matching the extra value`(
        extra: String,
        expected: Lens,
    ) {
        assertEquals(expected, Lens.fromExtra(extra))
    }

    @Test
    fun `should fall back to the back lens when the extra is missing`() {
        assertEquals(Lens.BACK, Lens.fromExtra(null))
    }
}
