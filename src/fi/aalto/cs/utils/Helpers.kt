package fi.aalto.cs.utils

import org.apache.commons.math3.distribution.PoissonDistribution
import org.fog.utils.Logger
import kotlin.math.roundToInt

fun enableDebugLogging() {
    Logger.ENABLED = true
}

fun enableReporting() {
    REPORTING_ENABLED = true
}

fun poissonNumberGenerator(mean: Double, scale: Double = mean * 0.1, lambda: Double = 2.5): () -> Double {
    val generator = PoissonDistribution(lambda)
    return {
        (generator.sample() - lambda) * scale + mean
    }
}

fun poissonNumber(mean: Double, scale: Double = mean * 0.1, lambda: Double = 2.5): Double =
    PoissonDistribution(lambda).run {
        (sample() - lambda) * scale + mean
    }

fun poissonNumber(mean: Int, scale: Int = mean / 10, lambda: Double = 2.5): Int =
    PoissonDistribution(lambda).run {
        (sample() - lambda.roundToInt()) * scale + mean
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