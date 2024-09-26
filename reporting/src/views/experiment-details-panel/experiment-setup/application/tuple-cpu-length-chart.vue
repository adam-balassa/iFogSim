<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">Tuple CPU lengths</h3>
    <Chart class="w-full" type="scatter" :data="chartData"/>
  </div>
</template>

<script setup lang="ts">
import Chart from 'primevue/chart';
import { computed } from "vue";
import { ChartData } from "chart.js";
import { histogram } from "@/utils/stats";

const props = defineProps<{
  tupleTypeCpuLength: { [tupleType: string]: number[] }
}>()

const chartData = computed<ChartData<'scatter'>>(() => ({
  datasets: Object.entries(props.tupleTypeCpuLength).map(([tupleType, cpuLengths]) => ({
    label: tupleType,
    data: histogram(cpuLengths, 20.0).map(([x, y]) => ({ x, y })),
    showLine: true,
    tension: .1
  }))
}))


</script>

<style scoped>

</style>