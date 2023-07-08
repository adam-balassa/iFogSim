package fi.aalto.cs.utils

import fi.aalto.cs.extensions.BandwidthMonitor
import fi.aalto.cs.extensions.E2ELatencyMonitor
import fi.aalto.cs.extensions.ExecutionLevelMonitor
import fi.aalto.cs.extensions.Monitor
import org.fog.utils.Logger
import org.fog.utils.MigrationDelayMonitor
import org.fog.utils.NetworkUsageMonitor
import org.fog.utils.TimeKeeper

fun enableDebugLogging() {
    Logger.ENABLED = true
}

fun enableReporting() {
    REPORTING_ENABLED = true
}

fun <K, V> MutableMap<K, MutableList<V>>.add(key: K, value: V) {
    this[key]?.run { add(value); true } ?: let {
        this[key] = mutableListOf(value)
    }
}

fun <K1, K2, V> MutableMap<K1, MutableMap<K2, V>>.add(key1: K1, key2: K2, value: V) {
    this[key1]?.run { put(key2, value); true } ?: let {
        this[key1] = mutableMapOf(key2 to value)
    }
}

fun clearMonitors() {
    val monitors = listOf<Monitor>(
        BandwidthMonitor,
        E2ELatencyMonitor,
        ExecutionLevelMonitor,
        TimeKeeper.getInstance(),
        MigrationDelayMonitor(),
        NetworkUsageMonitor()
    )

    monitors.forEach { it.clear() }
}
