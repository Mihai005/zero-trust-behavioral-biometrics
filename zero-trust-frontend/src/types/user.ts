export interface LoginDTO {
    username: string;
    password: string;
}

export interface AuthResponseDTO {
    token: string;
}

export interface RegisterDTO {
    username: string;
    password: string;
    confirmPassword: string;
}
