package fi.aalto.cs.experiments

import fi.aalto.cs.utils.logNormal
import kotlin.math.roundToInt

fun main() {
    println(hist((0..100).map { logNormal(1000.0) }, 10))
}

fun hist(arr: List<Double>, bins: Int = 10): List<Pair<Int, Int>> {
    val min = arr.min()
    val max = arr.max()
    val partitions = (0 until bins).map { min + it * (max - min) / (bins - 1) }
    val hist = List(bins) { i -> arr.count { el -> partitions.indexOfLast { p -> p <= el } == i } }
    return partitions.map { it.roundToInt() }.zip(hist)
}
