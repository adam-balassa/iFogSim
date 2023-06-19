<template>
  <div class="min-h-full surface-ground px-7 py-4 font-light">
    <template v-if="selectedExperiment">
      <h1 class="font-normal mb-2">{{ selectedExperiment.app }}</h1>
      <h2 class="text-color-secondary font-light mt-0">{{ selectedExperiment.experiment }}</h2>
      <TabView>
        <TabPanel>
          <template #header>
            <i class="pi pi-sliders-h mr-3"></i>
            <span>Setup</span>
          </template>
          <ExperimentSetup :setup="selectedExperiment.setup" :type="selectedExperiment.type"/>
        </TabPanel>
        <TabPanel v-if="selectedExperiment.type === 'single'">
          <template #header>
            <i class="pi pi-chart-bar mr-3"></i>
            <span>Results</span>
          </template>
          <ExperimentResults :results="selectedExperiment.results" :setup="selectedExperiment.setup"/>
        </TabPanel>
        <TabPanel v-else>
          <template #header>
            <i class="pi pi-chart-bar mr-3"></i>
            <span>Aggregate</span>
          </template>
          <AggregateResults :experiments="selectedExperiment" />
        </TabPanel>
      </TabView>
    </template>
  </div>
</template>

<script setup lang="ts">
import TabView from "primevue/tabview";
import TabPanel from "primevue/tabpanel";
import ExperimentSetup from "./experiment-setup/experiment-setup.vue";
import useSelectedExperiment from "../use-selected-experiment";
import ExperimentResults from "./experiment-results/experiment-results.vue";
import AggregateResults from "@/views/experiment-details-panel/aggregate-results/aggregate-results.vue";

const { selectedExperiment } = useSelectedExperiment()

</script>

<style scoped>
:deep(.p-tabview-nav), :deep(.p-tabview-nav *), :deep(.p-tabview-panels) {
    background-color: transparent!important;
}

:deep(a:focus) {
    box-shadow: none!important;
}
</style>