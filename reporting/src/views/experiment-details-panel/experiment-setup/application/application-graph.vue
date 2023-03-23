<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Application graph</h3>
      <div class="network" ref="networkContainer"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExperimentDetails } from "../../../../types/types";
import { computed, ref, watch } from "vue";
import { DataSet } from "vis-data";
import { Network } from "vis-network";

const props = defineProps<{
  application: ExperimentDetails['setup']['application']
}>()

const networkContainer = ref<HTMLDivElement>()

const moduleNodeIdMap = computed<Map<string, number>>(() => new Map(
    Array.from(
      new Set(props.application.edges
        .flatMap(edge => [edge.from, edge.to])))
        .map((edge, i) => [edge, i])))

const nodes = computed(() => new DataSet(Array.from(moduleNodeIdMap.value).map(([key, value]) => ({
  id: value,
  label: key,
  group: props.application.modules.find(m => m.name === key) ? 0 : 1
}))))

const edges = computed(() => new DataSet(props.application.edges.map((edge, i) => ({
  id: i,
  from: moduleNodeIdMap.value.get(edge.from),
  to: moduleNodeIdMap.value.get(edge.to),
  label: edge.tuple?.toString(),
  arrows: {
    to: {
      enabled: true,
      arrow: 'arrow'
    }
  }
}))))

watch([networkContainer, moduleNodeIdMap], ([container]) => {
  if (container) {
    new Network(container, { edges: edges.value, nodes: nodes.value }, {})
  }
}, { immediate: true })

</script>

<style scoped>
.network {
    max-width:700px;
    cursor: all-scroll;
    /*background-color: white;*/
    /*border-radius: 6px;*/
    /*border: 1px solid #dee2e6*/
}
</style>