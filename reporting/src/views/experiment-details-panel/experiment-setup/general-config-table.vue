<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Factors</h3>
      <div class="w-fit">
        <DataTable
          :value="configData"
          class="p-datatable-sm"
          stripedRows
          resizableColumns
          columnResizeMode="fit"
          scrollable
        >
          <Column field="metric" header="Name" class="font-semibold font-monospace"/>
          <Column field="value" header="Value"/>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import { ExperimentDetails } from "../../../types/types";
import { computed } from "vue";
import { transposeConfigs } from "../../../utils/helpers";

const props = defineProps<{
  config: ExperimentDetails['setup']['config']
}>()

const configData = computed(() => transposeConfigs([props.config]))

</script>