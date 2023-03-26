package fi.aalto.cs.utils

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
import org.fog.placement.MicroservicesMobilityClusteringController
import org.fog.policy.AppModuleAllocationPolicy
import org.fog.scheduler.StreamOperatorScheduler
import org.fog.scheduler.TupleScheduler
import org.fog.utils.FogLinearPowerModel
import org.fog.utils.FogUtils.generateEntityId
import org.fog.utils.distribution.Distribution
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.Pair
import kotlin.String
import kotlin.also
import kotlin.apply
import kotlin.let
import kotlin.to
import org.apache.commons.math3.util.Pair as ApachePair

fun forwarding(mapping: Pair<TupleType, TupleType>, probability: Double = 1.0) =
    mapping to FractionalSelectivity(probability)

/**
 * Add an app module to the application
 * @param module Name of the `AppModule`
 * @param mips CPU resource consumption in Million instructions per second
 * @param ram Memory consumption in MB
 * @param storage Storage consumption in MB
 * @param bandwidth Bandwidth capacity to allocate in Mbps
 * @param selectivityMapping Forwarding probabilistic model from input tuples to output tuples
 */
fun <T>Simulation<T>.appModule(
    module: ModuleType,
    mips: Double = 1000.0,
    ram: Int = 10,
    storage: Long = 10_000,
    bandwidth: Long = 1000,
    selectivityMapping: Map<Pair<TupleType, TupleType>, SelectivityModel> = mutableMapOf(),
) {
    app.modules.add(
        AppModule(
            generateEntityId(),
            module.name,
            app.appId,
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
        ),
    )
}

/**
 * Add an app edge between two app modules to the application
 * @param source Source AppModule
 * @param destination Destination AppModule
 * @param tupleType The tuple's name that travels on the AppEdge
 * @param direction Up/Down/Actuator: which port should a FogDevice use to forward the tuple
 * @param cpuLength CPU power needed to process the tuple in Million instructions
 * @param dataSize The size of the tuple in MB
 * @param appEdgeType Indicates whether the edge is between modules or sensors or actuators
 */
fun <T>Simulation<T>.appEdge(
    source: ModuleType,
    destination: ModuleType,
    tupleType: TupleType,
    direction: TupleDirection,
    cpuLength: Double,
    dataSize: Double = 500.0,
    appEdgeType: AppEdgeType = AppEdgeType.InterModule,
) {
    val edge = AppEdge(source.name, destination.name, cpuLength, dataSize, tupleType.name, direction.id, appEdgeType.id)
    app.edges.add(edge)
    app.edgeMap[tupleType.name] = edge
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
 * @param busyPower Power consumption when the device is performing computation in MJ
 * @param idlePower Power consumption when the device is idle in MJ
 */
fun <T>Simulation<T>.fogDevice(
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
    busyPower: Double = 0.0, // MJ
    idlePower: Double = 0.0,
    costRatePerMips: Double = 0.0, // $
    costRatePerSecond: Double = 3.0,
    costRatePerMemory: Double = 0.0,
    costRatePerBandwidth: Double = 0.0,
    costRatePerStorage: Double = 0.001,
    microservicesFogDeviceType: MicroservicesFogDeviceType? = null,
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
    val deviceIndex = environment.fogDevices[type.name]?.size
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
        )
    }.also {
        it.parentId = parentId ?: NOT_SET
        it.level = level.id
        if (deviceIndex != null) {
            environment.fogDevices[type.name]?.add(it)
        } else {
            environment.fogDevices[type.name] = mutableListOf(it)
        }
    }
}

/**
 * Adds an AppLoop to the application
 */
fun <T>Simulation<T>.appLoop(vararg modules: ModuleType) {
    if (app.loops == null) app.loops = mutableListOf()
    app.loops.add(AppLoop(modules.map { it.name }))
}

/**
 * Adds a sensor to the simulation environment
 */
fun <T>Simulation<T>.sensor(
    gateway: FogDevice,
    tupleType: TupleType,
    latency: Double = 0.0,
    emissionDistribution: Distribution
) {
    environment.add(Sensor(
        "s-${gateway.name}",
        tupleType.name,
        user.id,
        app.appId,
        emissionDistribution
    ).also {
        it.latency = latency
        it.gatewayDeviceId = gateway.id
        it.app = app
    })
}

/**
 * Adds an actuator to the simulation environemnt
 */
fun <T>Simulation<T>.actuator(
    gateway: FogDevice,
    module: ModuleType,
    latency: Double = 0.0,
) {
    environment.add(Actuator(
        "a-${gateway.name}",
        user.id,
        app.appId,
        module.name
    ).also {
        it.latency = latency
        it.gatewayDeviceId = gateway.id
        it.app = app
    })
}

fun <T>Simulation<T>.placementRequest(
    module: ModuleType,
    sensor: Sensor
) = PlacementRequest(
    app.appId,
    sensor.id,
    sensor.gatewayDeviceId,
    mutableMapOf(module.name to sensor.gatewayDeviceId)
)

fun <T>Simulation<T>.microservicesMobilityClusteringController(
    clusterLevels: List<FogDeviceLevel>,
    clusterLinkLatency: Int = 0,
    placementStrategy: MicroservicePlacementStrategy = ClusteredPlacement
) = MicroservicesMobilityClusteringController(
    "controller",
    environment.fogDevices.flatMap { it.value },
    environment.sensors,
    listOf(app),
    clusterLevels.map { it.id },
    clusterLinkLatency.toDouble(),
    placementStrategy.id,
    environment.locator,
)
