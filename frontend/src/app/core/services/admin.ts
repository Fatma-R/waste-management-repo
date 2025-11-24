// src/app/core/services/employee.service.ts

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api';
import { CreateEmployeeDto, UpdateEmployeeDto, Employee} from '../../shared/models/employee.model'
import { Admin, CreateAdminDto, UpdateAdminDto } from '../../shared/models/admin.model';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  constructor(private api: ApiService) {}

  getAdmins(): Observable<Admin[]> {
    // baseUrl = http://localhost:8080/api/v1
    // → GET http://localhost:8080/api/v1/admins
    return this.api.get<Admin[]>('/admins');
  }

  getAdminById(id: string): Observable<Admin> {
    return this.api.get<Admin>(`/admins/${id}`);
  }

  updateAdmin(id: string, payload: UpdateAdminDto): Observable<Admin> {
    // PUT /admins/{id} with { fullName, email }
    return this.api.put<Admin>(`/admins/${id}`, payload);
  }

  deleteAdmin(id: string): Observable<void> {
    return this.api.delete<void>(`/admins/${id}`);
  }
}
