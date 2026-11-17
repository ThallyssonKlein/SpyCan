package com.tk.spycan

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class RecordingFileNameFactoryTest {
    @Test
    fun `should build the file name from the prefix and the clock timestamp`() {
        val clock = Clock.fixed(Instant.parse("2024-03-05T10:15:30Z"), ZoneOffset.UTC)
        val factory = RecordingFileNameFactory(clock)

        val fileName = factory.create()

        assertEquals("SpyCan_20240305_101530", fileName)
    }
}
