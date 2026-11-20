package com.tk.spycam

interface Metrics {
    fun count(
        name: String,
        vararg tags: Pair<String, String>,
    )
}
