package fi.aalto.cs.utils

import com.google.gson.GsonBuilder
import org.cloudbus.cloudsim.Pe
import org.fog.application.Application
import org.fog.entities.Actuator
import org.fog.entities.FogDevice
import org.fog.entities.MicroserviceFogDevice
import org.fog.entities.Sensor
import org.fog.utils.distribution.DeterministicDistribution
import java.text.SimpleDateFormat
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

inline fun <reified Config> reportSimulation(simulation: Simulation<Config>, rootDirectory: String) {
    val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").run {
        format(simulation.now.time)
    }
    val simulationResultDirectory = Path(rootDirectory, simulation.name, formattedTime)
    simulationResultDirectory.createDirectories()

    val setupFile = simulationResultDirectory.resolve("setup.json")
    setupFile.createFile().writeText(reportSimulationSetup(simulation))
}

inline fun <reified Config> reportSimulationSetup(simulation: Simulation<Config>): String {
    val config = getGlobalSettings(simulation.config)
    val fogDeviceConfigs = getFogDeviceConfigs(simulation.environment.fogDevices)
    val networkConfig = getNetworkConfig(simulation.environment.fogDevices)
    val sensorConfigs = getSensorConfigs(simulation.environment.sensors)
    val actuatorConfigs = getActuatorConfigs(simulation.environment.actuators)
    val applicationConfig = getApplicationConfig(simulation.app)

    val simulationSetup = mapOf(
        "config" to config,
        "network" to networkConfig,
        "fogDevices" to fogDeviceConfigs,
        "sensors" to sensorConfigs,
        "actuators" to actuatorConfigs,
        "application" to applicationConfig
    )

    return GsonBuilder().setPrettyPrinting().create().run {
        toJson(simulationSetup)
    }
}

inline fun <reified T> getGlobalSettings(settings: T): Map<String, Any?> {
    val type = T::class
    return mutableMapOf<String, Any>().apply {
        type.members.forEach {
            if (it.parameters.size == 1 && it.name !in listOf("hashCode", "toString")) {
                it.call(settings)?.let { value ->
                    put(it.name, value)
                }
            }
        }
    }
}

fun getSensorConfigs(sensors: List<Sensor>) =
    sensors.groupBy { it.tupleType }.map { (tuple, sensors) ->
        val sensor = sensors.first()
        listOfNotNull(
            "tuple" to tuple,
            "numberOfSensors" to sensors.size,
            "latency" to sensor.latency,
            "distributionType" to sensor.transmitDistribution::class.simpleName,
            (sensor.transmitDistribution as? DeterministicDistribution)?.let {
                "deterministicDistributionFactor" to it.value
            }
        ).toMap()
    }

fun getActuatorConfigs(actuators: List<Actuator>) =
    actuators.groupBy { it.actuatorType }.map { (type, actuators) ->
        val actuator = actuators.first()
        listOfNotNull(
            "type" to type,
            "numberOfActuators" to actuators.size,
            "latency" to actuator.latency
        ).toMap()
    }

fun getNetworkConfig(fogDevices: MutableMap<String, MutableList<FogDevice>>) =
    fogDevices.flatMap { (groupId, devices) ->
        devices.map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "parent" to it.parentId
                "level" to it.level,
                "group" to groupId,
            )
        }
    }

fun getFogDeviceConfigs(fogDevices: Map<String, List<FogDevice>>) =
    fogDevices.map { (levelId, devices) ->
        getConfigForFogDevice(levelId, devices.first()).apply {
            put("numberOfDevices", devices.size)
        }
    }

fun getConfigForFogDevice(levelId: String, device: FogDevice): MutableMap<String, Any> =
    listOfNotNull(
        "level" to levelId,
        "mips" to device.host.getPeList<Pe>().first().mips,
        "ram" to device.host.ram,
        "level" to FogDeviceLevel.values()[device.level],
        "storage" to device.host.storage,
        "bandwidth" to device.host.bw,
        "uplinkBandwidth" to device.uplinkBandwidth,
        "downlinkBandwidth" to device.downlinkBandwidth,
        "clusterLinkBandwidth" to device.clusterLinkBandwidth,
        "uplinkLatency" to device.uplinkLatency,
        "schedulingInterval" to device.schedulingInterval,
        "busyPower" to device.host.powerModel.getPower(1.0),
        "idlePower" to device.host.powerModel.getPower(0.0),
        "costRatePerMips" to device.ratePerMips,
        "costRatePerSecond" to device.characteristics.costPerSecond,
        "costRatePerMemory" to device.characteristics.costPerMem,
        "costRatePerBandwidth" to device.characteristics.costPerBw,
        "costRatePerStorage" to device.characteristics.costPerStorage,
        (device as? MicroserviceFogDevice)?.let { "microservicesFogDeviceType" to it.deviceType }
    ).toMap().toMutableMap()

fun getApplicationConfig(app: Application) =
    mapOf(
        "modules" to app.modules.map {
            mapOf(
                "name" to it.name,
                "mips" to it.mips,
                "ram" to it.ram,
                "storage" to it.size,
                "bandwidth" to it.bw
            )
        },
        "edges" to app.edges.map {
            mapOf(
                "from" to it.source,
                "to" to it.destination,
                "tuple" to it.tupleType,
                "direction" to (it.direction == TupleDirection.Up.id),
                "cpuLength" to it.tupleCpuLength,
                "dataSize" to it.tupleNwLength
            )
        }
    )
