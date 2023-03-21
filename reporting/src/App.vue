<template>
  <main>
    <Splitter class="splitter">
      <aside>
        <SplitterPanel :size="20" :min-size="20">
          <NavigationPanel/>
        </SplitterPanel>
      </aside>

      <SplitterPanel :size="80">
        <h1>Experiment details</h1>
      </SplitterPanel>
    </Splitter>
  </main>
</template>

<script setup lang="ts">
import Splitter from 'primevue/splitter'
import SplitterPanel from 'primevue/splitterpanel'
import NavigationPanel from "./views/navigation-panel/navigation-panel.vue";
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