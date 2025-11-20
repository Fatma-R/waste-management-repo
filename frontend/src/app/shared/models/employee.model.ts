export interface Employee {
  id: string;
  name: string;
  email: string;
  role: 'driver' | 'supervisor' | 'admin';
  assignedZones: string[];
  status: 'active' | 'offline' | 'on-route';
  phoneNumber?: string;
  createdAt?: Date;
}

export interface CreateEmployeeDto {
  name: string;
  email: string;
  role: Employee['role'];
  assignedZones?: string[];
  phoneNumber?: string;
}

export interface UpdateEmployeeDto extends Partial<CreateEmployeeDto> {
  status?: Employee['status'];
}
