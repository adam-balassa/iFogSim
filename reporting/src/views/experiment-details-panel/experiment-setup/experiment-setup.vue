<template>
  <div>
    <div class="card flex justify-content-center">
      <SelectButton v-model="selectedOptions" :options="setupOptions" option-value="value" multiple>
        <template #option="slotProps">
          <i class="pi mr-2" :class="slotProps.option.icon"></i>
          <span>{{ slotProps.option.name }}</span>
        </template>
      </SelectButton>
    </div>
    <GeneralConfigTable :config="setup.config" class="pt-3"/>
    <template v-if="selectedOptions?.includes('network')">
      <NetworkGraph :network="setup.network"/>
      <FogDevicesTable :fog-devices="setup.fogDevices" class="pt-3"/>
      <SensorsTable :sensors="setup.sensors" class="pt-3"/>
      <ActuatorsTable :actuators="setup.actuators" class="pt-3"/>
      <DeviceLocationMap :network="setup.network" class="pt-3"/>
    </template>
    <template v-if="selectedOptions?.includes('application')">
      <ApplicationGraph :application="setup.application" class="pt-3"/>
      <AppModulesTable :app-modules="setup.application.modules" class="pt-3"/>
      <AppEdgeTable :app-edges="setup.application.edges" class="pt-3"/>
      <TupleCpuLengthChart v-if="setup.tupleTypeToCpuLength" :tuple-type-cpu-length="setup.tupleTypeToCpuLength" class="pt-3"/>
    </template>
  </div>
</template>

<script setup lang="ts">
import SelectButton from "primevue/selectbutton";
import { ExperimentDetails } from "../../../types/types";
import FogDevicesTable from "./network/fog-devices-table.vue";
import GeneralConfigTable from "./general-config-table.vue";
import SensorsTable from "./network/sensors-table.vue";
import ActuatorsTable from "./network/actuators-table.vue";
import ApplicationGraph from "./application/application-graph.vue";
import AppModulesTable from "./application/app-modules-table.vue";
import AppEdgeTable from "./application/app-edge-table.vue";
import { ref } from "vue";
import NetworkGraph from "./network/network-graph.vue";
import TupleCpuLengthChart from "./application/tuple-cpu-length-chart.vue";
import DeviceLocationMap from "./network/device-location-map.vue";

defineProps<{
  setup: ExperimentDetails['setup']
}>()

const selectedOptions = ref<('network' | 'application')[]>(['network', 'application'])
const setupOptions = [
  { name: 'Network', icon: 'pi-sitemap', value: 'network' },
  { name: 'Application', icon: 'pi-box', value: 'application' },
]

</script>

<style scoped>
</style>