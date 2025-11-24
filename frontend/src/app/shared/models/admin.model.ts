export interface Admin {
  id: string;
  fullName: string;
  email: string;
  createdAt?: string;   // ISO string from backend (Instant)
  updatedAt?: string;   // ISO string from backend (Instant)
}

export interface CreateAdminDto {
  fullName: string;
  email: string;
}

export interface UpdateAdminDto {
  fullName: string;
  email: string;
}
