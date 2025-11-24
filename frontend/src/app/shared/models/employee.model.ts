export type EmployeeSkill = 'DRIVER' | 'AGENT';

export interface Employee {
  id: string;
  fullName: string;
  email: string;
  skill: EmployeeSkill;
  createdAt?: string;   // ISO string from backend (Instant)
  updatedAt?: string;   // ISO string from backend (Instant)

  // Future fields (UI-only for later evolutions)
  // status?: 'active' | 'offline' | 'on-route';
  // assignedZones?: string[];
  // phoneNumber?: string;
}

export interface CreateEmployeeDto {
  fullName: string;
  email: string;
  skill: EmployeeSkill;
}

export interface UpdateEmployeeDto {
  fullName: string;
  email: string;
  skill: EmployeeSkill;
}
