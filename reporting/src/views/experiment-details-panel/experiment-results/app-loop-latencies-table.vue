<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Average E2E latencies</h3>
      <div class="w-fit">
        <DataTable
          :value="tableData"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="appLoop" header="App loop"/>
          <Column field="avgLatency" header="Average latency"/>
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
  appLoopLatencies: ExperimentDetails['results']['appLoopLatencies']
}>()

const tableData = computed(() => props.appLoopLatencies.map(a => ({
  ...a,
  appLoop: a.appLoop.join(' > ')
})))

</script>