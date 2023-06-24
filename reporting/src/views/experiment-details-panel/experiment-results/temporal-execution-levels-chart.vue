<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl mb-0">Execution levels</h3>
    <SelectButton v-model="tupleType" :options="Object.keys(executionLevels)" class="my-3"/>
    <Chart class="h-15rem mb-3" type="doughnut" :data="pieChartData" :options="{
      plugins: { tooltip: { callbacks: { footer: pieChartFooter } } } }"
    />
    <Chart v-if="chartData" class="w-full" type="line" :data="chartData" :options="{
      interaction: { mode: 'point' },
      y: { stacked: true },
      plugins: { tooltip: { callbacks: { footer: areaChartFooter } } } }
    "/>
    <Message v-else class="w-full" severity="error">Too much data</Message>
  </div>
</template>

<script setup lang="ts">
import Message from 'primevue/message'
import Chart from 'primevue/chart'
import SelectButton from 'primevue/selectbutton'
import { ExperimentSetup } from "@/types/types";
import { computed, toRef } from "vue";
import { ChartData, TooltipItem } from "chart.js";
import { bins } from "@/utils/stats";
import { useComputedRef } from "@/utils/helpers";
import _ from "lodash";

const props = defineProps<{
  executionLevels: { [tupleType: string]: [string, number][] },
  levels: ExperimentSetup['fogDevices'],
}>()

const tupleType = useComputedRef(
  computed(() => Object.keys(props.executionLevels)),
  executionLevels => executionLevels[0]
)

const chartData = computed<ChartData<'line', number[]> | null>(() => {
  const data = props.executionLevels[tupleType.value]!.map(([level, time]) => ({
    level,
    num: time
  }))
  if (data.length > 200_000) {
    return null
  }
  const partitions = bins(data, 10)
  const labels = partitions.map(([time]) => time.toFixed(0))
  return {
    labels,
    datasets: props.levels.map(level => ({
      label: level.level,
      data: partitions.map(([,partition]) =>
        partition.filter((execution) => execution.level === level.level).length
      ),
      fill: true
    }))
  }
})

const pieChartData = computed<ChartData<'doughnut'>>(() => {
  const levels = props.executionLevels[tupleType.value].map(([level]) => level)
  const levelCounts = _(levels).countBy()
  const labels = levelCounts.keys().value()
  const data = levelCounts.values().value()
  return {
    labels,
    datasets: [{
      data,
      hoverOffset: 4
    }]
  }
})

function pieChartFooter(tooltipItems: TooltipItem<'doughnut'>[]) {
  const sum = _(tooltipItems[0].dataset.data).sum()
  return `${(tooltipItems[0].parsed / sum * 100).toFixed(1)}%`
}

function areaChartFooter(tooltipItems: TooltipItem<'line'>[]) {
  const sum = _(chartData.value!.datasets).sumBy(item => item.data[tooltipItems[0].dataIndex])
  return `${(tooltipItems[0].parsed.y / sum * 100).toFixed(1)}%`
}
</script>

<style scoped>

</style>