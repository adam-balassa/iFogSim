package fi.aalto.cs.extensions

import fi.aalto.cs.utils.sample
import org.fog.mobilitydata.Location
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextDouble

data class Area(val top: Double, val right: Double, val bottom: Double, val left: Double) {
    constructor(locations: List<Location>) : this(
        top = locations.minOf { it.latitude },
        right = locations.maxOf { it.longitude },
        bottom = locations.maxOf { it.latitude },
        left = locations.minOf { it.longitude },
    )

    val width get() = right - left
    val height get() = bottom - top
    val center get() = Location((top + bottom) / 2, (left + right) / 2, -1)

    fun cells(n: Int, m: Int): List<Area> {
        val cellWidth = width / m
        val cellHeight = height / n
        return List(n * m) { index ->
            val i = index / m
            val j = index % m
            Area(
                top + cellHeight * i,
                left + cellWidth * (j + 1),
                top + cellHeight * (i + 1),
                left + cellWidth * j,
            )
        }
    }

    fun randomPoint() = Location(top + nextDouble() * height, left + nextDouble() * width, -1)

    fun contains(location: Location) = location.latitude in top..bottom && location.longitude in left..right
}

fun generateUniformLocations(trafficDataSet: Map<String, List<Location>>, numberOfDevices: Int): List<Location> {
    val area = Area(trafficDataSet.values.flatten())
    val gridDensity = ceil(sqrt(numberOfDevices.toDouble())).toInt()
    val cells = area.cells(gridDensity, gridDensity)
    val chosenCells = cells.sample(numberOfDevices)
    return chosenCells.map { it.randomPoint() }
}

fun generateAssistedLocations(trafficDataSet: Map<String, List<Location>>, numberOfDevices: Int): List<Location> {
    val area = Area(trafficDataSet.values.flatten())
    val gridDensity = ceil(sqrt(numberOfDevices.toDouble())).toInt()
    val cells = area.cells(gridDensity, gridDensity)
    val cellWeights = cells.map { cell -> trafficDataSet.values.map { it.last() }.count { cell.contains(it) } }
    val weightedCells = cells.zip(cellWeights).flatMap { (cell, weight) -> List(weight) { cell } }
    val chosenCells = weightedCells.sample(numberOfDevices)
    return chosenCells.map { it.randomPoint() }
}
