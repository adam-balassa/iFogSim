package fi.aalto.cs.utils

interface NamedEntityType {
    val name: String
}

interface ModuleType : NamedEntityType
interface TupleType : NamedEntityType

enum class TupleDirection(val id: Int) {
    Up(1), Down(2), Actuator(3)
}

enum class AppEdgeType(val id: Int) {
    FromSensor(1), ToActuator(2), InterModule(3)
}
