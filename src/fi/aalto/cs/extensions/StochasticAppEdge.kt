package fi.aalto.cs.extensions

import org.fog.application.AppEdge

class StochasticAppEdge(
    source: String,
    destination: String,
    private val tupleCpuLengthGenerator: () -> Double,
    private val tupleNwLengthGenerator: () -> Double,
    tupleType: String,
    direction: Int,
    edgeType: Int,
    periodicity: Double? = null,
) : AppEdge(source, destination, tupleCpuLengthGenerator(), tupleNwLengthGenerator(), tupleType, direction, edgeType) {
    init {
        periodicity?.let {
            this.periodicity = it
            this.isPeriodic = true
        }
    }
    override fun getTupleCpuLength() = tupleCpuLengthGenerator().also {
        registerCpuLengthGeneration(tupleType, it)
    }
    override fun getTupleNwLength() = tupleNwLengthGenerator()

    companion object {
        val tupleTypeToCpuLength: MutableMap<String, MutableList<Double>> = mutableMapOf()
        private fun registerCpuLengthGeneration(tupleType: String, cpuLength: Double) {
            tupleTypeToCpuLength[tupleType]?.add(cpuLength) ?: run {
                tupleTypeToCpuLength[tupleType] = mutableListOf(cpuLength)
            }
        }
    }
}
