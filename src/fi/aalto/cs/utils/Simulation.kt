package fi.aalto.cs.utils

import org.cloudbus.cloudsim.Log
import org.cloudbus.cloudsim.core.CloudSim
import org.fog.application.Application
import org.fog.application.Application.createApplication
import org.fog.entities.Actuator
import org.fog.entities.FogBroker
import org.fog.entities.FogDevice
import org.fog.entities.Sensor
import org.fog.mobilitydata.DataParser
import org.fog.placement.LocationHandler
import org.fog.utils.TimeKeeper
import java.util.*

class Simulation<Config>(
    val name: String,
    val config: Config,
) {
    val now: Calendar = Calendar.getInstance()

    val network = Network()
    val workload = mutableMapOf<String, Workload>()

    init {
        Log.disable()
        CloudSim.init(1, now, false)
    }

    fun run() {
        TimeKeeper.getInstance().simulationStartTime = now.timeInMillis
        CloudSim.startSimulation()
        CloudSim.stopSimulation()
    }
}

class Workload(val id: String, val name: String) {
    val user: FogBroker = FogBroker("broker-$name$id")
    val application: Application = createApplication("$name$id", user.id)

    // Sensors and actuators are modeled as both physical devices and as parts of the application
    // They added as part of the workload (instead of the network), since their instances are application specific
    val sensors = mutableListOf<Sensor>()
    val actuators = mutableListOf<Actuator>()
}

class Network {
    val locator = LocationHandler(DataParser())
    val fogDevices = mutableMapOf<String, MutableList<FogDevice>>()

    fun add(device: FogDevice) {
        fogDevices["generic"]?.add(device)
            ?: let { fogDevices["generic"] = mutableListOf(device) }
    }
}
