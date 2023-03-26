<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Actuators</h3>
      <div class="w-fit">
        <DataTable
          :value="actuatorsConfig"
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
  actuators: ExperimentDetails['setup']['actuators']
}>()

const actuatorsConfig = computed(() => transposeConfigs(props.actuators, 'type'))

const columns = computed(() => props.actuators.map(actuator => actuator.type))
</script>