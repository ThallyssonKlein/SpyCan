@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tk.spycan

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private fun makeMockStateHolder(initialState: RecordingState = RecordingState.Idle): RecordingStateHolder {
    val stateHolder = mockk<RecordingStateHolder>()
    every { stateHolder.state } returns MutableStateFlow(initialState)
    every { stateHolder.markRecording(any()) } just Runs
    every { stateHolder.markIdle() } just Runs
    return stateHolder
}

private fun makeMockMetrics(): Metrics = mockk(relaxUnitFun = true)

private fun makeMockCameraRecorder(events: Flow<RecordingEvent>): CameraRecorder {
    val cameraRecorder = mockk<CameraRecorder>()
    every { cameraRecorder.record(any()) } returns events
    every { cameraRecorder.stopRecording() } just Runs
    return cameraRecorder
}

private fun neverEndingRecording(vararg events: RecordingEvent): Flow<RecordingEvent> =
    flow {
        events.forEach { emit(it) }
        awaitCancellation()
    }

private fun TestScope.makeSession(
    cameraRecorder: CameraRecorder,
    stateHolder: RecordingStateHolder = makeMockStateHolder(),
    metrics: Metrics = makeMockMetrics(),
) = RecordingSession(cameraRecorder, stateHolder, backgroundScope, metrics)

class RecordingSessionTest {
    @Test
    fun `should mark recording and count it when the camera reports started`() =
        runTest {
            val stateHolder = makeMockStateHolder()
            val metrics = makeMockMetrics()
            val session =
                makeSession(
                    cameraRecorder = makeMockCameraRecorder(neverEndingRecording(RecordingEvent.Started)),
                    stateHolder = stateHolder,
                    metrics = metrics,
                )

            session.start(Lens.FRONT)
            runCurrent()

            verify { stateHolder.markRecording(Lens.FRONT) }
            verify { metrics.count("recordings", "lens" to "FRONT", "status" to "started") }
        }

    @Test
    fun `should emit saved outcome and mark idle when the recording is saved`() =
        runTest {
            val stateHolder = makeMockStateHolder()
            val events =
                flow {
                    emit(RecordingEvent.Started)
                    emit(RecordingEvent.Saved("content://video/1"))
                }
            val session = makeSession(makeMockCameraRecorder(events), stateHolder)

            session.start(Lens.BACK)
            runCurrent()

            session.outcomes.test {
                assertEquals(RecordingOutcome.Saved("content://video/1"), awaitItem())
            }
            verify { stateHolder.markIdle() }
        }

    @Test
    fun `should emit failed outcome when the recording finalizes with an error`() =
        runTest {
            val metrics = makeMockMetrics()
            val events = flow { emit(RecordingEvent.Failed(errorCode = 2, cause = null)) }
            val session = makeSession(makeMockCameraRecorder(events), metrics = metrics)

            session.start(Lens.BACK)
            runCurrent()

            session.outcomes.test { assertEquals(RecordingOutcome.Failed, awaitItem()) }
            verify { metrics.count("recordings", "lens" to "BACK", "status" to "failed") }
        }

    @Test
    fun `should emit camera unavailable outcome when the camera cannot be bound`() =
        runTest {
            val stateHolder = makeMockStateHolder()
            val events = flow<RecordingEvent> { throw CameraUnavailableException(Lens.FRONT, IllegalStateException()) }
            val session = makeSession(makeMockCameraRecorder(events), stateHolder)

            session.start(Lens.FRONT)
            runCurrent()

            session.outcomes.test { assertEquals(RecordingOutcome.CameraUnavailable, awaitItem()) }
            verify { stateHolder.markIdle() }
        }

    @Test
    fun `should ignore start when a recording is already active`() =
        runTest {
            val cameraRecorder = makeMockCameraRecorder(neverEndingRecording(RecordingEvent.Started))
            val session = makeSession(cameraRecorder)

            session.start(Lens.BACK)
            runCurrent()
            session.start(Lens.FRONT)
            runCurrent()

            verify(exactly = 1) { cameraRecorder.record(any()) }
        }

    @Test
    fun `should allow a new start after the previous recording finished`() =
        runTest {
            val cameraRecorder = makeMockCameraRecorder(flow { emit(RecordingEvent.Saved("content://video/1")) })
            val session = makeSession(cameraRecorder)

            session.start(Lens.BACK)
            runCurrent()
            session.start(Lens.BACK)
            runCurrent()

            verify(exactly = 2) { cameraRecorder.record(Lens.BACK) }
        }

    @Test
    fun `should stop the camera recorder when stopped while recording`() =
        runTest {
            val stateHolder = makeMockStateHolder(RecordingState.Recording(Lens.BACK))
            val cameraRecorder = makeMockCameraRecorder(neverEndingRecording())
            val session = makeSession(cameraRecorder, stateHolder)

            session.stop()

            verify { cameraRecorder.stopRecording() }
        }

    @Test
    fun `should cancel the pending start and emit cancelled when stopped while idle`() =
        runTest {
            val stateHolder = makeMockStateHolder()
            val session = makeSession(makeMockCameraRecorder(neverEndingRecording()), stateHolder)
            session.start(Lens.BACK)
            runCurrent()

            session.stop()
            runCurrent()

            session.outcomes.test { assertEquals(RecordingOutcome.Cancelled, awaitItem()) }
            verify { stateHolder.markIdle() }
        }

    @Test
    fun `should emit cancelled when stopped without any recording job`() =
        runTest {
            val session = makeSession(makeMockCameraRecorder(neverEndingRecording()))

            session.stop()

            session.outcomes.test { assertEquals(RecordingOutcome.Cancelled, awaitItem()) }
        }
}
