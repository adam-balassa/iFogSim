package fi.aalto.cs.utils

import com.google.gson.GsonBuilder
import fi.aalto.cs.extensions.E2ELatencyMonitor
import fi.aalto.cs.extensions.ExecutionLevelMonitor
import fi.aalto.cs.extensions.StochasticAppEdge
import org.cloudbus.cloudsim.Pe
import org.fog.application.AppLoop
import org.fog.application.Application
import org.fog.entities.Actuator
import org.fog.entities.FogDevice
import org.fog.entities.MicroserviceFogDevice
import org.fog.entities.Sensor
import org.fog.utils.Config
import org.fog.utils.MigrationDelayMonitor
import org.fog.utils.NetworkUsageMonitor
import org.fog.utils.TimeKeeper
import org.fog.utils.distribution.DeterministicDistribution
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

var REPORTING_ENABLED = false

inline fun <reified Config> reportSimulation(simulation: Simulation<Config>, rootDirectory: String) {
    if (!REPORTING_ENABLED) {
        return
    }
    val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").run {
        format(simulation.now.time)
    }
    val simulationResultDirectory = Path(rootDirectory, simulation.name, formattedTime)
    simulationResultDirectory.createDirectories()

    val gson = GsonBuilder().setPrettyPrinting().create()

    val setupFile = simulationResultDirectory.resolve("setup.json")
    setupFile.createFile().writeText(gson.toJson(reportSimulationSetup(simulation)))

    val resultFile = simulationResultDirectory.resolve("results.json")
    resultFile.createFile().writeText(gson.toJson(reportSimulationResults(simulation)))
}

fun <T> reportSimulationResults(simulation: Simulation<T>): Map<String, Any> {
    val timeKeeper = TimeKeeper.getInstance()
    val executionTime = Calendar.getInstance().timeInMillis - timeKeeper.simulationStartTime
    val appLoopLatencies = getAppLoopLatencies(simulation.workload.values.first().application.loops, timeKeeper)
    val tupleExecutionLatencies = getTupleExecutionLatencies(timeKeeper)
    val fogDeviceEnergyConsumptions = getFogDeviceEnergyConsumptions(simulation.network.fogDevices)
    val networkUsage = NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME
    val migrationDelay = MigrationDelayMonitor.getMigrationDelay()

    return mapOf(
        "executionTime" to executionTime,
        "networkUsage" to networkUsage,
        "migrationDelay" to migrationDelay,
        "appLoopLatencies" to appLoopLatencies,
        "tupleExecutionLatencies" to tupleExecutionLatencies,
        "fogDeviceEnergyConsumptions" to fogDeviceEnergyConsumptions,
        "executionLevels" to ExecutionLevelMonitor.tupleTypeToLatencyMap,
    )
}

fun getAppLoopLatencies(appLoops: List<AppLoop>, timeKeeper: TimeKeeper) =
    appLoops.map { appLoop ->
        mapOf(
            "appLoop" to appLoop.modules,
            "avgLatency" to timeKeeper.loopIdToCurrentAverage[appLoop.loopId],
            "latencies" to E2ELatencyMonitor.loopIdToLatencies[appLoop.loopId]
        )
    }

fun getTupleExecutionLatencies(timeKeeper: TimeKeeper) =
    timeKeeper.tupleTypeToAverageCpuTime.map { (tupleType, avgCpuTime) ->
        mapOf(
            "tuple" to tupleType,
            "cpuTime" to avgCpuTime
        )
    }

fun getFogDeviceEnergyConsumptions(fogDevices: Map<String, List<FogDevice>>) =
    fogDevices.flatMap { (groupId, devices) ->
        devices.map {
            mapOf(
                "group" to groupId,
                "name" to it.name,
                "energy" to it.energyConsumption
            )
        }
    }

inline fun <reified Config> reportSimulationSetup(simulation: Simulation<Config>): Map<String, Any> {
    val config = getGlobalSettings(simulation.config)
    val fogDeviceConfigs = getFogDeviceConfigs(simulation.network.fogDevices)
    val networkConfig = getNetworkConfig(simulation.network.fogDevices)
    val sensorConfigs = getSensorConfigs(simulation.workload.values.first().sensors)
    val actuatorConfigs = getActuatorConfigs(simulation.workload.values.first().actuators)
    val applicationConfig = getApplicationConfig(simulation.workload.values.first().application)

    return listOfNotNull(
        "config" to config,
        "network" to networkConfig,
        "fogDevices" to fogDeviceConfigs,
        "sensors" to sensorConfigs,
        "actuators" to actuatorConfigs,
        "application" to applicationConfig,
        if (StochasticAppEdge.tupleTypeToCpuLength.isNotEmpty()) {
            "tupleTypeToCpuLength" to StochasticAppEdge.tupleTypeToCpuLength
        } else {
            null
        }
    ).toMap()
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

fun getNetworkConfig(fogDevices: Map<String, List<FogDevice>>): List<Any> {
    val devicesMap = fogDevices
        .flatMap { it.value }
        .associateBy { it.id }
        .toMutableMap()
    val deviceToClusterId = mutableMapOf<Int, Int>()
    var clusterId = 0
    while (devicesMap.isNotEmpty()) {
        val device = devicesMap.values.first()
        if (device.clusterMembers.isEmpty()) {
            deviceToClusterId[device.id] = -1
        } else {
            deviceToClusterId[device.id] = clusterId
            device.clusterMembers.forEach {
                deviceToClusterId[it] = clusterId
                devicesMap.remove(it)
            }
            ++clusterId
        }
        devicesMap.remove(device.id)
    }
    return fogDevices.flatMap { (groupId, devices) ->
        devices.map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "parent" to it.parentId,
                "level" to it.level,
                "group" to groupId,
                "cluster" to deviceToClusterId[it.id]
            )
        }
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
        (device as? MicroserviceFogDevice)?.let { "microservicesFogDeviceType" to it.deviceType },
        (device as? MicroserviceFogDevice)?.let { "broadcast" to it.broadcastResults }
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
