package fi.aalto.cs.experiments

import fi.aalto.cs.utils.poissonNumberGenerator

fun main() {
    val generator = poissonNumberGenerator(4000.0)
    println(
        (0..100)
            .map { generator() }
            .groupBy { it }
            .map { (value, count) -> value to count.size }
            .sortedBy { (key, _) -> key }
    )
}
