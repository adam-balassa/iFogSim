package fi.aalto.cs.experiments

import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.*
import fi.aalto.cs.utils.Simulation
import fi.aalto.cs.utils.TupleDirection.*
import org.cloudbus.cloudsim.Host
import org.cloudbus.cloudsim.Pe
import org.cloudbus.cloudsim.Storage
import org.cloudbus.cloudsim.power.PowerHost
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking
import org.fog.application.AppLoop
import org.fog.application.Application
import org.fog.entities.*
import org.fog.entities.Actuator
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import org.fog.placement.MicroservicesMobilityClusteringController
import org.fog.placement.PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT
import org.fog.policy.AppModuleAllocationPolicy
import org.fog.scheduler.StreamOperatorScheduler
import org.fog.utils.FogLinearPowerModel
import org.fog.utils.FogUtils
import org.fog.utils.distribution.DeterministicDistribution
import java.util.*

private enum class Modules : ModuleType {
    Client,
    Service1,
    Service2,
    Service3,
    Display,
}

private enum class Tuples : TupleType {
    RawData,
    FilteredData1,
    FilteredData2,
    Result1,
    Result2,
    Result1Display,
    Result2Display,
}

object SENSOR : ModuleType, TupleType {
    override val name = "SENSOR"
}

private val simulation = Simulation(
    "Cardiovascular Health Monitoring Application CHM)",
    object {
        val numberOfMobileUser = 5
        val sensorTransmissionTime = 10.0
    },
)

fun main() {
    createApplication()
    createRandomMobilityDatasets()
    createMobileUsers()
    createFogDevices()

    simulation.run {
        val controller = MicroservicesMobilityClusteringController(
            "controller",
            environment.fogDevices,
            environment.sensors,
            listOf(app),
            listOf(2),
            2.0,
            CLUSTERED_MICROSERVICES_PLACEMENT,
            environment.locator,
        )

        controller.submitPlacementRequests(
            environment.sensors.map {
                PlacementRequest(app.appId, it.id, it.gatewayDeviceId, mutableMapOf(Modules.Client.name to it.gatewayDeviceId))
            },
            1,
        )
    }

    simulation.run()
}

private fun createApplication() {
    simulation.run {
        appModule(Modules.Client, ram = 128, mips = 150.0, storage = 100, selectivityMapping = mapOf(
            forwarding(SENSOR to Tuples.RawData, 0.9),
            forwarding(Tuples.Result1 to Tuples.Result1Display),
            forwarding(Tuples.Result2 to Tuples.Result2Display)
        ))
        appModule(Modules.Service1, ram = 512, mips = 250.0, storage = 200, selectivityMapping = mapOf(
            forwarding(Tuples.RawData to Tuples.FilteredData1),
            forwarding(Tuples.RawData to Tuples.FilteredData2)
        ))
        appModule(Modules.Service2, ram = 512, mips = 350.0, storage = 200, selectivityMapping = mapOf(
            forwarding(Tuples.FilteredData1 to Tuples.Result1)
        ))
        appModule(Modules.Service3, ram = 2048, mips = 450.0, storage = 1000, selectivityMapping = mapOf(
            forwarding(Tuples.FilteredData2 to Tuples.Result2)
        ))
    }

    simulation.run {
        appEdge(SENSOR, Modules.Client, SENSOR, Up, appEdgeType = FromSensor, cpuLength = 1000.0)
        appEdge(Modules.Client, Modules.Service1, Tuples.RawData, Up, cpuLength = 2000.0)
        appEdge(Modules.Service1, Modules.Service2, Tuples.FilteredData1, Up, cpuLength = 2500.0)
        appEdge(Modules.Service1, Modules.Service3, Tuples.FilteredData2, Up, cpuLength = 4000.0)

        appEdge(Modules.Service2, Modules.Client, Tuples.Result1, Down, cpuLength = 14.0)
        appEdge(Modules.Service3, Modules.Client, Tuples.Result2, Down, cpuLength = 28.0)
        appEdge(Modules.Client, Modules.Display, Tuples.Result1Display, Down, appEdgeType = ToActuator, cpuLength = 14.0)
        appEdge(Modules.Client, Modules.Display, Tuples.Result2Display, Down, appEdgeType = ToActuator, cpuLength = 14.0)
    }

    simulation.app.setSpecialPlacementInfo(Modules.Service3.name, "cloud")

    simulation.app.run {
        loops = listOf(
            AppLoop(
                listOf(
                    SENSOR.name,
                    Modules.Client.name,
                    Modules.Service1.name,
                    Modules.Service2.name,
                    Modules.Client.name,
                    Modules.Display.name,
                ),
            ),
        )
    }
}

private fun createMobileUsers() {
    val userMobilityPattern = mutableMapOf<Int, Int>()
    for (id in 1..simulation.config.numberOfMobileUser)
        userMobilityPattern[id] = DIRECTIONAL_MOBILITY

    simulation.environment.locator.parseUserInfo(userMobilityPattern, dataset_random)

    val mobileUserDataIds = simulation.environment.locator.mobileUserDataId
    for (i in 0 until simulation.config.numberOfMobileUser) {
        val mobile = addMobile(
            "mobile_$i",
            simulation.user.id,
            simulation.app,
            NOT_SET,
        ) // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
        mobile.uplinkLatency = 2.0 // latency of connection between the smartphone and proxy server is 2 ms
        simulation.environment.locator.linkDataWithInstance(mobile.id, mobileUserDataIds[i])
        mobile.level = 3
        simulation.environment.add(mobile)
    }
}

private fun createFogDevice(
    nodeName: String,
    mips: Long,
    ram: Int,
    upBw: Long,
    downBw: Long,
    ratePerMips: Double,
    busyPower: Double,
    idlePower: Double,
    deviceType: String,
): MicroserviceFogDevice {
    val peList: MutableList<Pe> = ArrayList()

    // 3. Create PEs and add these into a list.
    peList.add(Pe(0, PeProvisionerOverbooking(mips.toDouble()))) // need to store Pe id and MIPS Rating
    val hostId = FogUtils.generateEntityId()
    val storage: Long = 1000000 // host storage
    val bw = 10000
    val host = PowerHost(
        hostId,
        RamProvisionerSimple(ram),
        BwProvisionerOverbooking(bw.toLong()),
        storage,
        peList,
        StreamOperatorScheduler(peList),
        FogLinearPowerModel(busyPower, idlePower),
    )
    val hostList: MutableList<Host> = ArrayList()
    hostList.add(host)
    val arch = "x86" // system architecture
    val os = "Linux" // operating system
    val vmm = "Xen"
    val time_zone = 10.0 // time zone this resource located
    val cost = 3.0 // the cost of using processing in this resource
    val costPerMem = 0.05 // the cost of using memory in this resource
    val costPerStorage = 0.001 // the cost of using storage in this
    // resource
    val costPerBw = 0.0 // the cost of using bw in this resource
    val storageList = LinkedList<Storage>() // we are not adding SAN
    // devices by now
    val characteristics = FogDeviceCharacteristics(
        arch, os, vmm, host, time_zone, cost, costPerMem,
        costPerStorage, costPerBw,
    )
    return MicroserviceFogDevice(
        nodeName,
        characteristics,
        AppModuleAllocationPolicy(hostList),
        storageList,
        10.0,
        upBw.toDouble(),
        downBw.toDouble(),
        10000.0,
        0.0,
        ratePerMips,
        deviceType,
    )
}

private fun addMobile(name: String, userId: Int, app: Application, parentId: Int): FogDevice {
    val mobile: FogDevice = createFogDevice(
        name,
        200,
        2048,
        10000,
        270,
        0.0,
        87.53,
        82.44,
        MicroserviceFogDevice.CLIENT,
    )
    mobile.parentId = parentId
    // locator.setInitialLocation(name,drone.getId());
    val mobileSensor = Sensor(
        "s-$name",
        SENSOR.name,
        userId,
        app.appId,
        DeterministicDistribution(simulation.config.sensorTransmissionTime),
    ) // inter-transmission time of EEG sensor follows a deterministic distribution
    mobileSensor.app = app
    simulation.environment.add(mobileSensor)
    val mobileDisplay = Actuator("a-$name", userId, app.appId, Modules.Display.name)
    simulation.environment.add(mobileDisplay)
    mobileSensor.gatewayDeviceId = mobile.id
    mobileSensor.latency = 6.0 // latency of connection between EEG sensors and the parent Smartphone is 6 ms
    mobileDisplay.gatewayDeviceId = mobile.id
    mobileDisplay.latency = 1.0 // latency of connection between Display actuator and the parent Smartphone is 1 ms
    mobileDisplay.app = app
    return mobile
}

private fun createFogDevices() {
    val locator = simulation.environment.locator
    locator.parseResourceInfo()
    if (locator.getLevelWiseResources(locator.getLevelID("Cloud")).size == 1) {
        val cloud: FogDevice = createFogDevice(
            "cloud",
            44800,
            40000,
            100,
            10000,
            0.01,
            (16 * 103).toDouble(),
            16 * 83.25,
            MicroserviceFogDevice.CLOUD,
        ) // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.parentId = NOT_SET
        locator.linkDataWithInstance(
            cloud.id,
            locator.getLevelWiseResources(locator.getLevelID("Cloud"))[0],
        )
        cloud.level = 0
        simulation.environment.add(cloud)
        for (i in locator.getLevelWiseResources(locator.getLevelID("Proxy")).indices) {
            val proxy: FogDevice = createFogDevice(
                "proxy-server_$i",
                2800,
                4000,
                10000,
                10000,
                0.0,
                107.339,
                83.4333,
                MicroserviceFogDevice.FON,
            ) // creates the fog device Proxy Server (level=1)
            locator.linkDataWithInstance(
                proxy.id,
                locator.getLevelWiseResources(
                    locator.getLevelID("Proxy"),
                )[i],
            )
            proxy.parentId = cloud.id // setting Cloud as parent of the Proxy Server
            proxy.uplinkLatency = 100.0 // latency of connection from Proxy Server to the Cloud is 100 ms
            proxy.level = 1
            simulation.environment.add(proxy)
        }
        for (i in locator.getLevelWiseResources(locator.getLevelID("Gateway")).indices) {
            val gateway: FogDevice = createFogDevice(
                "gateway_$i",
                2800,
                4000,
                10000,
                10000,
                0.0,
                107.339,
                83.4333,
                MicroserviceFogDevice.FCN,
            )
            locator.linkDataWithInstance(
                gateway.id,
                locator.getLevelWiseResources(locator.getLevelID("Gateway"))[i],
            )
            gateway.parentId = locator.determineParent(gateway.id, SETUP_TIME)
            gateway.uplinkLatency = 4.0
            gateway.level = 2
            simulation.environment.add(gateway)
        }
    }
}

private fun createRandomMobilityDatasets() {
    val randMobilityGenerator = RandomMobilityGenerator()
    for (i in 0 until simulation.config.numberOfMobileUser) {
        randMobilityGenerator.createRandomData(random_walk_mobility_model, i + 1, dataset_random, false)
    }
}
