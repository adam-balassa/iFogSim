package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel
import org.cloudbus.cloudsim.core.CloudSim.clock
import org.fog.entities.FogDevice
import org.fog.entities.Tuple

data class ExecutingTuple(
    val deviceId: Int,
    val level: String,
    val tupleType: String,
    val executionStart: Double,
    var executionEnd: Double = 0.0
) {
    val executionTime get() = executionEnd - executionStart
}

object TupleExecutionTimeMonitor : Monitor {
    private val executingTupleMap = mutableMapOf<Int, ExecutingTuple>()
    val executedTuples get() = executingTupleMap.values.filter { it.executionEnd > 0 }.toList()

    fun executionStart(tuple: Tuple, device: FogDevice) {
        executingTupleMap[tuple.cloudletId] = ExecutingTuple(device.id, FogDeviceLevel.byId(device.level)!!.name, tuple.tupleType, clock())
    }

    fun executionEnd(tuple: Tuple) {
        if (executingTupleMap.containsKey(tuple.cloudletId)) {
            executingTupleMap[tuple.cloudletId]!!.executionEnd = clock()
        }
    }

    override fun clear() {
        executingTupleMap.clear()
    }
}
