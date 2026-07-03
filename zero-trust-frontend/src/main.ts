import { createApp } from 'vue';
import './style.css';
import App from './App.vue';
import 'vuetify/styles';
import '@mdi/font/css/materialdesignicons.css';
import { createVuetify } from 'vuetify';
import * as components from 'vuetify/components';
import * as directives from 'vuetify/directives';
import router from './router';

const THEME_STORAGE_KEY = 'ui-theme';
const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
const initialTheme = storedTheme === 'dark' ? 'dark' : 'light';
document.documentElement.setAttribute('data-theme', initialTheme);

const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: initialTheme,
  },
});

const app = createApp(App);
app.use(vuetify);
app.use(router);
app.mount('#app');
