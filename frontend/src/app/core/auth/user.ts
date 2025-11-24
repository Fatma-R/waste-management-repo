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
  //passwords are generated randomly through the signup endpoint
  //password: string;
  fullName?: string;  
  roles?: string[];
  skill?: string;
}