package fi.aalto.cs.utils

import org.apache.commons.math3.distribution.PoissonDistribution
import org.fog.utils.distribution.UniformDistribution
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextInt

fun uniformNumberGenerator(a: Double, b: Double): () -> Double {
    val generator = UniformDistribution(a, b)
    return { generator.nextValue }
}

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
