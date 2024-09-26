package fi.aalto.cs.experiments

import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.TupleDirection.Down
import fi.aalto.cs.utils.TupleDirection.Up
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
//    enableReporting()
    CHM().run()
}

class CHM {
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

    private object SENSOR : ModuleType, TupleType {
        override val name = "SENSOR"
    }

    private enum class FogDevices : FogDeviceType {
        Mobile, ProxyServer, Gateway, cloud // ktlint-disable enum-entry-name-case
    }

    private val simulation = Simulation(
        "Cardiovascular Health Monitoring Application (CHM)",
        object {
            val numberOfMobileUser = 5
            val sensorTransmissionTime = 30.0
        },
    )

    init {
        initializeApplication()
        initializeMobileUsers()
        initializeFogDevices()

        simulation.apply {
            microservicesMobilityClusteringController(
                clusterLevels = listOf(FogDeviceLevel.Gateway),
                clusterLinkLatency = 2,
                clientModule = Modules.Client
            )
        }
    }

    fun run() {
        simulation.run()
        reportSimulation(simulation, "./simulations")
    }

    private fun initializeApplication() = simulation.addApplication("").apply {
        addAppModule(
            Modules.Client,
            ram = 128,
            mips = 150.0,
            storage = 100,
            selectivityMapping = mapOf(
                forwarding(SENSOR to Tuples.RawData, 0.9),
                forwarding(Tuples.Result1 to Tuples.Result1Display),
                forwarding(Tuples.Result2 to Tuples.Result2Display)
            )
        )
        addAppModule(
            Modules.Service1,
            ram = 512,
            mips = 250.0,
            storage = 200,
            selectivityMapping = mapOf(
                forwarding(Tuples.RawData to Tuples.FilteredData1),
                forwarding(Tuples.RawData to Tuples.FilteredData2)
            )
        )
        addAppModule(
            Modules.Service2,
            ram = 512,
            mips = 350.0,
            storage = 200,
            selectivityMapping = mapOf(
                forwarding(Tuples.FilteredData1 to Tuples.Result1)
            )
        )
        addAppModule(
            Modules.Service3,
            ram = 2048,
            mips = 450.0,
            storage = 1000,
            selectivityMapping = mapOf(
                forwarding(Tuples.FilteredData2 to Tuples.Result2)
            )
        )

        addAppEdge(
            SENSOR,
            Modules.Client,
            SENSOR,
            Up,
            appEdgeType = FromSensor,
            cpuLength = 1000.0
        )
        addAppEdge(Modules.Client, Modules.Service1, Tuples.RawData, Up, cpuLength = 2000.0)
        addAppEdge(Modules.Service1, Modules.Service2, Tuples.FilteredData1, Up, cpuLength = 2500.0)
        addAppEdge(Modules.Service1, Modules.Service3, Tuples.FilteredData2, Up, cpuLength = 4000.0)

        addAppEdge(Modules.Service2, Modules.Client, Tuples.Result1, Down, cpuLength = 14.0)
        addAppEdge(Modules.Service3, Modules.Client, Tuples.Result2, Down, cpuLength = 28.0)
        addAppEdge(Modules.Client, Modules.Display, Tuples.Result1Display, Down, appEdgeType = ToActuator, cpuLength = 14.0)
        addAppEdge(Modules.Client, Modules.Display, Tuples.Result2Display, Down, appEdgeType = ToActuator, cpuLength = 14.0)

        application.setSpecialPlacementInfo(Modules.Service3.name, "cloud")

        addAppLoop(
            SENSOR,
            Modules.Client,
            Modules.Service1,
            Modules.Service2,
            Modules.Client,
            Modules.Display
        )
    }

    private fun initializeMobileUsers() = simulation.apply {
        // create random user-location dataset
        val randMobilityGenerator = RandomMobilityGenerator()
        (1..config.numberOfMobileUser).forEach {
            randMobilityGenerator.createRandomData(random_walk_mobility_model, it, dataset_random, false)
        }

        network.locator.parseUserInfo(
            (1..simulation.config.numberOfMobileUser).associateWith { DIRECTIONAL_MOBILITY },
            dataset_random
        )

        network.locator.mobileUserDataId.forEach { userId ->
            addMobile(userId)
        }
    }

    private fun addMobile(locatorUserId: String) = simulation.apply {
        val mobile = addFogDevice(
            FogDevices.Mobile,
            level = FogDeviceLevel.User,
            microservicesFogDeviceType = MicroservicesFogDeviceType.Client,
            mips = 200,
            ram = 2048,
            downlinkBandwidth = 270,
            uplinkLatency = 2.0,
            busyPower = 87.53,
            idlePower = 82.44,
        )

        workload[""]!!.addSensor(
            gateway = mobile,
            tupleType = SENSOR,
            latency = 6.0,
            emissionDistribution = DeterministicDistribution(simulation.config.sensorTransmissionTime)
        )

        workload[""]!!.addActuator(
            gateway = mobile,
            module = Modules.Display,
            latency = 1.0,
        )
        network.locator.linkDataWithInstance(mobile.id, locatorUserId)
    }

    private fun initializeFogDevices() = simulation.apply {
        network.locator.parseResourceInfo()
        val cloudLocationId = network.locator.getLevelWiseResources(FogDeviceLevel.Cloud.id).firstOrNull()
        check(cloudLocationId != null) { "Incorrectly parsed location info" }

        val cloud = addFogDevice(
            FogDevices.cloud,
            level = FogDeviceLevel.Cloud,
            microservicesFogDeviceType = MicroservicesFogDeviceType.Cloud,
            mips = 44_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
        ).also {
            network.locator.linkDataWithInstance(it.id, cloudLocationId)
        }

        network.locator.getLevelWiseResources(FogDeviceLevel.Proxy.id).forEach { proxyId ->
            addFogDevice(
                FogDevices.ProxyServer,
                level = FogDeviceLevel.Proxy,
                microservicesFogDeviceType = MicroservicesFogDeviceType.FON,
                parentId = cloud.id,
                mips = 2800,
                ram = 4000,
                uplinkLatency = 100.0,
                busyPower = 107.339,
                idlePower = 83.4333,
            ).also {
                network.locator.linkDataWithInstance(it.id, proxyId)
            }
        }

        network.locator.getLevelWiseResources(FogDeviceLevel.Gateway.id).forEach { gatewayId ->
            addFogDevice(
                FogDevices.Gateway,
                level = FogDeviceLevel.Gateway,
                microservicesFogDeviceType = MicroservicesFogDeviceType.FCN,
                mips = 2800,
                ram = 4000,
                uplinkLatency = 4.0,
                busyPower = 107.339,
                idlePower = 83.4333,
            ).also {
                network.locator.linkDataWithInstance(it.id, gatewayId)
                it.parentId = network.locator.determineParent(it.id, SETUP_TIME)
            }
        }
    }
}
