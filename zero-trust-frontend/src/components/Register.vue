<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { registerApi } from "../service/authApi";
import type { RegisterDTO } from "../types/user";

const registerDTO = reactive<RegisterDTO>({
  username: "",
  password: "",
  confirmPassword: "",
});

const notification = reactive({
  show: false,
  message: "",
  color: "error",
});

const isSubmitting = ref(false);
const router = useRouter();

const notify = (message: string, color = "error") => {
  notification.message = message;
  notification.color = color;
  notification.show = true;
};

const handleRegister = async () => {
  if (registerDTO.password !== registerDTO.confirmPassword) {
    notify("Passwords do not match");
    return;
  }

  isSubmitting.value = true;

  try {
    await registerApi(registerDTO);

    notify("Registration successful! Please login.", "success");

    setTimeout(() => {
      router.push("/login");
    }, 1500);
  } catch (error) {
    notify(error instanceof Error ? error.message : "Registration failed");
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <v-container class="register-page" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="4">
        <v-card class="register-card pa-4" elevation="4" rounded="lg">
          <v-card-title class="text-h5 text-center font-weight-bold">
            System Registration
          </v-card-title>

          <v-card-subtitle class="text-center mb-6">
            Create a new secure account
          </v-card-subtitle>

          <v-card-text>
            <v-form @submit.prevent="handleRegister">
              <v-text-field
                v-model="registerDTO.username"
                label="Username"
                type="text"
                required
                variant="outlined"
                class="mb-2"
              ></v-text-field>

              <v-text-field
                v-model="registerDTO.password"
                label="Password"
                type="password"
                required
                variant="outlined"
                class="mb-2"
              ></v-text-field>

              <v-text-field
                v-model="registerDTO.confirmPassword"
                label="Confirm Password"
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
                :loading="isSubmitting"
              >
                Register
              </v-btn>

              <div class="d-flex align-center justify-center mt-6 text-body-2">
                <span class="text-medium-emphasis mr-2"
                  >Already have an account?</span
                >
                <v-btn
                  to="/login"
                  color="primary"
                  variant="text"
                  size="small"
                  class="text-none font-weight-bold px-2"
                >
                  Login here
                </v-btn>
              </div>
              
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
.register-page {
  min-height: 100svh;
  display: flex;
  align-items: center;
}

.register-card {
  width: 100%;
}
</style>
