@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.experiments.RoadWeatherHeterogeneousNetwork.FogDevices.*
import fi.aalto.cs.experiments.RoadWeatherHeterogeneousNetwork.Modules.*
import fi.aalto.cs.experiments.RoadWeatherHeterogeneousNetwork.Tuples.*
import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.FogDeviceLevel.Gateway
import fi.aalto.cs.utils.FogDeviceLevel.Proxy
import fi.aalto.cs.utils.MicroservicePlacementStrategy.ClusteredPlacement
import fi.aalto.cs.utils.MicroservicePlacementStrategy.RandomPlacement
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    // enableReporting()
    RoadWeatherHeterogeneousNetwork().run()
}

class RoadWeatherHeterogeneousNetwork {
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
        Vehicle, FiveGRadioUnit, ProxyServer, cloud, CloudFON // ktlint-disable enum-entry-name-case
    }

    private val simulation = Simulation(
        "Road weather with heterogeneous network",
        object {
            val numberOfVehiclesPerRU = 8
            val numberOfRadioUnitsPerParent = 2
            val numberOfProxyServers = 2
            val centralisedPlacement = false
            val randomPlacement = false
        },
    )

    init {
        initializeApplication()
        initializeFogDevices()

        simulation.apply {
            microservicesController(
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

    private fun addVehicle(connectedRadioUnit: Int) = simulation.apply {
        val vehicle = addFogDevice(
            Vehicle,
            level = FogDeviceLevel.User,
            mips = poissonNumber(150).toLong(),
            ram = poissonNumber(256),
            parentId = connectedRadioUnit,
            downlinkBandwidth = poissonNumber(270).toLong(),
            uplinkLatency = poissonNumber(2.0, 0.2),
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = Client
        )
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

    private fun add5GRadioUnit(parentNodeId: Int, latency: Double) = simulation.apply {
        addFogDevice(
            FiveGRadioUnit,
            level = Gateway,
            parentId = parentNodeId,
            mips = poissonNumber(4000).toLong(),
            ram = poissonNumber(2500),
            uplinkLatency = latency,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FCN,
            broadcastResults = true
        ).let {
            for (i in 0 until config.numberOfVehiclesPerRU) {
                addVehicle(it.id)
            }
        }
    }

    private fun addProxyServer(cloudId: Int) = simulation.apply {
        addFogDevice(
            ProxyServer,
            level = Proxy,
            parentId = cloudId,
            mips = poissonNumber(5000).toLong(),
            ram = poissonNumber(5000),
            uplinkLatency = poissonNumber(100.0),
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = if (config.centralisedPlacement) FCN else FON
        ).let {
            for (i in 0 until config.numberOfRadioUnitsPerParent) {
                add5GRadioUnit(it.id, poissonNumber(20.0))
            }
        }
    }

    private fun initializeFogDevices() = simulation.apply {
        val cloud = addFogDevice(
            cloud,
            level = FogDeviceLevel.Cloud,
            mips = 88_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = Cloud
        )
        val cloudId = cloud.id
        if (config.numberOfProxyServers > 0) {
            for (i in 0 until config.numberOfProxyServers) {
                addProxyServer(cloudId)
            }
        } else {
            for (i in 0 until config.numberOfRadioUnitsPerParent) {
                add5GRadioUnit(cloudId, poissonNumber(100.0))
            }
        }
    }
}
