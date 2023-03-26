<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Summary</h3>
      <div class="w-fit">
        <DataTable
          :value="summary"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="metric"/>
          <Column field="value"/>
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
  results: ExperimentDetails['results']
}>()

const summary = computed(() => [
  { metric: 'Execution time (ms)', value: props.results.executionTime },
  { metric: 'Network usage (GB)', value: props.results.networkUsage },
  { metric: 'Migration delay (ms)', value: props.results.migrationDelay.toFixed(2) },
])

</script>