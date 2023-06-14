@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.experiments.GenerateBaseStations.FogDevices.*
import fi.aalto.cs.experiments.GenerateBaseStations.Modules.*
import fi.aalto.cs.experiments.GenerateBaseStations.Tuples.*
import fi.aalto.cs.extensions.generateAssistedLocations
import fi.aalto.cs.extensions.generateUniformLocations
import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.FogDeviceLevel.Gateway
import fi.aalto.cs.utils.FogDeviceLevel.Proxy
import fi.aalto.cs.utils.MicroservicePlacementStrategy.ClusteredPlacement
import fi.aalto.cs.utils.MicroservicePlacementStrategy.RandomPlacement
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.mobilitydata.Location
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    // enableReporting()
    GenerateBaseStations().run()
}

class GenerateBaseStations {
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
        "Road weather with generated base stations",
        object {
            val numberOfVehiclesPerRU = 8
            val numberOfRadioUnitsPerParent = 2
            val numberOfProxyServers = 2
            val centralisedPlacement = false
            val randomPlacement = false
            val numberOfRadioUnits get() = numberOfProxyServers * numberOfRadioUnitsPerParent
            val numberOfVehicles get() = numberOfVehiclesPerRU * numberOfRadioUnits
        },
    )

    init {
        initializeApplication()
        initializeVehicles()
        initializeFogDevices()

        simulation.apply {
            microservicesMobilityClusteringController(
                clusterLevels = listOf(Gateway),
                clusterLinkLatency = 2,
                clientModule = DriverAssistanceSystem,
                placementStrategy = if (config.randomPlacement) RandomPlacement else ClusteredPlacement
            )
        }
    }

    fun run() {
        simulation.run()
        reportSimulation(simulation, "./simulations")
    }

    private fun initializeApplication() = simulation.apply {
        addApplication("").apply {
            addAppModule(
                DriverAssistanceSystem,
                ram = 128,
                mips = 100.0,
                storage = 100,
                selectivityMapping = mapOf(
                    forwarding(NIRCamera to NIRCameraImage),
                    forwarding(RoadWeatherConditions to EstimatedBreakingDistance, 0.2)
                )
            )
            addAppModule(
                RoadWeatherClassification,
                ram = 1024,
                mips = 2500.0,
                storage = 200,
                selectivityMapping = mapOf(
                    forwarding(NIRCameraImage to RoadWeatherConditions),
                )
            )

            addAppEdge(
                NIRCamera,
                DriverAssistanceSystem,
                NIRCamera,
                Up,
                appEdgeType = FromSensor,
                cpuLength = 500.0,
                cpuLengthGenerator = poissonNumberGenerator(500.0)
            )
            addAppEdge(
                DriverAssistanceSystem,
                RoadWeatherClassification,
                NIRCameraImage,
                Up,
                cpuLength = 5000.0,
                cpuLengthGenerator = poissonNumberGenerator(5000.0)
            )

            addAppEdge(
                RoadWeatherClassification,
                DriverAssistanceSystem,
                RoadWeatherConditions,
                Down,
                cpuLength = 1000.0,
                cpuLengthGenerator = poissonNumberGenerator(1000.0)
            )
            addAppEdge(DriverAssistanceSystem, SpeedControl, EstimatedBreakingDistance, Actuator, appEdgeType = ToActuator, cpuLength = 14.0, dataSize = 1.0)

            addAppLoop(
                NIRCamera,
                DriverAssistanceSystem,
                RoadWeatherClassification,
                DriverAssistanceSystem,
                SpeedControl,
            )
        }
    }

    private fun initializeVehicles() = simulation.apply {
        val randMobilityGenerator = RandomMobilityGenerator()
        (1..simulation.config.numberOfVehicles).forEach {
            randMobilityGenerator.createRandomData(random_walk_mobility_model, it, "./dataset/road-weather-locations-", false)
        }

        network.locator.parseUserInfo(
            (1..simulation.config.numberOfVehicles).associateWith { DIRECTIONAL_MOBILITY },
            "./dataset/road-weather-locations-"
        )

        network.locator.mobileUserDataId.forEach { userId ->
            addVehicle().also {
                network.locator.linkDataWithInstance(it.id, userId)
            }
        }
    }

    private fun addVehicle() = simulation.run {
        addFogDevice(
            Vehicle,
            level = FogDeviceLevel.User,
            mips = poissonNumber(150).toLong(),
            ram = poissonNumber(256),
            downlinkBandwidth = poissonNumber(270).toLong(),
            uplinkLatency = poissonNumber(2.0, 0.2),
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = Client
        ).also { vehicle ->
            workload[""]!!.apply {
                addSensor(
                    gateway = vehicle,
                    tupleType = NIRCamera,
                    latency = 6.0,
                    emissionDistribution = DeterministicDistribution(100.0)
                )
                addActuator(
                    gateway = vehicle,
                    module = SpeedControl,
                    latency = 1.0,
                )
            }
        }
    }

    private fun add5GRadioUnit(location: Location) = simulation.run {
        addFogDevice(
            FiveGRadioUnit,
            level = Gateway,
            mips = poissonNumber(4000).toLong(),
            ram = poissonNumber(2500),
            uplinkLatency = 20.0,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FCN,
            broadcastResults = true,
            location = location
        )
    }

    private fun addProxyServer(cloudId: Int, location: Location) = simulation.run {
        addFogDevice(
            ProxyServer,
            level = Proxy,
            parentId = cloudId,
            mips = poissonNumber(5000).toLong(),
            ram = poissonNumber(5000),
            uplinkLatency = poissonNumber(100.0),
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = if (config.centralisedPlacement) FCN else FON,
            location = location
        )
    }

    private fun initializeFogDevices() = simulation.apply {
        val trafficDataset = network.locator.dataObject.usersLocation.entries.associate { (deviceId, locations) ->
            deviceId to locations.values.toList()
        }

        val cloud = addFogDevice(
            cloud,
            level = FogDeviceLevel.Cloud,
            mips = 88_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = Cloud,
            location = generateUniformLocations(trafficDataset, 1).first()
        )

        generateUniformLocations(trafficDataset, config.numberOfProxyServers).forEach { location ->
            addProxyServer(cloud.id, location)
        }

        generateAssistedLocations(trafficDataset, config.numberOfRadioUnits).forEach { location ->
            add5GRadioUnit(location).also {
                it.parentId = network.locator.determineParent(it.id, SETUP_TIME)
            }
        }
    }
}
