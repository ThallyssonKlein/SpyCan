package com.tk.spycam

sealed interface MainCommand {
    data class StartRequested(val lens: Lens) : MainCommand

    data object StopRequested : MainCommand
}
