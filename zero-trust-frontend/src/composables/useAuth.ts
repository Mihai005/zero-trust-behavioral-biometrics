import { computed, ref } from "vue";
import { resetBiometricState } from './useBiometricStream';

const token = ref<string | null>(sessionStorage.getItem("jwt_token"));

export const isValidJwt = (value: string | null): value is string => {
  if (!value) {
    return false;
  }

  const parts = value.split('.');
  if (parts.length !== 3) {
    return false;
  }

  try {
    const payload = JSON.parse(atob(parts[1]));

    if (typeof payload.exp === 'number') {
      return payload.exp * 1000 > Date.now();
    }

    return true;
  } catch {
    return false;
  }
};

export const getAuthHeader = (): Record<string, string> => {
  return isValidJwt(token.value) ? { Authorization: `Bearer ${token.value}` } : {};
};

export function useAuth() {
  const isAuthenticated = computed(() => isValidJwt(token.value));

  const setToken = (nextToken: string) => {
    resetBiometricState();
    token.value = nextToken;
    sessionStorage.setItem("jwt_token", nextToken);
  }

  const logout = () => {
    resetBiometricState();
    token.value = null;
    sessionStorage.removeItem("jwt_token");
  };

  return { token, isAuthenticated, setToken, logout };
}
