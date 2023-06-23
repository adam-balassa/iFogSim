<template>
  <div>
    <ResultsSummaryTable :results="results" class="pt-3"/>
    <TupleExecutionTable :tuple-executions="results.tupleExecutionLatencies" class="pt-3"/>
    <ExecutionLevelsChart
      v-if="results.executionLevels"
      :execution-levels="results.executionLevels"
      :levels="setup.fogDevices"
      class="pt-3"
    />
    <AppLoopLatenciesTable :app-loop-latencies="results.appLoopLatencies" class="pt-3"/>
    <AppLoopLatenciesChart :app-loop-latencies="results.appLoopLatencies" class="pt-3"/>
    <PowerConsumptionChart :power-consumptions="results.fogDeviceEnergyConsumptions" class="pt-3"/>
  </div>
</template>

<script setup lang="ts">

import ResultsSummaryTable from "./results-summary-table.vue";
import { ExperimentDetails } from "@/types/types";
import TupleExecutionTable from "./tuple-execution-table.vue";
import AppLoopLatenciesTable from "./app-loop-latencies-table.vue";
import PowerConsumptionChart from "./power-consumption-chart.vue";
import ExecutionLevelsChart from "./execution-levels-chart.vue";
import AppLoopLatenciesChart from "./app-loop-latencies-chart.vue";

defineProps<{
  results: ExperimentDetails['results'];
  setup: ExperimentDetails['setup']
}>()


</script>

<style scoped>

</style>