<template>
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
</template>

<script setup lang="ts">
import Tree, { TreeExpandedKeys, TreeNode, TreeSelectionKeys } from 'primevue/tree';
import useListExperiments from "./use-list-experiments";
import { computed, ref, watch } from "vue";
import useSelectedExperiment from "../use-selected-experiment";
import { last } from "lodash";

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
    select(last(nextExperiments)!.app, last(last(nextExperiments)!.experiments)!)
  }
  if (nextExperiments) {
    const expandAll: TreeExpandedKeys = {};
    nextExperiments.forEach(experiment => {
      expandAll[experiment.app] = true
    })
    expandedKeys.value = expandAll;
  }
})

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