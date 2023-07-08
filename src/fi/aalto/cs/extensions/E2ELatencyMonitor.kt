package fi.aalto.cs.extensions

object E2ELatencyMonitor : Monitor {
    val loopIdToLatencies = mutableMapOf<Int, MutableList<Double>>()

    fun registerE2ELatency(loopId: Int, latency: Double) {
        if (loopIdToLatencies.containsKey(loopId)) {
            loopIdToLatencies[loopId]!!.add(latency)
        } else {
            loopIdToLatencies[loopId] = mutableListOf(latency)
        }
    }

    override fun clear() {
        loopIdToLatencies.clear()
    }
}
