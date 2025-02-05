@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.FromSensor
import fi.aalto.cs.utils.AppEdgeType.ToActuator
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.ModulePlacementStrategy.Static
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    // enableReporting()
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
            val numberOfVehiclesPerRU = 3
            val numberOfRadioUnitsPerParent = 3
            val numberOfProxyServers = 2
            val nirCameraFPS = 10
            val classificationModulePlacement = FogDevices.FiveGRadioUnit
            val microserviceController = true
        },
    )

    init {
        initializeApplication()
        initializeFogDevices()

        simulation.apply {
            if (config.microserviceController) {
                microservicesController(
                    clusterLevels = listOf(FogDeviceLevel.Gateway),
                    clusterLinkLatency = 2,
                    clientModule = Modules.DriverAssistanceSystem
                )
            } else {
                controller(
                    placementStrategy = Static,
                    staticPlacement = mapOf(
                        Modules.DriverAssistanceSystem to FogDevices.Vehicle,
                        Modules.RoadWeatherClassification to config.classificationModulePlacement
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

    private fun addVehicle(connectedRadioUnit: Int) = simulation.apply {
        val vehicle = fogDevice(
            FogDevices.Vehicle,
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
            module = Modules.SpeedControl,
            latency = 1.0,
        )
    }

    private fun add5GRadioUnit(parentNodeId: Int, latency: Double) = simulation.apply {
        fogDevice(
            FogDevices.FiveGRadioUnit,
            level = FogDeviceLevel.Gateway,
            parentId = parentNodeId,
            mips = 2500,
            ram = 2500,
            uplinkLatency = latency,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = if (config.microserviceController) FCN else null
        ).let {
            for (i in 0 until config.numberOfVehiclesPerRU) {
                addVehicle(it.id)
            }
        }
    }

    private fun addProxyServer(cloudId: Int) = simulation.apply {
        fogDevice(
            FogDevices.ProxyServer,
            level = FogDeviceLevel.Proxy,
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
            FogDevices.cloud,
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
