package com.tk.spycan

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RecordingStateHolderTest {
    @Test
    fun `should start idle`() {
        val holder = RecordingStateHolder()

        assertEquals(RecordingState.Idle, holder.state.value)
    }

    @Test
    fun `should emit recording with the lens when marked recording`() =
        runTest {
            val holder = RecordingStateHolder()

            holder.state.test {
                assertEquals(RecordingState.Idle, awaitItem())
                holder.markRecording(Lens.FRONT)
                assertEquals(RecordingState.Recording(Lens.FRONT), awaitItem())
            }
        }

    @Test
    fun `should emit idle when marked idle after recording`() =
        runTest {
            val holder = RecordingStateHolder()
            holder.markRecording(Lens.BACK)

            holder.state.test {
                assertEquals(RecordingState.Recording(Lens.BACK), awaitItem())
                holder.markIdle()
                assertEquals(RecordingState.Idle, awaitItem())
            }
        }
}
