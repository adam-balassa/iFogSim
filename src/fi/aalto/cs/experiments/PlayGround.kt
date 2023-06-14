package fi.aalto.cs.experiments

import org.apache.commons.math3.random.EmpiricalDistribution

fun main() {
    val distro = EmpiricalDistribution().apply { load(doubleArrayOf(10.0, 10.0, 20.0, 20.9)) }
    repeat(10) { println(distro.nextValue) }
}
