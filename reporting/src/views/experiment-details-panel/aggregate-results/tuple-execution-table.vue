<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Tuple execution times</h3>
      <div class="w-fit">
        <DataTable
          :value="tupleExecutions"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="tuple" header="Tuple"/>
          <Column field="cpuTime" header="CPU time">
            <template #body="slotProps">
              <span v-html="formatSetting(slotProps.data.cpuTime)"></span>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import { AggregateExperimentResults } from "@/types/types";
import { confidenceInterval } from "@/utils/stats";

const props = defineProps<{
  tupleExecutions: AggregateExperimentResults['tupleExecutionLatencies']
}>()


function formatSetting(sample: number[]): string {
  const [mean, interval] = confidenceInterval(sample)
  return `${mean.toFixed(2)} &#177; ${interval.toFixed(2)}`
}

</script>