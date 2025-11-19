import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api';
import { Employee, CreateEmployeeDto, UpdateEmployeeDto } from '../../shared/models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  constructor(private api: ApiService) {}

  getEmployees(): Observable<Employee[]> {
    return this.api.get<Employee[]>('/employees');
  }

  getEmployeeById(id: string): Observable<Employee> {
    return this.api.get<Employee>(`/employees/${id}`);
  }

  createEmployee(employee: CreateEmployeeDto): Observable<Employee> {
    return this.api.post<Employee>('/employees', employee);
  }

  updateEmployee(id: string, employee: UpdateEmployeeDto): Observable<Employee> {
    return this.api.put<Employee>(`/employees/${id}`, employee);
  }

  deleteEmployee(id: string): Observable<void> {
    return this.api.delete<void>(`/employees/${id}`);
  }

  autoAssign(): Observable<{ success: boolean; message: string }> {
    return this.api.post<{ success: boolean; message: string }>('/assignments/auto-assign', {});
  }

  // Mock data for development
  getMockEmployees(): Observable<Employee[]> {
    const mockEmployees: Employee[] = [
      { id: '1', name: 'John Smith', email: 'john@example.com', role: 'driver', assignedZones: ['Zone A'], status: 'active' },
      { id: '2', name: 'Sarah Johnson', email: 'sarah@example.com', role: 'driver', assignedZones: ['Zone B', 'Zone C'], status: 'on-route' },
      { id: '3', name: 'Mike Davis', email: 'mike@example.com', role: 'supervisor', assignedZones: ['Zone A', 'Zone B'], status: 'active' },
      { id: '4', name: 'Emily Brown', email: 'emily@example.com', role: 'driver', assignedZones: ['Zone C'], status: 'offline' }
    ];
    return of(mockEmployees);
  }
}
