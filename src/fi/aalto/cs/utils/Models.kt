package fi.aalto.cs.utils

interface NamedEntityType {
    val name: String
}

interface ModuleType : NamedEntityType
interface TupleType : NamedEntityType
interface FogDeviceType : NamedEntityType
enum class MicroservicesFogDeviceType(val typeName: String) {
    FCN("fcn"), FON("fon"), Client("client"), Cloud("cloud")
}
enum class FogDeviceLevel(val id: Int) {
    Cloud(0), Proxy(1), Gateway(2), User(3);
    companion object {
        fun byId(id: Int) = values().find { it.id == id }
    }
}

enum class TupleDirection(val id: Int) {
    Up(1), Down(2), Actuator(3)
}

enum class AppEdgeType(val id: Int) {
    FromSensor(1), ToActuator(2), InterModule(3)
}

enum class MicroservicePlacementStrategy(val id: Int) {
    ClusteredPlacement(2), DistributedPlacement(3)
}

enum class ModulePlacementStrategy {
    Edgewards, CloudOnly, Static
}
