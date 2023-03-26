<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">App edges / tuples</h3>
      <div class="w-fit">
        <DataTable
          :value="appEdgesConfig"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="metric" header="Metric" class="font-semibold font-monospace"/>
          <Column v-for="col in columns" :field="col" :header="col"/>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import { ExperimentDetails } from "../../../../types/types";
import { computed } from "vue";
import { transposeConfigs } from "../../../../utils/helpers";

const props = defineProps<{
  appEdges: ExperimentDetails['setup']['application']['edges']
}>()

const appEdgesConfig = computed(() => transposeConfigs(props.appEdges, 'tuple')
    .filter(config => !['direction', 'from', 'to'].includes(config.metric)))

const columns = computed(() => props.appEdges.map(edge => edge.tuple))
</script>