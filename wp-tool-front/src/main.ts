import './assets/main.css'

import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import App from './App.vue'

import 'notiflix/dist/notiflix-3.2.8.min.css'

const vuetify = createVuetify({
  components,
  directives,
})
/**
 * Vueアプリケーションを起動する
 */
createApp(App).use(vuetify).mount('#app')
