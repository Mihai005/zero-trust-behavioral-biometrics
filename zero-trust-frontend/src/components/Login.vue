<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '../composables/useAuth';
import type { LoginDTO } from '../types/user';
import { loginApi } from '../service/authApi';

const loginDTO = reactive<LoginDTO>({
    username: '',
    password: ''
});

const notification = reactive({
  show: false,
  message: '',
  color: 'error',
});

const { setToken } = useAuth();
const isSubmitting = ref(false);
const router = useRouter();

const notify = (message: string, color = 'error') => {
  notification.message = message;
  notification.color = color;
  notification.show = true;
};

const handleLogin = async () => {
    isSubmitting.value = true;
    try {
      const response = await loginApi(loginDTO);
      setToken(response.token);
      await router.push('/dashboard');
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Login failed');
    } finally {
      isSubmitting.value = false;
    }
};
</script>

<template>
  <v-container class="login-page" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="4">
        
        <v-card class="login-card pa-4" elevation="4" rounded="lg">
          <v-card-title class="text-h5 text-center font-weight-bold">
            System Login
          </v-card-title>
          
          <v-card-subtitle class="text-center mb-6">
            Zero-Trust Workspace
          </v-card-subtitle>

          <v-card-text>
            <v-form @submit.prevent="handleLogin">
              
              <v-text-field
                v-model="loginDTO.username"
                label="Username"
                type="text"
                required
                variant="outlined"
                class="mb-2"
              ></v-text-field>

              <v-text-field
                v-model="loginDTO.password"
                label="Password"
                type="password"
                required
                variant="outlined"
                class="mb-4"
              ></v-text-field>

              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                elevation="2"
                class="mb-3"
                :loading="isSubmitting"
              >
                Authenticate
              </v-btn>

              <v-btn
                to="/register"
                color="primary"
                variant="tonal"
                block
                size="large"
              >
                Create an account
              </v-btn>

            </v-form>
          </v-card-text>
        </v-card>

      </v-col>
    </v-row>
  </v-container>

  <v-snackbar
    v-model="notification.show"
    :color="notification.color"
    location="top"
    timeout="3000"
  >
    {{ notification.message }}
  </v-snackbar>
</template>

<style scoped>
.login-page {
  min-height: 100svh;
  display: flex;
  align-items: center;
}

.login-card {
  width: 100%;
}
</style>
