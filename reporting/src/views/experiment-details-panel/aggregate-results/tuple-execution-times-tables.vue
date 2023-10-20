<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Tuple execution times</h3>
      <div class="grid">
        <div v-for="{ tupleType, executionTimes } in tableData" class="col">
          <h4 class="font-normal text-xl">{{ tupleType }}</h4>
          <DataTable
            :value="executionTimes"
            class="p-datatable-sm"
            stripedRows
            resizableColumns
            columnResizeMode="fit"
            scrollable
            :title="tupleType"
          >
            <Column field="level" header="Level"/>
            <Column field="time" header="CPU time">
              <template #body="slotProps">
                <span v-html="formatSetting(slotProps.data.time)"></span>
              </template>
            </Column>
          </DataTable>
        </div>
        </div>
      </div>
      <div class="w-fit">

    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import { AggregateExperimentResults, ExperimentDetails } from "@/types/types";
import { computed } from "vue";
import { confidenceInterval } from "@/utils/stats";

const props = defineProps<{
  executedTuples: Exclude<AggregateExperimentResults['executedTuples'], undefined>;
  tupleExecutions: AggregateExperimentResults['tupleExecutionLatencies']
}>()

const tableData = computed(() => Object.entries(props.executedTuples)
  .map(([tupleType, executionTimes]) => ({
    tupleType,
    executionTimes: [
      ...Object.entries(executionTimes).map(([level, time]) => ({ level, time })),
      { level: 'average', time: props.tupleExecutions.find(e => e.tuple === tupleType)?.cpuTime ?? [] }
    ]
  }))
)

function formatSetting(sample: number[]): string {
  const [mean, interval] = confidenceInterval(sample)
  return `${mean.toFixed(2)} &#177; ${interval.toFixed(2)}`
}

</script>