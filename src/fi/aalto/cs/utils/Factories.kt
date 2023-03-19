package fi.aalto.cs.utils

import org.fog.application.AppEdge
import org.fog.application.AppModule
import org.fog.application.selectivity.FractionalSelectivity
import org.fog.application.selectivity.SelectivityModel
import org.fog.scheduler.TupleScheduler
import org.fog.utils.FogUtils.generateEntityId
import org.apache.commons.math3.util.Pair as ApachePair

fun forwarding(mapping: Pair<TupleType, TupleType>, probability: Double = 1.0) =
    mapping to FractionalSelectivity(probability)

fun <T>Simulation<T>.appModule(
    module: ModuleType,
    mips: Double = 1000.0,
    ram: Int = 10,
    storage: Long = 10_000,
    bandwidth: Long = 1000,
    selectivityMapping: Map<Pair<TupleType, TupleType>, SelectivityModel> = mutableMapOf(),
) {
    app.modules.add(
        AppModule(
            generateEntityId(),
            module.name,
            app.appId,
            user.id,
            mips,
            ram,
            bandwidth,
            storage,
            "Xen",
            TupleScheduler(mips, 1),
            mutableMapOf<ApachePair<String, String>, SelectivityModel?>().apply {
                selectivityMapping.forEach { (t1, t2), model ->
                    put(ApachePair(t1.name, t2.name), model)
                }
            },
        ),
    )
}

fun <T>Simulation<T>.appEdge(
    source: ModuleType,
    destination: ModuleType,
    tupleType: TupleType,
    direction: TupleDirection,
    cpuLength: Double,
    dataSize: Double = 500.0,
    appEdgeType: AppEdgeType = AppEdgeType.InterModule,
) {
    val edge = AppEdge(source.name, destination.name, cpuLength, dataSize, tupleType.name, direction.id, appEdgeType.id)
    app.edges.add(edge)
    app.edgeMap[tupleType.name] = edge
}
