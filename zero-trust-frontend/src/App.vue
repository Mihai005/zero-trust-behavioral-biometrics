<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useTheme } from 'vuetify';

type ThemeName = 'light' | 'dark';

const THEME_STORAGE_KEY = 'ui-theme';
const theme = useTheme();

const activeTheme = computed(() => theme.global.name.value as ThemeName);
const themeIcon = computed(() =>
  activeTheme.value === 'dark' ? 'mdi-weather-sunny' : 'mdi-weather-night',
);
const themeLabel = computed(() =>
  activeTheme.value === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
);

const applyTheme = (nextTheme: ThemeName) => {
  theme.global.name.value = nextTheme;
  document.documentElement.setAttribute('data-theme', nextTheme);
};

const toggleTheme = () => {
  const nextTheme = activeTheme.value === 'dark' ? 'light' : 'dark';
  applyTheme(nextTheme);
  localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
};

onMounted(() => {
  const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  const initialTheme: ThemeName = savedTheme === 'dark' ? 'dark' : 'light';
  applyTheme(initialTheme);
});
</script>

<template>
  <v-app>
    <v-btn
      class="theme-toggle"
      color="primary"
      variant="tonal"
      icon
      :aria-label="themeLabel"
      @click="toggleTheme"
    >
      <v-icon :icon="themeIcon" />
    </v-btn>

    <router-view />
  </v-app>
</template>

<style scoped>
.theme-toggle {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1200;
}
</style>
