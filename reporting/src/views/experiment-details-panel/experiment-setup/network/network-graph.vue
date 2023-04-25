<template>
  <div>
    <div class="flex flex-column align-items-center">
      <h3 class="font-normal text-2xl">Network graph</h3>
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
  network: ExperimentDetails['setup']['network']
}>()

const networkContainer = ref<HTMLDivElement>()

const nodes = computed(() => new DataSet(props.network.map(el => ({
  id: el.id,
  group: el.cluster?.toString() ?? el.group ?? el.level.toString(),
  label: el.name,
  ...(Number.isInteger(el.level) ? { level: Number(el.level) } : {})
}))))
const edges = computed(() => new DataSet(props.network.map(el => ({ id: el.id, from: el.parent, to: el.id }))))

watch([networkContainer, nodes], ([container]) => {
  if (container) {
    new Network(container, { edges: edges.value, nodes: nodes.value }, {
      layout: {
        hierarchical: {
          direction: 'UD',
          shakeTowards: 'leaves',
          sortMethod: 'directed'
        },
      }
    })
  }
}, { immediate: true })

</script>

<style scoped>
.network {
    cursor: all-scroll;
    height: 300px;
    width: 100%
}
</style>