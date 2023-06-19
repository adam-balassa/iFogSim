package fi.aalto.cs.utils

import org.apache.commons.math3.distribution.PoissonDistribution
import java.util.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt

fun <T> List<T>.sample(n: Int, replace: Boolean = n > size): List<T> {
    val indices =
        if (replace) {
            (0 until n).map { nextInt(size) }.toList()
        } else {
            val remainingIndices = indices.toSet().toMutableSet()
            val chosenIndices = mutableSetOf<Int>()
            repeat(n) {
                val index = remainingIndices.random()
                remainingIndices.remove(index)
                chosenIndices.add(index)
            }
            chosenIndices
        }
    return indices.map { this[it] }
}
fun uniform(a: Double = 0.0, b: Double = 1.0) = a + nextDouble() * (b - a)

fun normal(mu: Double = 0.0, sigma: Double = 1.0): Double = mu + sigma * Random().nextGaussian()

fun logNormal(mean: Double, sigma: Double = 0.5, mu: Double = ln(mean / 7.0)): Double {
    val logNormalMean = exp(mu + sigma * sigma / 2)
    return mean - logNormalMean + exp(normal(mu, sigma))
}

fun poisson(mean: Double, scale: Double = mean * 0.1, lambda: Double = 2.5): Double =
    PoissonDistribution(lambda).run {
        (sample() - lambda) * scale + mean
    }
