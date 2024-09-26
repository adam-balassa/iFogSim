<template>
  <div class="overflow-y-auto max-h-screen">
    <Tree
      class="tree-navigation"
      selection-mode="multiple"
      meta-key-selection
      :value="experimentNodes"
      v-model:selection-keys="selectedKeys"
      @update:selection-keys="onSelect"
      v-model:expanded-keys="expandedKeys"
      :filter="true"
      filterMode="lenient"
    />
  </div>
</template>

<script setup lang="ts">
import Tree, { TreeExpandedKeys, TreeNode, TreeSelectionKeys } from 'primevue/tree';
import useListExperiments from "./use-list-experiments";
import { computed, ref, watch } from "vue";
import useSelectedExperiment, { experimentId, idToExperiment } from "../use-selected-experiment";
import _ from "lodash";
import { ExperimentListing } from "@/types/types";
import { confidenceInterval } from "@/utils/stats";

const { experiments } = useListExperiments()
const { selectedExperiment, selectExperiment } = useSelectedExperiment()
const selectedKeys = ref<TreeSelectionKeys>({})
const expandedKeys = ref<TreeExpandedKeys>({})

const experimentNodes = computed<TreeNode[] | undefined>(() => experiments.value?.map(app => ({
  key: app.app,
  label: app.app,
  icon: 'pi pi-fw pi-box',
  selectable: false,
  children: app.experiments.map(experiment => ({
    key: experimentId({ app: app.app, experiment }),
    label: experiment,
    type: app.app,
    icon: 'pi pi-fw pi-file',
    leaf: true,
    selectable: true
  }))
})))

watch(experiments, nextExperiments => {
  if (nextExperiments?.length && nextExperiments[0].experiments.length && !selectedExperiment.value) {
    const experiment = defaultExperiment(nextExperiments)
    select(experiment)
    expandedKeys.value = { [experiment.app]: true }
  }
})

function defaultExperiment(listing: ExperimentListing) {
  return _(listing)
      .flatMap(app => app.experiments.map(experiment => ({ app: app.app, experiment })))
      .sortBy(({ experiment }) => experiment )
      .last()!!
}

function select(id: { app: string, experiment: string }) {
  selectedKeys.value = { [experimentId(id)]: true }
  selectExperiment([id], true)
}

function onSelect(experimentIds: TreeSelectionKeys) {
  const experiments = Object.entries(experimentIds)
    .filter(([_, isSelected]) => isSelected)
    .map(([id]) => id)
    .map(idToExperiment)
  selectExperiment(experiments, experiments.length === 1)
}


</script>

<style scoped>

.tree-navigation {
    border: 0;
}

:deep(.p-treenode-leaf) {
    cursor: pointer;
}

:deep(*) {
    box-shadow: none!important;
}

</style>