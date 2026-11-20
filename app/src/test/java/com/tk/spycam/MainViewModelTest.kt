package com.tk.spycam

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private fun makeMockStateHolder(initialState: RecordingState): RecordingStateHolder {
    val stateHolder = mockk<RecordingStateHolder>()
    every { stateHolder.state } returns MutableStateFlow(initialState)
    return stateHolder
}

private fun makeMockMetrics(): Metrics = mockk(relaxUnitFun = true)

class MainViewModelTest {
    @Test
    fun `should request start with the tapped lens when idle`() =
        runTest {
            val viewModel = MainViewModel(makeMockStateHolder(RecordingState.Idle), makeMockMetrics())

            viewModel.commands.test {
                viewModel.onLensTapped(Lens.FRONT)

                assertEquals(MainCommand.StartRequested(Lens.FRONT), awaitItem())
            }
        }

    @Test
    fun `should request stop when a recording is active`() =
        runTest {
            val stateHolder = makeMockStateHolder(RecordingState.Recording(Lens.BACK))
            val viewModel = MainViewModel(stateHolder, makeMockMetrics())

            viewModel.commands.test {
                viewModel.onLensTapped(Lens.BACK)

                assertEquals(MainCommand.StopRequested, awaitItem())
            }
        }

    @Test
    fun `should count the tapped lens`() {
        val metrics = makeMockMetrics()
        val viewModel = MainViewModel(makeMockStateHolder(RecordingState.Idle), metrics)

        viewModel.onLensTapped(Lens.BACK)

        verify { metrics.count("lens_tapped", "lens" to "BACK") }
    }

    @Test
    fun `should expose the recording state of the holder`() {
        val state = RecordingState.Recording(Lens.FRONT)
        val viewModel = MainViewModel(makeMockStateHolder(state), makeMockMetrics())

        assertEquals(state, viewModel.recordingState.value)
    }
}
