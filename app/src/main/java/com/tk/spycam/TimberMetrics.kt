package com.tk.spycam

import timber.log.Timber

// No analytics SDK is configured; metrics are emitted as structured log lines until one is adopted.
class TimberMetrics : Metrics {
    override fun count(
        name: String,
        vararg tags: Pair<String, String>,
    ) {
        val renderedTags = tags.joinToString(separator = ",") { (key, value) -> "$key=$value" }
        Timber.i("[TimberMetrics] metric: %s tags: %s", name, renderedTags)
    }
}
