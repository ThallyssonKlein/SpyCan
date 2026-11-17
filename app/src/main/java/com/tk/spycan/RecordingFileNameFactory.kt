package com.tk.spycan

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordingFileNameFactory(private val clock: Clock) {
    private val timestampFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN, Locale.US)

    fun create(): String = "$FILE_NAME_PREFIX${LocalDateTime.now(clock).format(timestampFormatter)}"

    private companion object {
        const val FILE_NAME_PREFIX = "SpyCan_"
        const val TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"
    }
}
