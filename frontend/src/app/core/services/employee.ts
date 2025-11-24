// src/app/core/services/employee.service.ts

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api';
import { CreateEmployeeDto, UpdateEmployeeDto, Employee} from '../../shared/models/employee.model'

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  constructor(private api: ApiService) {}

  getEmployees(): Observable<Employee[]> {
    // baseUrl = http://localhost:8080/api/v1
    // → GET http://localhost:8080/api/v1/employees
    return this.api.get<Employee[]>('/employees');
  }

  getEmployeeById(id: string): Observable<Employee> {
    return this.api.get<Employee>(`/employees/${id}`);
  }

  createEmployee(payload: CreateEmployeeDto): Observable<Employee> {
    // POST /employees with { fullName, email, skill }
    return this.api.post<Employee>('/employees', payload);
  }

  updateEmployee(id: string, payload: UpdateEmployeeDto): Observable<Employee> {
    // PUT /employees/{id} with { fullName, email, skill }
    return this.api.put<Employee>(`/employees/${id}`, payload);
  }

  deleteEmployee(id: string): Observable<void> {
    return this.api.delete<void>(`/employees/${id}`);
  }

  // Keep this for when backend has it; UI will just 404 if clicked for now.
  autoAssign(): Observable<{ success: boolean; message: string }> {
    return this.api.post<{ success: boolean; message: string }>(
      '/assignments/auto-assign',
      {}
    );
  }

  // If you want to keep mocks for local dev, you can keep a method like this,
  // but it's no longer used by the EmployeesComponent:
  
  getMockEmployees(): Observable<Employee[]> {
    const mockEmployees: Employee[] = [
      { id: '1', fullName: 'John Smith', email: 'john@example.com', skill: 'DRIVER' },
      { id: '2', fullName: 'Sarah Johnson', email: 'sarah@example.com', skill: 'AGENT' }
    ];
    return of(mockEmployees);
  }
  
}
