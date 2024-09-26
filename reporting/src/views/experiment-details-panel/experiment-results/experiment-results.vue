<template>
  <div>
    <ResultsSummaryTable :results="results" class="pt-3"/>
    <TupleExecutionTimesTables
      v-if="results.executedTuples"
      :executed-tuples="results.executedTuples"
      :tuple-executions="results.tupleExecutionLatencies"
      class="pt-3"
    />
    <ExecutionLevelsChart
      v-if="results.executionLevels && areSimpleExecutionLevels(results.executionLevels)"
      :execution-levels="results.executionLevels"
      :levels="setup.fogDevices"
      class="pt-3"
    />
    <TemporalExecutionLevelsChart
      v-if="results.executionLevels && areTemporalExecutionLevels(results.executionLevels)"
      :execution-levels="results.executionLevels"
      :levels="setup.fogDevices"
      class="pt-3"
    />
    <AppLoopLatenciesTable :app-loop-latencies="results.appLoopLatencies" class="pt-3"/>
    <AppLoopLatenciesChart :app-loop-latencies="results.appLoopLatencies" class="pt-3"/>
    <WaitingTimeCharts
      v-if="results.waitingTuples"
      :waiting-tuples="results.waitingTuples"
      :devices="setup.network"
      :levels="setup.fogDevices"
    />
    <PowerConsumptionChart :power-consumptions="results.fogDeviceEnergyConsumptions" class="pt-3"/>
  </div>
</template>

<script setup lang="ts">

import ResultsSummaryTable from "./results-summary-table.vue";
import { ExperimentDetails } from "@/types/types";
import AppLoopLatenciesTable from "./app-loop-latencies-table.vue";
import PowerConsumptionChart from "./power-consumption-chart.vue";
import ExecutionLevelsChart from "./execution-levels-chart.vue";
import AppLoopLatenciesChart from "./app-loop-latencies-chart.vue";
import { areSimpleExecutionLevels, areTemporalExecutionLevels } from "@/utils/helpers";
import TemporalExecutionLevelsChart
  from "@/views/experiment-details-panel/experiment-results/temporal-execution-levels-chart.vue";
import WaitingTimeCharts from "@/views/experiment-details-panel/experiment-results/waiting-time-charts.vue";
import TupleExecutionTimesTables
  from "@/views/experiment-details-panel/experiment-results/tuple-execution-times-tables.vue";

defineProps<{
  results: ExperimentDetails['results'];
  setup: ExperimentDetails['setup']
}>()


</script>

<style scoped>

</style>