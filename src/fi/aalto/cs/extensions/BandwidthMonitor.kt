package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel
import org.cloudbus.cloudsim.core.CloudSim.clock
import org.fog.entities.FogDevice
import org.fog.entities.Tuple

data class WaitingTuple(
    val deviceId: Int,
    val level: String,
    val tupleType: String,
    val uplink: Boolean,
    val waitStart: Double,
    var waitEnd: Double = 0.0
) {
    val waitTime get() = waitEnd - waitStart
}

object BandwidthMonitor : Monitor {
    private val waitingTupleMap = mutableMapOf<Int, WaitingTuple>()
    val waitingTuples get() = waitingTupleMap.values.filter { it.waitEnd > 0 }.toList()

    fun startWait(tuple: Tuple, device: FogDevice, uplink: Boolean) {
        waitingTupleMap[tuple.cloudletId] = WaitingTuple(device.id, FogDeviceLevel.byId(device.level)!!.name, tuple.tupleType, uplink, clock())
    }

    fun send(tuple: Tuple, device: FogDevice, uplink: Boolean, delay: Double) {
        if (waitingTupleMap.containsKey(tuple.cloudletId)) {
            waitingTupleMap[tuple.cloudletId]!!.waitEnd = clock() + delay
        } else {
            waitingTupleMap[tuple.cloudletId] = WaitingTuple(device.id, FogDeviceLevel.byId(device.level)!!.name, tuple.tupleType, uplink, clock(), clock() + delay)
        }
    }

    override fun clear() {
        waitingTupleMap.clear()
    }
}
