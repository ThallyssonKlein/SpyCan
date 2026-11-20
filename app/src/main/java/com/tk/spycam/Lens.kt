package com.tk.spycam

enum class Lens {
    BACK,
    FRONT,
    ;

    companion object {
        fun fromExtra(value: String?): Lens = entries.firstOrNull { it.name == value } ?: BACK
    }
}
