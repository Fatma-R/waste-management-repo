export interface User {
  id?: string;
  fullName: string;
  email: string;
  password?: string;
  createdAt?: string;      // ISO date string returned by backend
  updatedAt?: string;      // ISO date string returned by backend
}
