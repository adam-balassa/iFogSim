import { createApp } from 'vue'
import App from './App.vue'
import PrimeVue from 'primevue/config';
import "primevue/resources/primevue.css";
import "primevue/resources/themes/lara-light-indigo/theme.css";
import "primeflex/primeflex.css";
import 'primeicons/primeicons.css';
import './style.css'
import VueGoogleMaps from '@fawmi/vue-google-maps'

const app = createApp(App)

app.use(PrimeVue)
app.use(VueGoogleMaps, {
  load: {
    key: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
  },
})

app.mount('#app')
