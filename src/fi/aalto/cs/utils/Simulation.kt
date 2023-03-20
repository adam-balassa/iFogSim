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
    val user: FogBroker
    val environment: SimulationEnvironment
    val app: Application
    val now: Calendar = Calendar.getInstance()

    init {
        Log.disable()
        CloudSim.init(1, now, false)
        user = FogBroker("broker")
        environment = SimulationEnvironment()
        app = createApplication(name, user.id)
    }

    fun run() {
        TimeKeeper.getInstance().simulationStartTime = now.timeInMillis
        CloudSim.startSimulation()
        CloudSim.stopSimulation()
    }
}

class SimulationEnvironment {
    val locator = LocationHandler(DataParser())
    val fogDevices = mutableMapOf<String, MutableList<FogDevice>>()
    val sensors = mutableListOf<Sensor>()
    val actuators = mutableListOf<Actuator>()

    fun add(device: FogDevice) {
        fogDevices["generic"]?.add(device)
            ?: let { fogDevices["generic"] = mutableListOf(device) }
    }
    fun add(actuator: Actuator) = actuators.add(actuator)
    fun add(sensor: Sensor) = sensors.add(sensor)
}
