<template>
  <main class="w-screen">
    <Splitter class="splitter max-w-full">
      <aside>
        <SplitterPanel :size="25" :min-size="20">
          <NavigationPanel/>
        </SplitterPanel>
      </aside>

      <SplitterPanel class="overflow-x-hidden" :size="75">
        <ExperimentDetailsPanel class="max-w-full"/>
      </SplitterPanel>
    </Splitter>
  </main>
</template>

<script setup lang="ts">
import Splitter from 'primevue/splitter'
import SplitterPanel from 'primevue/splitterpanel'
import NavigationPanel from "./views/navigation-panel/navigation-panel.vue";
import ExperimentDetailsPanel from "./views/experiment-details-panel/experiment-details-panel.vue";
</script>

<style scoped>
main {
    height: 100vh;
}

.splitter {
    height: 100%;
    border: 0;
}
</style>