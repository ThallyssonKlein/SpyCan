package com.tk.spycan

import com.tk.spycan.databinding.ActivityMainBinding

class RecordingControls(private val binding: ActivityMainBinding) {
    fun render(state: RecordingState) {
        when (state) {
            RecordingState.Idle -> renderIdle()
            is RecordingState.Recording -> renderRecording(state.lens)
        }
    }

    // The active lens' button becomes Stop; the other is disabled while recording.
    private fun renderRecording(lens: Lens) {
        val recordingBack = lens == Lens.BACK
        binding.statusText.setText(R.string.main_status_recording)
        binding.backButton.setText(if (recordingBack) R.string.main_stop_back else R.string.main_record_back)
        binding.frontButton.setText(if (recordingBack) R.string.main_record_front else R.string.main_stop_front)
        binding.backButton.isEnabled = recordingBack
        binding.frontButton.isEnabled = !recordingBack
    }

    private fun renderIdle() {
        binding.statusText.setText(R.string.main_status_idle)
        binding.backButton.setText(R.string.main_record_back)
        binding.frontButton.setText(R.string.main_record_front)
        binding.backButton.isEnabled = true
        binding.frontButton.isEnabled = true
    }
}
