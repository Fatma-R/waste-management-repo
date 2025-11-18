export interface User {
  id?: string;
  fullName: string;
  email: string;
  password?: string;
  roles?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface JwtResponse {
  token: string;
  email: string;
  roles: string[];
  message: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  fullName?: string;
  roles?: string[];
}