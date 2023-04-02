<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">Execution levels</h3>
    <Chart class="w-full" type="bar" :data="chartData" :options="{
      x: { stacked: true, grid: { offset: false } },
      y: { stacked: true, grid: { offset: false } }
     }"/>
  </div>
</template>

<script setup lang="ts">
import Chart from 'primevue/chart';
import { ExperimentSetup } from "../../../types/types";
import { computed } from "vue";
import { ChartData } from "chart.js";

const props = defineProps<{
  executionLevels: { [tupleType: string]: string[] },
  levels: ExperimentSetup['fogDevices'],
}>()

const chartData = computed<ChartData<'bar'>>(() => {
  const tupleTypes = Object.keys(props.executionLevels);
  const levels = props.levels.map(d => d.level);
  const levelCounts = Object.entries(props.executionLevels)
      .reduce<Map<string, Map<string, number>>>((acc, [tupleType, levels]) => {
        levels.forEach(level => {
          acc.get(level)!.set(tupleType, acc.get(level)!.get(tupleType)! + 1)
        })
        return acc;
      }, new Map(levels.map(level => [
          level, new Map(tupleTypes.map(tupleType => [tupleType, 0]))
      ])))

  return {
    labels: tupleTypes,
    datasets: Array.from(levelCounts).map(([level, executionCountByTuple]) => ({
      type: 'bar',
      label: level,
      data: tupleTypes.map(tupleType => executionCountByTuple.get(tupleType)!)
    }))
  }
})


</script>

<style scoped>

</style>