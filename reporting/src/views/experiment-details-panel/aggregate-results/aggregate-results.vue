<template>
  <div>
    <ResultsSummaryTable :results="experiments.results" class="pt-3"/>
    <AppLoopLatenciesTable :app-loop-latencies="experiments.results.appLoopLatencies" class="pt-3"/>
    <AppLoopLatencyChart
      :app-loop-latencies="experiments.results.appLoopLatencies"
      :experiments="experiments.experiment.split(',')"
      class="pt-3"/>
  </div>
</template>

<script setup lang="ts">

import ResultsSummaryTable from "./results-summary-table.vue";
import { ExperimentDetails, AggregateExperimentResults, AggregateExperimentDetails } from "@/types/types";
import AppLoopLatenciesTable from "./app-loop-latencies-table.vue";
import AppLoopLatencyChart from "./app-loop-latency-chart.vue";

defineProps<{
  experiments: AggregateExperimentDetails
}>()


</script>

<style scoped>

</style>