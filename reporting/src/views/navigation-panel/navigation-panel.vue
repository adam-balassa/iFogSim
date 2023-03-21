<template>
  <h1>Navigation panel</h1>
</template>

<script setup lang="ts">
import useListExperiments from "./use-list-experiments";
import { watch } from "vue";

const { experiments } = useListExperiments()

watch(experiments, console.log)

</script>

<style scoped>

</style>