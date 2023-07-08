package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel
import org.cloudbus.cloudsim.core.CloudSim

object ExecutionLevelMonitor : Monitor {
    val tupleTypeToExecutionLevel = mutableMapOf<String, MutableList<Pair<String, Double>>>()
    fun registerTupleExecution(tupleType: String, levelId: Int) {
        val time = CloudSim.clock()
        FogDeviceLevel.byId(levelId)?.name?.let { level ->
            if (tupleTypeToExecutionLevel.containsKey(tupleType)) {
                tupleTypeToExecutionLevel[tupleType]!!.add(level to time)
            } else {
                tupleTypeToExecutionLevel[tupleType] = mutableListOf(level to time)
            }
        }
    }

    override fun clear() {
        tupleTypeToExecutionLevel.clear()
    }
}
