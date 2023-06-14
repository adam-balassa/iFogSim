package fi.aalto.cs.utils

import org.fog.utils.Logger

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
