<template>
  <Map
    :class="devices.length"
    class="map"
    :center="devices[0].location"
    :zoom="15"
    :options="{
      styles: [
        {
          featureType: 'poi',
          stylers: [
            { visibility: 'off' }
          ]
        },
        {
          featureType: 'transit',
          stylers: [
            { visibility: 'off' }
          ]
        }
      ]
    }"
  >
    <Marker
      v-for="device in devices"
      :position="device.location"
      :icon="icon(device)"
    />
  </Map>
</template>

<script setup lang="ts">
import Map from '@fawmi/vue-google-maps/src/components/map.vue'
import Marker from '@fawmi/vue-google-maps/src/components/marker.vue'
import VehicleSvg from '@/assets/vehicle.svg?url'
import BaseStationSvg from '@/assets/base-station.svg?url'
import ProxyServerSvg from '@/assets/proxy-server.svg?url'
import CloudSvg from '@/assets/cloud.svg?url'
import { ExperimentSetup } from "@/types/types";
import { computed } from "vue";

const props = defineProps<{
  network: ExperimentSetup['network']
}>()

const devices = computed(() => props.network.filter(device => device.location))
function icon(device: ExperimentSetup['network'][number]) {
  switch (device.level) {
    case 3:
      return VehicleSvg;
    case 2:
      return BaseStationSvg
    case 1:
      return ProxyServerSvg
    case 0:
      return CloudSvg
    default:
      return null
  }
}

</script>

<style scoped>
.map {
    width: 100%;
    height: 500px;
}
</style>