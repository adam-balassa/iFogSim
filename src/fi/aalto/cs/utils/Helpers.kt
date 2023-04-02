package fi.aalto.cs.utils

import org.apache.commons.math3.distribution.PoissonDistribution
import org.fog.utils.Logger

fun enableDebugLogging() {
    Logger.ENABLED = true
}

fun enableReporting() {
    REPORTING_ENABLED = true
}

fun poissonNumberGenerator(mean: Double, scale: Double, lambda: Double = 2.5): () -> Double {
    val generator = PoissonDistribution(lambda)
    return {
        (generator.sample() - lambda) * scale + mean
    }
}
