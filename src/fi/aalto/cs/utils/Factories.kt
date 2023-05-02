package fi.aalto.cs.utils

import fi.aalto.cs.extensions.StochasticAppEdge
import fi.aalto.cs.utils.MicroservicePlacementStrategy.ClusteredPlacement
import org.cloudbus.cloudsim.Pe
import org.cloudbus.cloudsim.power.PowerHost
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking
import org.fog.application.AppEdge
import org.fog.application.AppLoop
import org.fog.application.AppModule
import org.fog.application.selectivity.FractionalSelectivity
import org.fog.application.selectivity.SelectivityModel
import org.fog.entities.*
import org.fog.mobilitydata.References.NOT_SET
import org.fog.placement.*
import org.fog.policy.AppModuleAllocationPolicy
import org.fog.scheduler.StreamOperatorScheduler
import org.fog.scheduler.TupleScheduler
import org.fog.utils.FogLinearPowerModel
import org.fog.utils.FogUtils.generateEntityId
import org.fog.utils.distribution.Distribution
import kotlin.Pair
import kotlin.apply
import org.apache.commons.math3.util.Pair as ApachePair

fun forwarding(mapping: Pair<TupleType, TupleType>, probability: Double = 1.0) =
    mapping to FractionalSelectivity(probability)

fun <T> Simulation<T>.addApplication(
    id: String = "${this.workload.size + 1}",
    name: String = (this.name.split(' ').joinToString { "-" } + "-" + id),
) = Workload(name).also { workload[id] = it }

/**
 * Add an app module to the application
 * @param module Name of the `AppModule`
 * @param mips CPU resource consumption in Million instructions per second
 * @param ram Memory consumption in MB
 * @param storage Storage consumption in MB
 * @param bandwidth Bandwidth capacity to allocate in Mbps
 * @param selectivityMapping Forwarding probabilistic model from input tuples to output tuples
 */
fun Workload.addAppModule(
    module: ModuleType,
    mips: Double = 1000.0,
    ram: Int = 10,
    storage: Long = 10_000,
    bandwidth: Long = 1000,
    selectivityMapping: Map<Pair<TupleType, TupleType>, SelectivityModel> = mutableMapOf(),
) = AppModule(
    generateEntityId(),
    module.name,
    application.appId,
    user.id,
    mips,
    ram,
    bandwidth,
    storage,
    "Xen",
    TupleScheduler(mips, 1),
    mutableMapOf<ApachePair<String, String>, SelectivityModel?>().apply {
        selectivityMapping.forEach { (t1, t2), model ->
            put(ApachePair(t1.name, t2.name), model)
        }
    },
).also {
    application.modules.add(it)
}

/**
 * Add an app edge between two app modules to the application
 * @param source Source AppModule
 * @param destination Destination AppModule
 * @param tupleType The tuple's name that travels on the AppEdge
 * @param direction Up/Down/Actuator: which port should a FogDevice use to forward the tuple
 * @param cpuLength CPU power needed to process the tuple in Million instructions
 * @param cpuLengthGenerator Indicates that the tuple's CPU-length should be a stochastic value determined by the generator
 * @param dataSize The size of the tuple in MB
 * @param dataSizeGenerator Indicates that the tuple's data size should be a stochastic value determined by the generator
 * @param appEdgeType Indicates whether the edge is between modules or sensors or actuators
 */
fun Workload.addAppEdge(
    source: ModuleType,
    destination: ModuleType,
    tupleType: TupleType,
    direction: TupleDirection,
    cpuLength: Double? = null,
    cpuLengthGenerator: (() -> Double)? = null,
    dataSize: Double = 500.0,
    dataSizeGenerator: (() -> Double)? = null,
    appEdgeType: AppEdgeType = AppEdgeType.InterModule,
): AppEdge {
    val edge = if (cpuLengthGenerator != null) {
        StochasticAppEdge(
            source.name,
            destination.name,
            cpuLengthGenerator,
            dataSizeGenerator ?: { dataSize },
            tupleType.name,
            direction.id,
            appEdgeType.id
        )
    } else if (cpuLength != null) {
        AppEdge(source.name, destination.name, cpuLength, dataSize, tupleType.name, direction.id, appEdgeType.id)
    } else {
        throw IllegalStateException("Either parameters cpuLengthGenerator or cpuLength are mandatory")
    }
    return edge.also {
        application.edges.add(it)
        application.edgeMap[tupleType.name] = edge
    }
}

/**
 * Constructs a FogDevice and adds it to the simulation environment
 * @param type Name of the fog device is derived from the type
 * @param mips CPU frequency in Million instructions per second
 * @param ram Memory capacity of the device in MB
 * @param parentId The parent fog device in the network topology tree
 * @param level Indicates the level of the fog device in the network topology tree
 * @param storage Storage capacity of the device in MB
 * @param uplinkBandwidth Bandwidth of the up-link port of the device in Mbps
 * @param downlinkBandwidth Bandwidth of the down-link port of the device in Mbps
 * @param clusterLinkBandwidth Bandwidth between cluster nodes in Mbps
 * @param uplinkLatency Latency of the up-link connection in milliseconds
 * @param schedulingInterval How often should the VM scheduling logic run in the host (ms)
 * @param busyPower Power consumption when the device is performing computation in MJ/s
 * @param idlePower Power consumption when the device is idle in MJ/s
 * @param costRatePerMips The cost of consuming 1 million instructions per second
 * @param costRatePerSecond The cost of running the host for 1 second
 * @param costRatePerBandwidth The cost of 1 Mbps
 * @param costRatePerStorage The cost of 1MB storage
 * @param microservicesFogDeviceType Indicates that the fog device is a MicroserviceFogDevice and determines its function in the microservices infrastructure
 */
fun <T> Simulation<T>.addFogDevice(
    type: FogDeviceType,
    mips: Long, // million CPU instructions/s
    ram: Int, // MB
    parentId: Int? = null,
    level: FogDeviceLevel = FogDeviceLevel.Proxy,
    storage: Long = 1_000_000, // MB
    bandwidth: Long = 10_000,
    uplinkBandwidth: Long = 10_000,
    downlinkBandwidth: Long = 10_000,
    clusterLinkBandwidth: Long = 10_000,
    uplinkLatency: Double = 0.0, // ms
    schedulingInterval: Double = 10.0,
    busyPower: Double = 0.0, // MJ/s
    idlePower: Double = 0.0,
    costRatePerMips: Double = 0.0, // $
    costRatePerSecond: Double = 3.0,
    costRatePerMemory: Double = 0.0,
    costRatePerBandwidth: Double = 0.0,
    costRatePerStorage: Double = 0.001,
    microservicesFogDeviceType: MicroservicesFogDeviceType? = null,
    broadcastResults: Boolean = false
): FogDevice {
    val processingElements = listOf(Pe(0, PeProvisionerOverbooking(mips.toDouble())))
    val host = PowerHost(
        generateEntityId(),
        RamProvisionerSimple(ram),
        BwProvisionerOverbooking(bandwidth),
        storage,
        processingElements,
        StreamOperatorScheduler(processingElements),
        FogLinearPowerModel(busyPower, idlePower),
    )
    val characteristics = FogDeviceCharacteristics(
        "x86", "Linux", "Xen", host,
        2.0,
        costRatePerSecond, costRatePerMemory, costRatePerStorage, costRatePerBandwidth
    )
    val deviceIndex = network.fogDevices[type.name]?.size
    val name = type.name + (deviceIndex?.let { "-$it" } ?: "")
    return if (microservicesFogDeviceType == null) {
        FogDevice(
            name,
            characteristics,
            AppModuleAllocationPolicy(listOf(host)),
            emptyList(),
            schedulingInterval,
            uplinkBandwidth.toDouble(),
            downlinkBandwidth.toDouble(),
            uplinkLatency,
            costRatePerMips
        )
    } else {
        MicroserviceFogDevice(
            name,
            characteristics,
            AppModuleAllocationPolicy(listOf(host)),
            emptyList(),
            schedulingInterval,
            uplinkBandwidth.toDouble(),
            downlinkBandwidth.toDouble(),
            clusterLinkBandwidth.toDouble(),
            uplinkLatency,
            costRatePerMips,
            microservicesFogDeviceType.typeName
        ).also {
            it.broadcastResults = broadcastResults
        }
    }.also {
        it.parentId = parentId ?: NOT_SET
        it.level = level.id
        if (deviceIndex != null) {
            network.fogDevices[type.name]?.add(it)
        } else {
            network.fogDevices[type.name] = mutableListOf(it)
        }
    }
}

/**
 * Adds an AppLoop to the application
 * @param modules the path of modules to measure for E2E latency
 */
fun Workload.addAppLoop(vararg modules: ModuleType) {
    if (application.loops == null) application.loops = mutableListOf()
    application.loops.add(AppLoop(modules.map { it.name }))
}

/**
 * Adds a sensor to the simulation environment
 * @param gateway The fog device that the sensor is attached to
 * @param tupleType The tuple that the sensor emits periodically
 * @param latency The latency between the sensor and its gateway device
 * @param emissionDistribution The probabilistic model for the delay between 2 emissions
 */
fun Workload.addSensor(
    gateway: FogDevice,
    tupleType: TupleType,
    latency: Double = 0.0,
    emissionDistribution: Distribution
) = Sensor(
    "s-${gateway.name}-${application.appId}",
    tupleType.name,
    user.id,
    application.appId,
    emissionDistribution
).also {
    it.latency = latency
    it.gatewayDeviceId = gateway.id
    it.app = application

    sensors.add(it)
}

/**
 * Adds an actuator to the simulation environment
 * @param gateway The fog device that the actuator is attached to
 * @param module The module that the actuator acts as (mostly for E2E latency measurements)
 * @param latency The latency between the fog device and the actuator
 */
fun Workload.addActuator(
    gateway: FogDevice,
    module: ModuleType,
    latency: Double = 0.0,
) = Actuator(
    "a-${gateway.name}",
    user.id,
    application.appId,
    module.name
).also {
    it.latency = latency
    it.gatewayDeviceId = gateway.id
    it.app = application

    actuators.add(it)
}

/**
 * Factory method for a client module placement request
 */
private fun Workload.placementRequest(
    module: ModuleType,
    sensor: Sensor
) = PlacementRequest(
    application.appId,
    sensor.id,
    sensor.gatewayDeviceId,
    mutableMapOf(module.name to sensor.gatewayDeviceId)
)

/**
 * Generic iFogSim controller
 * @param placementStrategy The module placement strategy
 * @param staticPlacement Determines which modules should be forced to be placed onto all of the devices for the given device type
 */
fun <T> Simulation<T>.controller(
    placementStrategy: ModulePlacementStrategy,
    staticPlacement: Map<ModuleType, FogDeviceType> = mapOf(),
) = Controller(
    "controller",
    network.fogDevices.flatMap { it.value },
    workload.values.flatMap { it.sensors },
    workload.values.flatMap { it.actuators },
).apply {
    val moduleMapping = ModuleMapping.createModuleMapping().apply {
        staticPlacement.forEach { (moduleType, fogDeviceType) ->
            network.fogDevices[fogDeviceType.name]?.forEach {
                addModuleToDevice(moduleType.name, it.name)
            }
        }
    }
    workload.values.forEach {
        submitApplication(
            it.application,
            0,
            when (placementStrategy) {
                ModulePlacementStrategy.Edgewards -> ModulePlacementEdgewards(
                    network.fogDevices.flatMap { it.value },
                    it.sensors,
                    it.actuators,
                    it.application,
                    moduleMapping
                )

                ModulePlacementStrategy.Static -> ModulePlacementMapping(
                    network.fogDevices.flatMap { it.value },
                    it.application,
                    moduleMapping
                )

                ModulePlacementStrategy.CloudOnly -> ModulePlacementOnlyCloud(
                    network.fogDevices.flatMap { it.value },
                    it.sensors,
                    it.actuators,
                    it.application
                )
            }
        )
    }
}

/**
 * Creates a MicroservicesController
 * @param clusterLevels determines which devices can manage their clusters
 * @param clusterLinkLatency the latency between any 2 devices in a cluster
 * @param placementStrategy the microservice placement strategy
 * @param clientModule which AppModule should be forced to be placed on client devices
 */
fun <T> Simulation<T>.microservicesController(
    clusterLevels: List<FogDeviceLevel> = listOf(),
    clusterLinkLatency: Int = 0,
    placementStrategy: MicroservicePlacementStrategy = ClusteredPlacement,
    clientModule: ModuleType? = null
) = MicroservicesController(
    "controller",
    network.fogDevices.flatMap { it.value },
    workload.values.flatMap { it.sensors },
    workload.values.map { it.application },
    clusterLevels.map { it.id },
    clusterLinkLatency.toDouble(),
    placementStrategy.id,
).apply {
    if (clientModule != null) {
        workload.values.forEach { workload ->
            workload.run {
                val clientModulePlacements = sensors.map { placementRequest(clientModule, it) }
                submitPlacementRequests(clientModulePlacements, 1)
            }
        }
    }
}

/**
 * Creates a MicroservicesMobilityClusteringController
 * @param clusterLevels determines which devices can manage their clusters
 * @param clusterLinkLatency the latency between any 2 devices in a cluster
 * @param placementStrategy the microservice placement strategy
 * @param clientModule which AppModule should be forced to be placed on client devices
 */
fun <T> Simulation<T>.microservicesMobilityClusteringController(
    clusterLevels: List<FogDeviceLevel>,
    clusterLinkLatency: Int = 0,
    placementStrategy: MicroservicePlacementStrategy = ClusteredPlacement,
    clientModule: ModuleType? = null
) = MicroservicesMobilityClusteringController(
    "controller",
    network.fogDevices.flatMap { it.value },
    workload.values.flatMap { it.sensors },
    workload.values.map { it.application },
    clusterLevels.map { it.id },
    clusterLinkLatency.toDouble(),
    placementStrategy.id,
    network.locator,
).apply {
    if (clientModule != null) {
        workload.values.forEach { workload ->
            workload.run {
                val clientModulePlacements = sensors.map { placementRequest(clientModule, it) }
                submitPlacementRequests(clientModulePlacements, 1)
            }
        }
    }
}
