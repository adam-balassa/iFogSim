<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Tuple execution times</h3>
      <div class="w-fit">
        <DataTable
          :value="tableData"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="tuple" header="Tuple"/>
          <Column field="cpuTime" header="CPU time"/>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import { computed } from "vue";
import { ExperimentDetails } from "../../../types/types";

const props = defineProps<{
  tupleExecutions: ExperimentDetails['results']['tupleExecutionLatencies']
}>()

const tableData = computed(() => props.tupleExecutions.map(t => ({
  ...t,
  cpuTime: t.cpuTime.toFixed(3)
})))

</script>