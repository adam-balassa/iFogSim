<template>
  <div>
    <ResultsSummaryTable :results="experiments.results" class="pt-3"/>
    <TupleExecutionTimesTables
      :tuple-executions="experiments.results.tupleExecutionLatencies"
      :executed-tuples="experiments.results.executedTuples"
    />
    <ExecutionLevelsChart
      v-if="experiments.results.executionLevels && areSimpleExecutionLevels(experiments.results.executionLevels)"
      :execution-levels="experiments.results.executionLevels"
      :levels="experiments.setup.fogDevices"
      class="pt-3"
    />
    <TemporalExecutionLevelsChart
      v-if="experiments.results.executionLevels && areTemporalExecutionLevels(experiments.results.executionLevels)"
      :execution-levels="experiments.results.executionLevels"
      :levels="experiments.setup.fogDevices"
      class="pt-3"
    />
    <AppLoopLatenciesTable :app-loop-latencies="experiments.results.appLoopLatencies" class="pt-3"/>
    <AppLoopLatenciesChart
      :app-loop-latencies="experiments.results.appLoopLatencies"
      :experiments="experiments.experiment.split(',')"
      class="pt-3"/>

    <WaitingTimeCharts
      v-if="experiments.results.waitingTuples"
      :waiting-tuples="experiments.results.waitingTuples"
      :devices="experiments.setup.network"
      :levels="experiments.setup.fogDevices"
    />
  </div>
</template>

<script setup lang="ts">

import ResultsSummaryTable from "./results-summary-table.vue";
import { AggregateExperimentDetails } from "@/types/types";
import AppLoopLatenciesTable from "./app-loop-latencies-table.vue";
import TupleExecutionTable from "./tuple-execution-table.vue";
import ExecutionLevelsChart from "@/views/experiment-details-panel/experiment-results/execution-levels-chart.vue";
import AppLoopLatenciesChart from "@/views/experiment-details-panel/aggregate-results/app-loop-latencies-chart.vue";
import TemporalExecutionLevelsChart
  from "@/views/experiment-details-panel/experiment-results/temporal-execution-levels-chart.vue";
import { areSimpleExecutionLevels, areTemporalExecutionLevels } from "@/utils/helpers";
import WaitingTimeCharts from "@/views/experiment-details-panel/experiment-results/waiting-time-charts.vue";
import TupleExecutionTimesTables
  from "./tuple-execution-times-tables.vue";

defineProps<{
  experiments: AggregateExperimentDetails
}>()


</script>

<style scoped>

</style>