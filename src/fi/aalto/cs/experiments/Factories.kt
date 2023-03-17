package fi.aalto.cs.experiments

import org.cloudbus.cloudsim.core.CloudSim
import java.util.*

fun main() {
    val num_user = 1 // number of cloud users

    val calendar = Calendar.getInstance()
    val trace_flag = false // mean trace events

    CloudSim.init(num_user, calendar, trace_flag)
}
