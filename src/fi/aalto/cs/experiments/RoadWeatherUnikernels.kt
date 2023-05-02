@file:Suppress("KotlinConstantConditions", "EmptyRange")

package fi.aalto.cs.experiments

import fi.aalto.cs.experiments.RoadWeatherUnikernels.FogDevices.*
import fi.aalto.cs.experiments.RoadWeatherUnikernels.Modules.*
import fi.aalto.cs.experiments.RoadWeatherUnikernels.Tuples.*
import fi.aalto.cs.utils.*
import fi.aalto.cs.utils.AppEdgeType.*
import fi.aalto.cs.utils.FogDeviceLevel.Gateway
import fi.aalto.cs.utils.FogDeviceLevel.Proxy
import fi.aalto.cs.utils.MicroservicesFogDeviceType.*
import fi.aalto.cs.utils.TupleDirection.*
import org.fog.utils.distribution.DeterministicDistribution

fun main() {
    // enableDebugLogging()
    enableReporting()
    RoadWeatherUnikernels().run()
}

class RoadWeatherUnikernels {
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
        "Road weather with unikernels",
        object {
            val numberOfVehiclesPerRU = 8
            val numberOfRadioUnitsPerParent = 2
            val numberOfProxyServers = 2
        },
    )

    init {
        initializeFogDevices()
        initializeUsers()

        simulation.apply {
            microservicesController(
                clusterLevels = listOf(Gateway),
                clusterLinkLatency = 2,
                clientModule = DriverAssistanceSystem
            )
        }
    }

    fun run() {
        simulation.run()
        reportSimulation(simulation, "./simulations")
    }

    private fun initializeUsers() = simulation.apply {
        network.fogDevices[FiveGRadioUnit.name]?.forEach { radioUnit ->
            for (i in 0 until config.numberOfVehiclesPerRU) {
                val workload = this@RoadWeatherUnikernels.addApplication()
                addVehicle(radioUnit.id, workload.id)
            }
        }
    }

    private fun addApplication() = simulation.run {
        val numberOfVehicles = config.numberOfProxyServers * config.numberOfRadioUnitsPerParent * config.numberOfVehiclesPerRU
        addApplication(name = "rwa").apply {
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
                ram = 1024 / numberOfVehicles + 10,
                mips = 2500.0 / numberOfVehicles + 25,
                storage = 200 / numberOfVehicles + 10L,
                bandwidth = 1000 / numberOfVehicles + 10L,
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
            )
            addAppEdge(
                DriverAssistanceSystem,
                RoadWeatherClassification,
                NIRCameraImage,
                Up,
                cpuLength = 5000.0,
            )

            addAppEdge(
                RoadWeatherClassification,
                DriverAssistanceSystem,
                RoadWeatherConditions,
                Down,
                cpuLength = 1000.0,
            )
            addAppEdge(
                DriverAssistanceSystem,
                SpeedControl,
                EstimatedBreakingDistance,
                Actuator,
                appEdgeType = ToActuator,
                cpuLength = 14.0,
                dataSize = 1.0
            )

            addAppLoop(
                NIRCamera,
                DriverAssistanceSystem,
                RoadWeatherClassification,
                DriverAssistanceSystem,
                SpeedControl,
            )
        }
    }

    private fun addVehicle(connectedRadioUnit: Int, workloadId: String) = simulation.apply {
        val vehicle = addFogDevice(
            Vehicle,
            level = FogDeviceLevel.User,
            mips = 150,
            ram = 256,
            parentId = connectedRadioUnit,
            downlinkBandwidth = 270,
            uplinkLatency = 2.0,
            busyPower = 87.53,
            idlePower = 82.44,
            microservicesFogDeviceType = Client
        )
        workload[workloadId]!!.apply {
            addSensor(
                gateway = vehicle,
                tupleType = NIRCamera,
                latency = 6.0,
                emissionDistribution = DeterministicDistribution(1000.0 / 10.0)
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
            mips = 4000,
            ram = 2500,
            uplinkLatency = latency,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FCN,
            broadcastResults = true
        )
    }

    private fun addProxyServer(cloudId: Int) = simulation.apply {
        addFogDevice(
            ProxyServer,
            level = Proxy,
            parentId = cloudId,
            mips = 5000,
            ram = 5000,
            uplinkLatency = 100.0,
            busyPower = 107.339,
            idlePower = 83.4333,
            microservicesFogDeviceType = FON
        ).let {
            for (i in 0 until config.numberOfRadioUnitsPerParent) {
                add5GRadioUnit(it.id, 20.0)
            }
        }
    }

    private fun initializeFogDevices() = simulation.apply {
        val cloud = addFogDevice(
            cloud,
            level = FogDeviceLevel.Cloud,
            mips = 44_800,
            ram = 40_000,
            uplinkBandwidth = 100,
            costRatePerMips = 0.001,
            busyPower = 16 * 103.0,
            idlePower = 16 * 83.25,
            microservicesFogDeviceType = Cloud
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
