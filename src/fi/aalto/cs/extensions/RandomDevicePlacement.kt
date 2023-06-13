package fi.aalto.cs.extensions

import fi.aalto.cs.utils.FogDeviceLevel.User
import fi.aalto.cs.utils.add
import org.fog.application.Application
import org.fog.entities.ControllerComponent.CPU
import org.fog.entities.FogDevice
import org.fog.entities.PlacementRequest
import org.fog.entities.Tuple.UP
import org.fog.placement.MicroservicePlacementLogic
import org.fog.placement.PlacementLogicOutput
import org.fog.utils.ModuleLaunchConfig
import kotlin.Double
import kotlin.Int
import kotlin.Pair
import kotlin.String
import kotlin.apply
import kotlin.let
import kotlin.to
import org.apache.commons.math3.util.Pair as ApachePair

/** Placement request -> (App module -> Device id) */
typealias Placement = MutableMap<Int, MutableMap<String, Int>>

/** Device -> (App module -> number of instances) */
typealias ModuleInstances = MutableMap<Int, MutableMap<String, Int>>

class RandomDevicePlacement : MicroservicePlacementLogic {
    private lateinit var chosenDevice: FogDevice
    override fun run(
        fogDevices: List<FogDevice>,
        applicationInfo: Map<String, Application>,
        resourceAvailability: Map<Int, Map<String, Double>>,
        placementRequests: List<PlacementRequest>
    ): PlacementLogicOutput {
        val (placement, moduleInstances) = executePlacement(fogDevices, placementRequests, applicationInfo)
        return generatePlacementOutput(
            placement,
            moduleInstances,
            placementRequests,
            applicationInfo
        )
    }

    private fun executePlacement(
        fogDevices: List<FogDevice>,
        placementRequests: List<PlacementRequest>,
        applicationInfo: Map<String, Application>
    ): Pair<Placement, ModuleInstances> {
        chosenDevice = fogDevices.filter { it.level != User.id }.random()

        fun ModuleInstances.put(deviceId: Int, microservice: String) {
            this[deviceId]?.let { it[microservice] = (it[microservice] ?: 0) + 1 } ?: let {
                this[deviceId] = mutableMapOf(microservice to 1)
            }
        }

        // Place special placement
        val moduleInstances = mutableMapOf<Int, MutableMap<String, Int>>().apply {
            placementRequests.forEach { pr ->
                val app = applicationInfo[pr.applicationId]!!
                app.specialPlacementInfo.entries.forEach { (microservice, devices) ->
                    devices.forEach { deviceName ->
                        put(fogDevices[deviceName]!!.id, microservice)
                    }
                }
            }
        }

        // Place modules
        val placement = mutableMapOf<Int, MutableMap<String, Int>>().apply {
            placementRequests.forEach { pr ->
                val app = applicationInfo[pr.applicationId]!!
                val microservicesToPlace = app.modules
                    .filter { !pr.placedMicroservices.containsKey(it.name) }
                    .map { it.name }
                    .toSet()
                microservicesToPlace.forEach { microservice ->
                    add(pr.placementRequestId, microservice, chosenDevice.id)
                    moduleInstances.put(chosenDevice.id, microservice)
                }
            }
        }

        return placement to moduleInstances
    }

    private fun generatePlacementOutput(
        placement: Placement,
        moduleInstances: ModuleInstances,
        placementRequests: List<PlacementRequest>,
        applicationInfo: Map<String, Application>,
    ): PlacementLogicOutput {
        val prMap = placementRequests.associateBy { it.placementRequestId }

        fun updatePlacementRequests() {
            val alreadyMappedPlacements = mutableMapOf<Int, MutableList<String>>()
            placement.forEach { (id, serviceToDevice) ->
                val placementRequest = prMap[id]!!
                serviceToDevice.forEach { (microservice, device) ->
                    if (placementRequest.placedMicroservices.containsKey(microservice)) {
                        alreadyMappedPlacements.add(id, microservice)
                    } else {
                        placementRequest.placedMicroservices[microservice] = device
                    }
                }
            }
            alreadyMappedPlacements.entries
                .associate { (id, servicesToDelete) -> servicesToDelete to placement[id]!! }
                .entries.forEach { (servicesToDelete, serviceToDevice) ->
                    servicesToDelete.forEach { serviceToDevice.remove(it) }
                }
        }
        fun generatePlacementPerDevice(): Map<Int, Map<Application, List<ModuleLaunchConfig>>> {
            val moduleToApp = applicationInfo.values
                .flatMap { it.modules }
                .associate { it.name to (it to applicationInfo[it.appId]!!) }
            val perDevice = moduleInstances.entries.associate { (deviceId, instances) ->
                deviceId to instances.entries
                    .map { (microservice, count) -> ModuleLaunchConfig(moduleToApp[microservice]!!.first, count) }
                    .groupBy { moduleToApp[it.module.name]!!.second }
            }
            return perDevice
        }

        fun generateServiceDiscovery(): MutableMap<Int, MutableList<ApachePair<String, Int>>> {
            fun getClientDevices(app: Application, microservice: String, placed: Map<String, Int>) =
                app.edges
                    .filter { it.destination == microservice }
                    .filter { it.direction == UP }
                    .map { it.source }
                    .mapNotNull { placed[it] }

            val serviceDiscovery = mutableMapOf<Int, MutableList<ApachePair<String, Int>>>()
            placement.forEach { (id, serviceToDevice) ->
                val placementRequest = prMap[id]!!
                serviceToDevice.forEach { (microservice, device) ->
                    getClientDevices(
                        applicationInfo[placementRequest.applicationId]!!,
                        microservice,
                        placementRequest.placedMicroservices
                    ).forEach { clientDevice ->
                        serviceDiscovery.add(clientDevice, ApachePair.create(microservice, device))
                    }
                }
            }
            return serviceDiscovery
        }

        updatePlacementRequests()
        val perDevice = generatePlacementPerDevice()
        val serviceDiscovery = generateServiceDiscovery()

        return PlacementLogicOutput(perDevice, serviceDiscovery, placementRequests.associateWith { -1 })
    }

    override fun updateResources(resourceAvailability: MutableMap<Int, MutableMap<String, Double>>) {
        resourceAvailability.add(chosenDevice.id, CPU, 0.0)
    }

    override fun postProcessing() {
        // no op
    }

    private operator fun List<FogDevice>.get(name: String) = find { it.name == name }
    private fun List<FogDevice>.findById(id: Int) = find { it.id == id }
}
