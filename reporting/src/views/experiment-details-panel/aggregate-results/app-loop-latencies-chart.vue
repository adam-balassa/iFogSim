<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">E2E latency distributions</h3>
    <AppLoopLatencyChart
      v-for="{ title, datasets } in datasets"
      :title="title"
      :datasets="datasets"
    />
  </div>
</template>

<script setup lang="ts">
import { AggregateExperimentResults } from "@/types/types";
import { computed, defineComponent } from "vue";
import AppLoopLatencyChart from "@/views/experiment-details-panel/experiment-results/app-loop-latency-chart.vue";

const props = defineProps<{
  experiments: string[];
  appLoopLatencies: AggregateExperimentResults['appLoopLatencies'];
}>()


const datasets = computed<{
  title: string;
  datasets: {
    title: string;
    latencies: number[]
  }[]
}[]>(() =>
  props.appLoopLatencies.map((appLoop) => ({
    title: appLoop.appLoop.join(' > '),
    datasets: appLoop.latencies.map((latencies, i) => ({
      title: props.experiments[i],
      latencies
    }))
  })
))


</script>
