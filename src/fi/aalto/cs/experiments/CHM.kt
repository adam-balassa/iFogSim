package fi.aalto.cs.experiments

import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.TupleDirection.Down
import fi.aalto.cs.utils.TupleDirection.Up
import org.fog.entities.FogDevice
import org.fog.entities.PlacementRequest
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import org.fog.placement.MicroservicesMobilityClusteringController
import org.fog.placement.PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT
import org.fog.utils.distribution.DeterministicDistribution

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

private enum class FogDevices : FogDeviceType {
    Mobile, ProxyServer, Gateway, cloud // ktlint-disable enum-entry-name-case
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
            environment.fogDevices.flatMap { it.value },
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

    simulation.appLoop(
        SENSOR,
        Modules.Client,
        Modules.Service1,
        Modules.Service2,
        Modules.Client,
        Modules.Display
    )
}

private fun createMobileUsers() {
    val userMobilityPattern = mutableMapOf<Int, Int>()
    for (id in 1..simulation.config.numberOfMobileUser)
        userMobilityPattern[id] = DIRECTIONAL_MOBILITY

    simulation.environment.locator.parseUserInfo(userMobilityPattern, dataset_random)

    val mobileUserDataIds = simulation.environment.locator.mobileUserDataId
    for (i in 0 until simulation.config.numberOfMobileUser) {
        val mobile = addMobile()
        simulation.environment.locator.linkDataWithInstance(mobile.id, mobileUserDataIds[i])
    }
}

private fun addMobile(): FogDevice =
    simulation.fogDevice(
        FogDevices.Mobile,
        level = FogDeviceLevel.User,
        microservicesFogDeviceType = MicroservicesFogDeviceType.Client,
        mips = 200,
        ram = 2048,
        downlinkBandwidth = 270,
        uplinkLatency = 2.0,
        busyPower = 87.53,
        idlePower = 82.44,
    ).also { mobile ->
        simulation.sensor(
            gateway = mobile,
            tupleType = SENSOR,
            latency = 6.0,
            emissionDistribution = DeterministicDistribution(simulation.config.sensorTransmissionTime)
        )

        simulation.actuator(
            gateway = mobile,
            module = Modules.Display,
            latency = 1.0,
        )
    }

private fun createFogDevices() {
    val locator = simulation.environment.locator
    locator.parseResourceInfo()
    if (locator.getLevelWiseResources(locator.getLevelID("Cloud")).size == 1) {
        val cloud = simulation.fogDevice(
            FogDevices.cloud,
            level = FogDeviceLevel.Cloud,
            microservicesFogDeviceType = MicroservicesFogDeviceType.Cloud,
            mips = 44_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,

        )
        locator.linkDataWithInstance(
            cloud.id,
            locator.getLevelWiseResources(locator.getLevelID("Cloud"))[0],
        )
        for (i in locator.getLevelWiseResources(locator.getLevelID("Proxy")).indices) {
            val proxy = simulation.fogDevice(
                FogDevices.ProxyServer,
                level = FogDeviceLevel.Proxy,
                microservicesFogDeviceType = MicroservicesFogDeviceType.FON,
                parentId = cloud.id,
                mips = 2800,
                ram = 4000,
                uplinkLatency = 100.0,
                busyPower = 107.339,
                idlePower = 83.4333,
            )
            locator.linkDataWithInstance(
                proxy.id,
                locator.getLevelWiseResources(locator.getLevelID("Proxy"))[i],
            )
        }
        for (i in locator.getLevelWiseResources(locator.getLevelID("Gateway")).indices) {
            val gateway = simulation.fogDevice(
                FogDevices.Gateway,
                level = FogDeviceLevel.Gateway,
                microservicesFogDeviceType = MicroservicesFogDeviceType.FCN,
                mips = 2800,
                ram = 4000,
                uplinkLatency = 4.0,
                busyPower = 107.339,
                idlePower = 83.4333,
            )
            locator.linkDataWithInstance(
                gateway.id,
                locator.getLevelWiseResources(locator.getLevelID("Gateway"))[i],
            )
            gateway.parentId = locator.determineParent(gateway.id, SETUP_TIME)
        }
    }
}

private fun createRandomMobilityDatasets() {
    val randMobilityGenerator = RandomMobilityGenerator()
    for (i in 0 until simulation.config.numberOfMobileUser) {
        randMobilityGenerator.createRandomData(random_walk_mobility_model, i + 1, dataset_random, false)
    }
}
