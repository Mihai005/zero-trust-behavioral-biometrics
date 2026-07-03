import type { AuthResponseDTO, LoginDTO, RegisterDTO } from "../types/user";

export const loginApi = async (loginDTO: LoginDTO) => {
  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: loginDTO.username,
        password: loginDTO.password,
      }),
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }

    return response.json() as Promise<AuthResponseDTO>;
  } catch (error: any) {
    throw new Error(error);
  }
};

export const registerApi = async (registerDTO: RegisterDTO) => {
  try {
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: registerDTO.username,
        password: registerDTO.password,
      }),
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }

    return response.status;
  } catch (error: any) {
    throw new Error(error);
  }
};
