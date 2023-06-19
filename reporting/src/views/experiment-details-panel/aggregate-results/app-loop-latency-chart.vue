<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">E2E latency distributions</h3>
    <Chart class="w-full" v-for="(chartData, i) of datasets" type="scatter"
           :data="chartData"
           :options="{
             plugins: {
               title: {
                display: true,
                text: props.appLoopLatencies[i].appLoop.join(' > ')
              }
            }
           }"
    />
  </div>
</template>

<script setup lang="ts">
import Chart from 'primevue/chart';
import { AggregateExperimentResults } from "@/types/types";
import {computed} from "vue";
import {ChartData} from "chart.js";
import { CDF } from "@/utils/stats";
import { last } from "lodash";

const props = defineProps<{
  experiments: string[];
  appLoopLatencies: AggregateExperimentResults['appLoopLatencies'];
}>()

const datasets = computed<ChartData<'scatter'>[]>(() => (
  props.appLoopLatencies.map(appLoop => {
    const cdfs = appLoop.latencies.map(measurement => CDF(measurement))
    return {
      datasets: cdfs
        .map(cdf => cdf.map(({x, y}) => ({ x, y: y / last(cdf)!.y })))
        .map((cdf, i) => ({
          displayName: appLoop.appLoop.join(' > '),
          label: props.experiments[i],
          showLine: true,
          data: cdf,
          tension: .1
        }))
    }
  })
))


</script>

<style scoped>

</style>