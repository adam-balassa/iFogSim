@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.experiments.SimpleRoadWeatherExample.FogDevices.*
import fi.aalto.cs.experiments.SimpleRoadWeatherExample.Modules.*
import fi.aalto.cs.experiments.SimpleRoadWeatherExample.Tuples.*
import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.*
import fi.aalto.cs.utils.FogDeviceLevel.Gateway
import fi.aalto.cs.utils.FogDeviceLevel.Proxy
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.ModulePlacementStrategy.Static
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    enableReporting()
    SimpleRoadWeatherExample().run()
}

class SimpleRoadWeatherExample {
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
        "Simple road weather example",
        object {
            val numberOfVehiclesPerRU = 8
            val numberOfRadioUnitsPerParent = 2
            val numberOfProxyServers = 2
            val nirCameraFPS = 10
            val classificationModulePlacement = ProxyServer
            val microserviceController = true
            val stochasticAppEdge = true
        },
    )

    init {
        initializeApplication()
        initializeFogDevices()

        simulation.apply {
            if (config.microserviceController) {
                microservicesController(
                    clusterLevels = listOf(Gateway),
                    clusterLinkLatency = 2,
                    clientModule = DriverAssistanceSystem
                )
            } else {
                controller(
                    placementStrategy = Static,
                    staticPlacement = mapOf(
                        DriverAssistanceSystem to Vehicle,
                        RoadWeatherClassification to config.classificationModulePlacement
                    )
                )
            }
        }
    }

    fun run() {
        simulation.run()
        reportSimulation(simulation, "./simulations")
    }

    private fun initializeApplication() = simulation.apply {
        appModule(
            DriverAssistanceSystem,
            ram = 128,
            mips = 100.0,
            storage = 100,
            selectivityMapping = mapOf(
                forwarding(NIRCamera to NIRCameraImage),
                forwarding(RoadWeatherConditions to EstimatedBreakingDistance, 0.2)
            )
        )
        appModule(
            RoadWeatherClassification,
            ram = 1024,
            mips = 2500.0,
            storage = 200,
            selectivityMapping = mapOf(
                forwarding(NIRCameraImage to RoadWeatherConditions),
            )
        )

        appEdge(
            NIRCamera,
            DriverAssistanceSystem,
            NIRCamera,
            Up,
            appEdgeType = FromSensor,
            cpuLength = 500.0,
            cpuLengthGenerator = if (config.stochasticAppEdge) poissonNumberGenerator(500.0, 20.0) else null
        )
        appEdge(
            DriverAssistanceSystem,
            RoadWeatherClassification,
            NIRCameraImage,
            Up,
            cpuLength = 5000.0,
            cpuLengthGenerator = if (config.stochasticAppEdge) poissonNumberGenerator(5000.0, 40.0) else null
        )

        appEdge(
            RoadWeatherClassification,
            DriverAssistanceSystem,
            RoadWeatherConditions,
            Down,
            cpuLength = 1000.0,
            cpuLengthGenerator = if (config.stochasticAppEdge) poissonNumberGenerator(1000.0, 30.0) else null
        )
        appEdge(DriverAssistanceSystem, SpeedControl, EstimatedBreakingDistance, Actuator, appEdgeType = ToActuator, cpuLength = 14.0, dataSize = 1.0)

        appLoop(
            NIRCamera,
            DriverAssistanceSystem,
            RoadWeatherClassification,
            DriverAssistanceSystem,
            SpeedControl,
        )
    }

    private fun addVehicle(connectedRadioUnit: Int) = simulation.apply {
        val vehicle = fogDevice(
            Vehicle,
            level = FogDeviceLevel.User,
            mips = 150,
            ram = 256,
            parentId = connectedRadioUnit,
            downlinkBandwidth = 270,
            uplinkLatency = 2.0,
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = if (config.microserviceController) Client else null
        )

        sensor(
            gateway = vehicle,
            tupleType = NIRCamera,
            latency = 6.0,
            emissionDistribution = DeterministicDistribution(1000.0 / config.nirCameraFPS)
        )

        actuator(
            gateway = vehicle,
            module = SpeedControl,
            latency = 1.0,
        )
    }

    private fun add5GRadioUnit(parentNodeId: Int, latency: Double) = simulation.apply {
        fogDevice(
            FiveGRadioUnit,
            level = Gateway,
            parentId = parentNodeId,
            mips = 4000,
            ram = 2500,
            uplinkLatency = latency,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = if (config.microserviceController) FCN else null,
            broadcastResults = config.microserviceController
        ).let {
            for (i in 0 until config.numberOfVehiclesPerRU) {
                addVehicle(it.id)
            }
        }
    }

    private fun addProxyServer(cloudId: Int) = simulation.apply {
        fogDevice(
            ProxyServer,
            level = Proxy,
            parentId = cloudId,
            mips = 5000,
            ram = 5000,
            uplinkLatency = 100.0,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = if (config.microserviceController) FON else null
        ).let {
            for (i in 0 until config.numberOfRadioUnitsPerParent) {
                add5GRadioUnit(it.id, 20.0)
            }
        }
    }

    private fun initializeFogDevices() = simulation.apply {
        val cloud = fogDevice(
            cloud,
            level = FogDeviceLevel.Cloud,
            mips = 44_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = if (config.microserviceController) Cloud else null
        )

        if (config.numberOfProxyServers > 0) {
            for (i in 0 until config.numberOfProxyServers) {
                addProxyServer(cloud.id)
            }
        } else {
            for (i in 0 until config.numberOfRadioUnitsPerParent) {
                add5GRadioUnit(cloud.id, 100.0)
            }
        }
    }
}
