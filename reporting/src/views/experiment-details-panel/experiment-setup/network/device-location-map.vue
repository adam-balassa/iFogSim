<template>
  <div class="flex flex-column align-items-center">
    <h3 class="font-normal text-2xl mb-0">Device locations</h3>
    <div class="flex align-items-center py-3">
      <label class="font-normal text-xl pr-3" for="show-vehicles">Show vehicles</label>
      <InputSwitch v-model="showVehicles" input-id="show-vehicles" />
    </div>
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
        :title="device.name"
        @click="selectedDevice = device"
      >
        <InfoWindow
          :closeclick="true"
          :opened="selectedDevice?.id === device.id"
          @closeclick="selectedDevice = null"
        >
          <span>{{ device.name }}</span>
        </InfoWindow>
      </Marker>
    </Map>
  </div>
</template>

<script setup lang="ts">
import InputSwitch from "primevue/inputswitch";
import Map from '@fawmi/vue-google-maps/src/components/map.vue'
import Marker from '@fawmi/vue-google-maps/src/components/marker.vue'
import InfoWindow from '@fawmi/vue-google-maps/src/components/infoWindow.vue'
import VehicleSvg from '@/assets/vehicle.svg?url'
import BaseStationSvg from '@/assets/base-station.svg?url'
import ProxyServerSvg from '@/assets/proxy-server.svg?url'
import CloudSvg from '@/assets/cloud.svg?url'
import { ExperimentSetup } from "@/types/types";
import { computed, ref } from "vue";

const props = defineProps<{
  network: ExperimentSetup['network']
}>()

const showVehicles = ref(true)
const selectedDevice = ref<ExperimentSetup['network'][number] | null>(null)

const devices = computed(() => props.network
  .filter(device => device.location)
  .filter(device => showVehicles.value || device.level < 3)
)
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