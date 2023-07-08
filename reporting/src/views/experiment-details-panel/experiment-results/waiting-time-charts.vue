<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl mb-0">Waiting times</h3>
    <Chart class="w-30rem" type="pie" :data="pieChartData" :options="{
      plugins: {
        legend: {
          labels: { generateLabels },
          onClick: function(mouseEvent, legendItem, legend) {
            legend.chart.getDatasetMeta(legendItem.datasetIndex).hidden = legend.chart.isDatasetVisible(legendItem.datasetIndex);
            legend.chart.update();
          }
        },
        tooltip: {
          callbacks: { label, title }
        }
      }
    }"/>
    <Chart class="w-full" type="bar" :data="chartData" :options="{
      x: { stacked: true, grid: { offset: false } },
      y: { stacked: true, grid: { offset: false } },
     }"/>
  </div>
</template>

<script setup lang="ts">
import Chart from 'primevue/chart';
import { ExperimentResults, ExperimentSetup } from "@/types/types";
import { computed } from "vue";
import {
  ChartData,
  Chart as ChartType,
  TooltipItem, Color
} from "chart.js";
import _ from "lodash";
import { mean } from "@/utils/stats";


const props = defineProps<{
  waitingTuples: NonNullable<ExperimentResults['waitingTuples']>,
  devices: ExperimentSetup['network'],
}>()

const chartData = computed<ChartData<'bar'>>(() => {
  return {
    labels: Object.keys(props.waitingTuples.byDeviceId).map(deviceId => props.devices.find(d => d.id === +deviceId)!.name),
    datasets: [
      {
        label: 'Devices',
        data: Object.values(props.waitingTuples.byDeviceId)
      },
    ]
  }
})


const pieChartData = computed<ChartData<'doughnut'>>(() => {
  const labels = [
    'uplink', 'downlink',
    ...Object.keys(props.waitingTuples.byLevel),
    ...Object.keys(props.waitingTuples.byTupleType),
  ]

  return {
    labels,
    datasets: [
      {
        label: 'By direction',
        backgroundColor: hslColors(0, 2),
        data: [props.waitingTuples.byDirection['true'], props.waitingTuples.byDirection['false']]
      },
      {
        label: 'By level',
        backgroundColor: hslColors(100, Object.keys(props.waitingTuples.byLevel).length),
        data: Object.values(props.waitingTuples.byLevel)
      },
      {
        label: 'By tuple type',
        backgroundColor: hslColors(180, Object.keys(props.waitingTuples.byTupleType).length),
        data: Object.values(props.waitingTuples.byTupleType)
      }
    ]
  }
})

function generateLabels(chart: ChartType) {
  // Get the default label list
  const original = ChartType.overrides.pie.plugins.legend.labels.generateLabels;
  const labelsOriginal = original(chart);

  // Build an array of colors used in the datasets of the chart
  const datasetColors = chart.data.datasets.map((e) => e.backgroundColor as Color).flat();
  const labelsToDatasetIndex = chart.data.datasets.flatMap((dataset, i) => dataset.data.map(() => i))

  // Modify the color and hide state of each label
  labelsOriginal.forEach(label => {
    // There are twice as many labels as there are datasets. This converts the label index into the corresponding dataset index
    label.datasetIndex = labelsToDatasetIndex[label.index!]

    // The hidden state must match the dataset's hidden state
    label.hidden = !chart.isDatasetVisible(label.datasetIndex);

    // Change the color to match the dataset
    const color = datasetColors[label.index!]
    if (color) {
      label.fillStyle = color;
    }
  });

  return labelsOriginal;
}

function label(item: TooltipItem<'doughnut'>) {
  const datasetIndexToLabelIndex = item.chart.data.datasets.reduce(([acc, prevCount], dataset) => {
    const nextAcc: [number[], number] = [[...acc, prevCount], prevCount + dataset.data.length]
    return nextAcc
  }, [[], 0] as [number[], number])[0]
  const labelIndex = datasetIndexToLabelIndex[item.datasetIndex] + item.dataIndex;
  return item.chart.data.labels![labelIndex] + ': ' + item.formattedValue;
}

function title([item]: TooltipItem<'doughnut'>[]) {
  return item.chart.data.datasets[item.datasetIndex].label
}

function hslColors(shade: number, n: number): string[] {
  return new Array<number>(n)
    .fill(0)
    .map((_, i) => 50 + i / n * 40)
    .map(intensity => `hsl(${shade}, 60%, ${intensity}%)`)
}


</script>

<style scoped>

</style>