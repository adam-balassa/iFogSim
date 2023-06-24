<template>
  <Panel header="QoS criterion">
    <span class="text-lg">P(latency&nbsp;&lt;&nbsp;</span>
    <InputNumber
      v-model="qosLatency"
      class="p-2"
      :max-fraction-digits="0"
      suffix=" ms"
      @update:model-value="l => qosProbability = nextProbability(l)"
    />
    <span class="text-lg">)&nbsp;=&nbsp;</span>
    <InputNumber
      v-model="qosProbability"
      class="p-2"
      :max-fraction-digits="3"
      @update:model-value="p => qosLatency = nextLatency(p)"/>
  </Panel>
  <Chart class="w-full" type="scatter" :data="datasets" :options="{interaction: { mode: 'x' }}"/>
</template>

<script setup lang="ts">
import InputNumber from 'primevue/inputnumber';
import Panel from 'primevue/panel';
import Chart from 'primevue/chart';
import { computed } from "vue";
import {ChartData} from "chart.js";
import { CDF, mean } from "@/utils/stats";
import { useComputedRef } from "@/utils/helpers";

const props = defineProps<{
  title: string;
  datasets: {
    title: string;
    latencies: [];
  }[]
}>()

const latencies = computed(() => props.datasets.flatMap(dataset => dataset.latencies))
const cdf = computed(() => CDF(latencies.value))
const qosLatency = useComputedRef(latencies, mean)
const qosProbability = useComputedRef(latencies, () => nextProbability(qosLatency.value))

const datasets = computed<ChartData<'scatter'>>(() => ({
  datasets: props.datasets.map(dataset => {
    const cdf = CDF(dataset.latencies)
    return {
      displayName: dataset.title,
      label: dataset.title,
      showLine: true,
      data: cdf,
      tension: .1
    }
  })
}))

function nextProbability(latency: number) {
  return latencies.value.filter(l => l < latency).length / latencies.value.length
}

function nextLatency(probability: number) {
  const y = probability * latencies.value.length
  return cdf.value.find(point => point.y >= y)!.x
}



</script>

<style scoped>
:deep(.p-inputnumber-input) {
    width: 7rem;
}
</style>