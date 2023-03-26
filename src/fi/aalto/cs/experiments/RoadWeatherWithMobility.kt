@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    enableReporting()
    RoadWeatherWithMobility().run()
}

class RoadWeatherWithMobility {
    private enum class Modules : ModuleType {
        DriverAssistanceSystem,
        RoadWeatherClassification,
        SpeedControl
    }

    private enum class Tuples : TupleType {
        NIRCameraImage,
        RoadWeatherConditions,
        EstimatedBreakingDistance
    }

    private object NIRCamera : ModuleType, TupleType {
        override val name = "SENSOR"
    }

    private enum class FogDevices : FogDeviceType {
        Vehicle, FiveGRadioUnit, ProxyServer, cloud // ktlint-disable enum-entry-name-case
    }

    private val simulation = Simulation(
        "Road weather with mobility",
        object {
            val numberOfVehicles = 25
            val nirCameraFPS = 10
        },
    )

    init {
        initializeApplication()
        initializeVehicles()
        initializeFogDevices()

        simulation.apply {
            microservicesMobilityClusteringController(
                clusterLevels = listOf(FogDeviceLevel.Gateway),
                clusterLinkLatency = 2,
                clientModule = Modules.DriverAssistanceSystem
            )
        }
    }

    fun run() {
        simulation.run()
        reportSimulation(simulation, "./simulations")
    }

    private fun initializeApplication() = simulation.apply {
        appModule(
            Modules.DriverAssistanceSystem,
            ram = 128,
            mips = 100.0,
            storage = 100,
            selectivityMapping = mapOf(
                forwarding(NIRCamera to Tuples.NIRCameraImage),
                forwarding(Tuples.RoadWeatherConditions to Tuples.EstimatedBreakingDistance, 0.2)
            )
        )
        appModule(
            Modules.RoadWeatherClassification,
            ram = 1024,
            mips = 2500.0,
            storage = 200,
            selectivityMapping = mapOf(
                forwarding(Tuples.NIRCameraImage to Tuples.RoadWeatherConditions),
            )
        )

        appEdge(NIRCamera, Modules.DriverAssistanceSystem, NIRCamera, Up, appEdgeType = FromSensor, cpuLength = 500.0)
        appEdge(Modules.DriverAssistanceSystem, Modules.RoadWeatherClassification, Tuples.NIRCameraImage, Up, cpuLength = 5000.0)

        appEdge(Modules.RoadWeatherClassification, Modules.DriverAssistanceSystem, Tuples.RoadWeatherConditions, Down, cpuLength = 1000.0)
        appEdge(Modules.DriverAssistanceSystem, Modules.SpeedControl, Tuples.EstimatedBreakingDistance, Actuator, appEdgeType = ToActuator, cpuLength = 14.0, dataSize = 1.0)

        appLoop(
            NIRCamera,
            Modules.DriverAssistanceSystem,
            Modules.RoadWeatherClassification,
            Modules.DriverAssistanceSystem,
            Modules.SpeedControl,
        )
    }

    private fun initializeVehicles() = simulation.apply {
        val randMobilityGenerator = RandomMobilityGenerator()
        (1..simulation.config.numberOfVehicles).forEach {
            randMobilityGenerator.createRandomData(random_walk_mobility_model, it, dataset_random, false)
        }

        environment.locator.parseUserInfo(
            (1..simulation.config.numberOfVehicles).associateWith { DIRECTIONAL_MOBILITY },
            dataset_random
        )

        environment.locator.mobileUserDataId.forEach { userId ->
            addVehicle().also {
                environment.locator.linkDataWithInstance(it.id, userId)
            }
        }
    }

    private fun addVehicle() = simulation.run {
        fogDevice(
            FogDevices.Vehicle,
            level = FogDeviceLevel.User,
            mips = 150,
            ram = 256,
            downlinkBandwidth = 270,
            uplinkLatency = 2.0,
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = Client
        ).also { vehicle ->
            sensor(
                gateway = vehicle,
                tupleType = NIRCamera,
                latency = 6.0,
                emissionDistribution = DeterministicDistribution(1000.0 / config.nirCameraFPS)
            )
            actuator(
                gateway = vehicle,
                module = Modules.SpeedControl,
                latency = 1.0,
            )
        }
    }

    private fun add5GRadioUnit() = simulation.run {
        fogDevice(
            FogDevices.FiveGRadioUnit,
            level = FogDeviceLevel.Gateway,
            mips = 2500,
            ram = 2500,
            uplinkLatency = 20.0,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FCN
        )
    }

    private fun addProxyServer(cloudId: Int) = simulation.run {
        fogDevice(
            FogDevices.ProxyServer,
            level = FogDeviceLevel.Proxy,
            parentId = cloudId,
            mips = 5000,
            ram = 5000,
            uplinkLatency = 100.0,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FON
        )
    }

    private fun initializeFogDevices() = simulation.apply {
        environment.locator.parseResourceInfo()
        val cloudLocationId = environment.locator.getLevelWiseResources(FogDeviceLevel.Cloud.id).firstOrNull()
        check(cloudLocationId != null) { "Incorrectly parsed location info" }

        val cloud = fogDevice(
            FogDevices.cloud,
            level = FogDeviceLevel.Cloud,
            mips = 44_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = Cloud
        ).also {
            environment.locator.linkDataWithInstance(it.id, cloudLocationId)
        }

        environment.locator.getLevelWiseResources(FogDeviceLevel.Proxy.id).forEach { proxyId ->
            addProxyServer(cloud.id).also {
                environment.locator.linkDataWithInstance(it.id, proxyId)
            }
        }

        environment.locator.getLevelWiseResources(FogDeviceLevel.Gateway.id).forEach { gatewayId ->
            add5GRadioUnit().also {
                environment.locator.linkDataWithInstance(it.id, gatewayId)
                it.parentId = environment.locator.determineParent(it.id, SETUP_TIME)
            }
        }
    }
}
