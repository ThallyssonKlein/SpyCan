package com.tk.spycan

interface Metrics {
    fun count(
        name: String,
        vararg tags: Pair<String, String>,
    )
}
