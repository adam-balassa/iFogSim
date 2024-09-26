<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">E2E latency distributions</h3>
    <AppLoopLatencyChart
      v-for="appLoop in appLoopLatencies"
      :title="appLoop.appLoop.join(' > ')"
      :datasets="[{ title: appLoop.appLoop.join(' > '), latencies: appLoop.latencies }]"
    />
  </div>
</template>

<script setup lang="ts">
import {ExperimentResults} from "@/types/types";
import AppLoopLatencyChart from "./app-loop-latency-chart.vue";

const props = defineProps<{
  appLoopLatencies: ExperimentResults['appLoopLatencies']
}>()


</script>

<style scoped>

</style>