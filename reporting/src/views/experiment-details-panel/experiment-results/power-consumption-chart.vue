<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl">Energy consumptions</h3>
    <div class="grid w-full">
      <div class="col-6" v-for="dataset of chartData.datasets" >
        <Chart type="scatter" :data="{ datasets: [dataset] }"/>
      </div>
    </div>
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
  powerConsumptions: ExperimentResults['fogDeviceEnergyConsumptions']
}>()

const chartData = computed<ChartData<'scatter'>>(() => ({
  datasets: _(props.powerConsumptions)
      .groupBy(d => d.group)
      .map(group => ({
        label: group[0].group,
        showLine: true,
        data: CDF(group.map(d => d.energy)),
        tension: .4
      })).value()
}))


</script>

<style scoped>

</style>