<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">E2E latency distributions</h3>
    <Chart class="w-full" v-for="chartData of datasets" type="scatter" :data="chartData"/>
  </div>
</template>

<script setup lang="ts">
import Chart from 'primevue/chart';
import { ExperimentResults } from "../../../types/types";
import { computed } from "vue";
import _ from "lodash";
import { CDF } from "../../../utils/helpers";
import { ChartData, ChartOptions } from "chart.js";

const props = defineProps<{
  appLoopLatencies: ExperimentResults['appLoopLatencies']
}>()

const datasets = computed<ChartData<'scatter'>[]>(() => (
  props.appLoopLatencies.map(appLoop => ({
    datasets: [
      {
        label: appLoop.appLoop.join(' > '),
        showLine: true,
        data: CDF(appLoop.latencies),
        tension: .4
      }
    ]
  }))
))


</script>

<style scoped>

</style>