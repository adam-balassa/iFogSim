<template>
  <div>
    <ResultsSummaryTable :results="experiments.results" class="pt-3"/>
    <TupleExecutionTable :tuple-executions="experiments.results.tupleExecutionLatencies" class="pt-3"/>
    <ExecutionLevelsChart
      v-if="experiments.results.executionLevels"
      :execution-levels="experiments.results.executionLevels"
      :levels="experiments.setup.fogDevices"
      class="pt-3"
    />
    <AppLoopLatenciesTable :app-loop-latencies="experiments.results.appLoopLatencies" class="pt-3"/>
    <AppLoopLatenciesChart
      :app-loop-latencies="experiments.results.appLoopLatencies"
      :experiments="experiments.experiment.split(',')"
      class="pt-3"/>
  </div>
</template>

<script setup lang="ts">

import ResultsSummaryTable from "./results-summary-table.vue";
import { AggregateExperimentDetails } from "@/types/types";
import AppLoopLatenciesTable from "./app-loop-latencies-table.vue";
import TupleExecutionTable from "./tuple-execution-table.vue";
import ExecutionLevelsChart from "@/views/experiment-details-panel/experiment-results/execution-levels-chart.vue";
import AppLoopLatenciesChart from "@/views/experiment-details-panel/aggregate-results/app-loop-latencies-chart.vue";

defineProps<{
  experiments: AggregateExperimentDetails
}>()


</script>

<style scoped>

</style>