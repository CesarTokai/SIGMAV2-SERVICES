import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';

import 'sweetalert2/dist/sweetalert2.min.css';

import 'leaflet/dist/leaflet.css';
import './utils/css/main.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// Títulos de rutas
router.beforeEach((to: any, from: any, next: any) => {
    document.title = (to.meta && to.meta.title) ? to.meta.title : 'SIGMAV2';
    next();
});

app.mount('#app');

