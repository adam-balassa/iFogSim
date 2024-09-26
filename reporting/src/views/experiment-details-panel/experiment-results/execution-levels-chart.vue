<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl mb-0">Execution levels</h3>
    <div class="flex align-items-center">
      <div class="flex align-items-center p-3">
        <label class="font-normal text-xl pr-2" for="normalize">Normalize</label>
        <InputSwitch v-model="normalize" input-id="normalize" />
      </div>
      <MultiSelect
        v-model="visualizedTupleTypes"
        :options="tupleTypes"
        placeholder="Visualized tuple types"
        class="w-20rem my-3"
      />
    </div>

    <Chart class="w-full" type="bar" :data="chartData" :options="{
      x: { stacked: true, grid: { offset: false } },
      y: { stacked: true, grid: { offset: false } },
     }"/>
  </div>
</template>

<script setup lang="ts">
import InputSwitch from 'primevue/inputswitch';
import MultiSelect from 'primevue/multiselect';
import Chart from 'primevue/chart';
import { ExperimentSetup } from "@/types/types";
import { computed, ref, toRef, watch } from "vue";
import { ChartData } from "chart.js";
import _, { max, sumBy } from "lodash";

const props = defineProps<{
  executionLevels: { [tupleType: string]: string[] },
  levels: ExperimentSetup['fogDevices'],
}>()

const visualizedTupleTypes = ref(Object.keys(props.executionLevels))
const tupleTypes = computed(() => Object.keys(props.executionLevels))
watch(tupleTypes, nextLevels => {
  if (visualizedTupleTypes.value.some(t => !tupleTypes.value.includes(t)))
    visualizedTupleTypes.value = Object.keys(nextLevels)
})

const normalize = ref(true)

const chartData = computed<ChartData<'bar'>>(() => {
  const tupleTypes = visualizedTupleTypes.value;
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

  if (normalize.value) {
    levelCounts.forEach((executionCountByTuple, level) => {
      executionCountByTuple.forEach((counts, tupleType) => {

      })
    })
  }
  const sumByTupleType = new Map<string, number>(_(Array.from(levelCounts.values()))
    .flatMap(c => [...c.entries()])
    .groupBy(([tuple]) => tuple)
    .map<[string, number]>((counts, tuple) => [tuple, normalize.value ? sumBy(counts, ([, count]) => count) : 1])
    .value()
  )

  return {
    labels: tupleTypes,
    datasets: Array.from(levelCounts).map(([level, executionCountByTuple]) => ({
      type: 'bar',
      label: level,
      data: tupleTypes.map(tupleType => executionCountByTuple.get(tupleType)! / sumByTupleType.get(tupleType)!)
    }))
  }
})


</script>

<style scoped>

</style>