<template>
  <div class="overflow-y-auto max-h-screen">
    <Tree
      class="tree-navigation"
      selection-mode="single"
      :value="experimentNodes"
      v-model:selection-keys="selectedKeys"
      v-model:expanded-keys="expandedKeys"
      :filter="true"
      filterMode="lenient"
      @node-select="onSelectExperiment"
    />
  </div>
</template>

<script setup lang="ts">
import Tree, { TreeExpandedKeys, TreeNode, TreeSelectionKeys } from 'primevue/tree';
import useListExperiments from "./use-list-experiments";
import { computed, ref, watch } from "vue";
import useSelectedExperiment from "../use-selected-experiment";
import _ from "lodash";
import { ExperimentListing } from "../../types/types";

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
    key: experiment,
    label: experiment,
    type: app.app,
    icon: 'pi pi-fw pi-file',
    leaf: true,
    selectable: true
  }))
})))

watch(experiments, nextExperiments => {
  if (nextExperiments?.length && nextExperiments[0].experiments.length && !selectedExperiment.value) {
    const { app, experiment } = defaultExperiment(nextExperiments)
    select(app, experiment)
    expandedKeys.value = { [app]: true }
  }
})

function defaultExperiment(listing: ExperimentListing) {
  return _(listing)
      .flatMap(app => app.experiments.map(experiment => ({ app: app.app, experiment })))
      .sortBy(({ experiment }) => experiment )
      .last()!!
}

function select(app: string, experiment: string) {
  selectedKeys.value = { [experiment]: true }
  selectExperiment(app, experiment)
}

function onSelectExperiment(node: TreeNode) {
  if (node.type && node.key) {
    selectExperiment(node.type, node.key)
  }
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