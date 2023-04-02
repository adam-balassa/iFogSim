package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel

object ExecutionLevelMonitor {
    val tupleTypeToLatencyMap = mutableMapOf<String, MutableList<String>>()
    fun registerTupleExecution(tupleType: String, levelId: Int) {
        FogDeviceLevel.byId(levelId)?.name?.let { level ->
            if (tupleTypeToLatencyMap.containsKey(tupleType)) {
                tupleTypeToLatencyMap[tupleType]!!.add(level)
            } else {
                tupleTypeToLatencyMap[tupleType] = mutableListOf(level)
            }
        }
    }
}
