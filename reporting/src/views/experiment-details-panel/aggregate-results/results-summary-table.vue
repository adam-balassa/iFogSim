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
          <Column field="value">
            <template #body="slotProps">
              <span v-html="formatSetting(slotProps.data.value)"></span>
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
import { computed } from "vue";
import AggregateResults from "@/views/experiment-details-panel/aggregate-results/aggregate-results.vue";
import { confidenceInterval } from "@/utils/stats";

const props = defineProps<{
  results: AggregateResults['results']
}>()

const summary = computed(() => [
  { metric: 'Execution time (ms)', value: props.results.executionTime },
  { metric: 'Network usage (MB)', value: props.results.networkUsage },
  { metric: 'Migration delay (ms)', value: props.results.migrationDelay },
])

function formatSetting(sample: number[]): string {
  const [mean, interval] = confidenceInterval(sample)
  return `${mean.toFixed(2)} &#177; ${interval.toFixed(2)}`
}

</script>