package com.tk.spycan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

class MainViewModel(
    private val stateHolder: RecordingStateHolder,
    private val metrics: Metrics,
) : ViewModel() {
    val recordingState: StateFlow<RecordingState> = stateHolder.state

    private val _commands = Channel<MainCommand>(Channel.BUFFERED)
    val commands: Flow<MainCommand> = _commands.receiveAsFlow()

    fun onLensTapped(lens: Lens) {
        val command =
            when (stateHolder.state.value) {
                is RecordingState.Recording -> MainCommand.StopRequested
                RecordingState.Idle -> MainCommand.StartRequested(lens)
            }
        Timber.i("[MainViewModel] lens tapped: %s, command: %s", lens, command)
        metrics.count(LENS_TAPPED_METRIC, LENS_TAG to lens.name)
        _commands.trySend(command)
    }

    private companion object {
        const val LENS_TAPPED_METRIC = "lens_tapped"
        const val LENS_TAG = "lens"
    }
}
