package fi.aalto.cs.extensions

import org.fog.entities.Tuple

fun copyTuple(inputTuple: Tuple, destinationDeviceId: Int) = Tuple(
    inputTuple.appId,
    inputTuple.cloudletId,
    inputTuple.direction,
    inputTuple.cloudletLength,
    inputTuple.numberOfPes,
    inputTuple.cloudletFileSize,
    inputTuple.cloudletOutputSize,
    inputTuple.utilizationModelCpu,
    inputTuple.utilizationModelRam,
    inputTuple.utilizationModelBw
).apply {
    userId = inputTuple.userId
    actualTupleId = inputTuple.actualTupleId
    srcModuleName = inputTuple.srcModuleName
    sourceModuleId = inputTuple.sourceModuleId
    destModuleName = inputTuple.destModuleName
    tupleType = inputTuple.tupleType
    sourceDeviceId = inputTuple.sourceDeviceId
    this.destinationDeviceId = destinationDeviceId
    setTraversedMicroservices(inputTuple.traversed)
}
