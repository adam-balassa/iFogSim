@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.experiments.GenerateBaseStations.FogDevices.*
import fi.aalto.cs.experiments.GenerateBaseStations.Modules.*
import fi.aalto.cs.experiments.GenerateBaseStations.Tuples.*
import fi.aalto.cs.extensions.PoissonFPS
import fi.aalto.cs.extensions.generateAssistedLocations
import fi.aalto.cs.extensions.generateUniformLocations
import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.FogDeviceLevel.*
import fi.aalto.cs.utils.MicroservicePlacementStrategy.ClusteredPlacement
import fi.aalto.cs.utils.MicroservicePlacementStrategy.RandomPlacement
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.MicroservicesFogDeviceType.Cloud
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.mobilitydata.Location
import org.fog.mobilitydata.RandomMobilityGenerator
import org.fog.mobilitydata.References.*
import java.lang.Thread.sleep

fun main() {
    enableReporting()
//    repeat(10) {
//        GenerateBaseStations("Main experiment | small | assisted", true, 4).run()
//        clearMonitors()
//        sleep(1000)
//    }
//    repeat(10) {
//        GenerateBaseStations("Main experiment | small | random", false, 4).run()
//        clearMonitors()
//        sleep(1000)
//    }
//    repeat(3) {
//        GenerateBaseStations("Main experiment | medium", true, 6).run()
//        clearMonitors()
//        sleep(1000)
//    }
    repeat(3) {
        GenerateBaseStations("Main experiment | XL", true, 10).run()
        clearMonitors()
        sleep(1000)
    }
    repeat(3) {
        GenerateBaseStations("Main experiment | XXL", true, 12).run()
        clearMonitors()
        sleep(1000)
    }
}

class GenerateBaseStations(
    name: String,
    assistedBaseStationPlacement: Boolean,
    numberOfProxyServers: Int,
) {
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
        name,
        object {
            val numberOfVehiclesPerRU = 10 // 20
            val numberOfRadioUnitsPerParent = 8 // 10
            val numberOfProxyServers = numberOfProxyServers // 4
            val assistedBaseStationPlacement = assistedBaseStationPlacement
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
                mips = 250.0,
                storage = 100,
                bandwidth = 52_428_800 / 10,
                selectivityMapping = mapOf(
                    forwarding(NIRCamera to NIRCameraImage),
                    forwarding(RoadWeatherConditions to EstimatedBreakingDistance, 0.2)
                )
            )
            addAppModule(
                RoadWeatherClassification,
                ram = 1024,
                mips = 500.0,
                storage = 200,
                bandwidth = 52_428_800 / 10,
                selectivityMapping = mapOf(
                    forwarding(NIRCameraImage to RoadWeatherConditions),
                ),
            )

            addAppEdge(
                NIRCamera,
                DriverAssistanceSystem,
                NIRCamera,
                Up,
                appEdgeType = FromSensor,
                cpuLength = 300.0,
                cpuLengthGenerator = { logNormal(300.0) },
                dataSize = 3_932_160.0,
            )
            addAppEdge(
                DriverAssistanceSystem,
                RoadWeatherClassification,
                NIRCameraImage,
                Up,
                cpuLength = 5000.0,
                cpuLengthGenerator = { logNormal(5000.0) },
                dataSize = 3_932_160.0
            )

            addAppEdge(
                RoadWeatherClassification,
                DriverAssistanceSystem,
                RoadWeatherConditions,
                Down,
                cpuLength = 800.0,
                cpuLengthGenerator = { logNormal(800.0) },
                dataSize = 30_720.0
            )
            addAppEdge(DriverAssistanceSystem, SpeedControl, EstimatedBreakingDistance, Actuator, appEdgeType = ToActuator, cpuLength = 14.0, dataSize = 100.0)

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
            level = User,
            mips = logNormal(2200.0).toLong(),
            ram = logNormal(256.0).toInt(),
            bandwidth = logNormal(52_428_800.0).toLong(),
            downlinkBandwidth = logNormal(512_000.0).toLong(),
            uplinkBandwidth = logNormal(52_428_800.0).toLong(),
            uplinkLatency = logNormal(2.0),
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = Client
        ).also { vehicle ->
            workload[""]!!.apply {
                addSensor(
                    gateway = vehicle,
                    tupleType = NIRCamera,
                    latency = 1.0,
                    emissionDistribution = PoissonFPS(30)
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
            mips = logNormal(3600.0).toLong(),
            ram = logNormal(2500.0).toInt(),
            bandwidth = logNormal(21_474_836_480.0).toLong(),
            downlinkBandwidth = logNormal(21_474_836_480.0).toLong(),
            uplinkBandwidth = logNormal(10_737_418_240.0).toLong(),
            clusterLinkBandwidth = logNormal(21_474_836_480.0).toLong(),
            uplinkLatency = logNormal(60.0),
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
            mips = 2 * logNormal(3300.0).toLong(),
            ram = logNormal(4000.0).toInt(),
            bandwidth = logNormal(21_474_836_480.0).toLong(),
            uplinkLatency = logNormal(100.0),
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
            mips = 96 * 3100,
            ram = 40_000,
            bandwidth = logNormal(21_474_836_480.0).toLong(),
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = Cloud,
            location = generateUniformLocations(trafficDataset, 1).first()
        )

        generateUniformLocations(trafficDataset, config.numberOfProxyServers).forEach { location ->
            addProxyServer(cloud.id, location)
        }

        generateBaseStationLocation(trafficDataset).forEach { location ->
            add5GRadioUnit(location).also {
                it.parentId = network.locator.determineParent(it.id, SETUP_TIME)
            }
        }
    }

    private fun generateBaseStationLocation(dataset: Map<String, List<Location>>) = simulation.run {
        if (config.assistedBaseStationPlacement) {
            generateAssistedLocations(dataset, config.numberOfRadioUnits)
        } else {
            generateUniformLocations(dataset, config.numberOfRadioUnits)
        }
    }
}
