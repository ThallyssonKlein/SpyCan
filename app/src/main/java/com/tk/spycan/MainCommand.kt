package com.tk.spycan

sealed interface MainCommand {
    data class StartRequested(val lens: Lens) : MainCommand

    data object StopRequested : MainCommand
}
