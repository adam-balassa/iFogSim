package fi.aalto.cs.extensions

import fi.aalto.cs.utils.poisson
import org.fog.utils.distribution.Distribution

class PoissonFPS(fps: Int) : Distribution() {
    private val meanInterleaving = 1000.0 / fps
    override fun getNextValue() = poisson(meanInterleaving)

    override fun getDistributionType() = POISSON

    override fun getMeanInterTransmitTime() = meanInterleaving
}

