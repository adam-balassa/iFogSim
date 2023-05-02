package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel

object ExecutionLevelMonitor {
    val tupleTypeToExecutionLevel = mutableMapOf<String, MutableList<String>>()
    fun registerTupleExecution(tupleType: String, levelId: Int) {
        FogDeviceLevel.byId(levelId)?.name?.let { level ->
            if (tupleTypeToExecutionLevel.containsKey(tupleType)) {
                tupleTypeToExecutionLevel[tupleType]!!.add(level)
            } else {
                tupleTypeToExecutionLevel[tupleType] = mutableListOf(level)
            }
        }
    }
}
