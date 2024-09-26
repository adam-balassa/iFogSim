package fi.aalto.cs.utils

import com.google.gson.GsonBuilder
import fi.aalto.cs.extensions.*
import org.cloudbus.cloudsim.Pe
import org.fog.application.Application
import org.fog.entities.Actuator
import org.fog.entities.FogDevice
import org.fog.entities.MicroserviceFogDevice
import org.fog.entities.Sensor
import org.fog.placement.LocationHandler
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
    val appLoopLatencies = getAppLoopLatencies(simulation.workload.values, timeKeeper)
    val tupleExecutionLatencies = getTupleExecutionLatencies(simulation.workload.values, timeKeeper)
    val fogDeviceEnergyConsumptions = getFogDeviceEnergyConsumptions(simulation.network.fogDevices)
    val networkUsage = NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME
    val migrationDelay = MigrationDelayMonitor.getMigrationDelay()
//    val executionLevels = getExecutionLevels(simulation.workload.values)
    val waitingTuples = getWaitingTuples(BandwidthMonitor.waitingTuples)
    val executedTuples = getExecutedTuples(TupleExecutionTimeMonitor.executedTuples)

    return mapOf(
        "executionTime" to executionTime,
        "networkUsage" to networkUsage,
        "migrationDelay" to migrationDelay,
        "appLoopLatencies" to appLoopLatencies,
        "tupleExecutionLatencies" to tupleExecutionLatencies,
        "fogDeviceEnergyConsumptions" to fogDeviceEnergyConsumptions,
//        "executionLevels" to executionLevels,
        "waitingTuples" to waitingTuples,
        "executedTuples" to executedTuples,
    )
}

private fun getWaitingTuples(waitingTuples: List<WaitingTuple>): Map<String, Map<Any, Double>> =
    mapOf(
        "byTupleType" to waitingTuples.groupBy { it.tupleType }.mapValues { (_, values) -> values.sumOf { it.waitTime } / values.size }.toMap(),
        "byDeviceId" to waitingTuples.groupBy { it.deviceId }.mapValues { (_, values) -> values.sumOf { it.waitTime } / values.size }.toMap(),
        "byLevel" to waitingTuples.groupBy { it.level }.mapValues { (_, values) -> values.sumOf { it.waitTime } / values.size }.toMap(),
        "byDirection" to waitingTuples.groupBy { it.uplink }.mapValues { (_, values) -> values.sumOf { it.waitTime } / values.size }.toMap()
    )

private fun getExecutedTuples(executedTuples: List<ExecutingTuple>): Map<String, Map<String, Double>> =
    executedTuples
        .groupBy { it.tupleType }
        .mapValues { (_, tuples) -> tuples
            .groupBy { it.level }
            .mapValues { (_, tuples) -> tuples.sumOf { it.executionTime } / tuples.size }
        }
        .toMap()

private fun getExecutionLevels(workloads: Collection<Workload>): Map<String, List<List<Any>>> {
    val applications = workloads.groupBy { it.name }
    return ExecutionLevelMonitor.tupleTypeToExecutionLevel.entries.groupBy { (tupleType) ->
        val app = applications.keys.find { tupleType.startsWith(it) && "-" in tupleType.substring(it.length) }
        app ?: ""
    }.flatMap { (_, tuples) ->
        tuples
            .groupBy { it.key.split("-").last() }
            .entries.map { (tupleType, executionLevels) ->
                tupleType to executionLevels.flatMap { measurements ->
                    measurements.value.map { listOf(it.first, it.second) }
                }
            }
    }.toMap()
}

fun getAppLoopLatencies(workloads: Collection<Workload>, timeKeeper: TimeKeeper): List<Map<String, Any?>> {
    val applications = workloads.groupBy { it.name }
    return applications.values.flatMap { apps ->
        apps.first().application.loops.indices.map { i ->
            val loops = apps.map { it.application.loops[i] }
            mapOf(
                "appLoop" to loops[0].modules,
                "avgLatency" to loops.sumOf { timeKeeper.loopIdToCurrentAverage[it.loopId] ?: 0.0 } / loops.size,
                "latencies" to loops.flatMap { E2ELatencyMonitor.loopIdToLatencies[it.loopId] ?: emptyList() }
            )
        }
    }
}

fun getTupleExecutionLatencies(workloads: Collection<Workload>, timeKeeper: TimeKeeper): List<Map<String, Any>> {
    val applications = workloads.groupBy { it.name }
    return timeKeeper.tupleTypeToAverageCpuTime.entries.groupBy { (tupleType) ->
        val app = applications.keys.find { tupleType.startsWith(it) && "-" in tupleType.substring(it.length) }
        app ?: ""
    }.flatMap { (_, tuples) ->
        tuples
            .groupBy { it.key.split("-").last() }
            .entries.map { (tupleType, avgCpuTimes) ->
                mapOf(
                    "tuple" to tupleType,
                    "cpuTime" to avgCpuTimes.sumOf { it.value } / avgCpuTimes.size
                )
            }
    }
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
    val networkConfig = getNetworkConfig(simulation.network.fogDevices, simulation.network.locator)
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
//        if (StochasticAppEdge.tupleTypeToCpuLength.isNotEmpty()) {
//            "tupleTypeToCpuLength" to StochasticAppEdge.tupleTypeToCpuLength
//        } else {
//            null
//        }
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
            },
            "FPS" to 1000 / sensor.transmitDistribution.meanInterTransmitTime
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

fun getNetworkConfig(fogDevices: Map<String, List<FogDevice>>, locator: LocationHandler): List<Any> {
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
            listOfNotNull(
                "id" to it.id,
                "name" to it.name,
                "parent" to it.parentId,
                "level" to it.level,
                "group" to groupId,
                "cluster" to deviceToClusterId[it.id],
                run {
                    val deviceId = locator.getDataIdByInstanceID(it.id)
                    val location = locator.dataObject.usersLocation[deviceId]?.values?.last()
                        ?: locator.dataObject.resourceLocationData[deviceId]
                    location?.let {
                        "location" to mapOf(
                            "lat" to it.latitude,
                            "lng" to it.longitude
                        )
                    }
                }
            ).toMap()
        }
    }
}

fun getFogDeviceConfigs(fogDevices: Map<String, List<FogDevice>>) =
    fogDevices.map { (typeId, devices) ->
        getConfigForFogDevice(typeId, devices.first()).apply {
            put("numberOfDevices", devices.size)
        }
    }

fun getConfigForFogDevice(typeId: String, device: FogDevice): MutableMap<String, Any> =
    listOfNotNull(
        "type" to typeId,
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
        (device as? MicroserviceFogDevice)?.let { "broadcast" to it.broadcastResults },
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
