<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Fog devices</h3>
      <div class="w-fit">
        <DataTable
          :value="fogDeviceProperties"
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
  fogDevices: ExperimentDetails['setup']['fogDevices']
}>()

const groupBy = computed(() => props.fogDevices[0].type != null ? 'type' : 'level')

const fogDeviceProperties = computed(() => transposeConfigs(props.fogDevices, groupBy.value)
)

const columns = computed(() => props.fogDevices.map(device => device[groupBy.value]))
</script>