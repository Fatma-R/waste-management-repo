import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { EmployeeService } from '../../core/services/employee';
import { AuthService } from '../../core/auth/auth.service'; // <-- add this
import { NotificationService } from '../../core/services/notification';
import { CreateEmployeeDto, Employee, EmployeeSkill, UpdateEmployeeDto } from '../../shared/models/employee.model';


@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './employees.html',
  styleUrls: ['./employees.scss']
})
export class EmployeesComponent implements OnInit {
  employees: Employee[] = [];
  selectedEmployee: Employee | null = null;
  isLoading = true;

  // Create/Edit form modal
  isEmployeeFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingEmployeeId: string | null = null;

  // Form model (aligned with backend DTO)
  employeeForm: CreateEmployeeDto = {
    fullName: '',
    email: '',
    skill: 'DRIVER'
  };

  // Filter: by skill
  filterSkill: 'all' | EmployeeSkill = 'all';

  constructor(
    private employeeService: EmployeeService,
    private authService: AuthService, 
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.isLoading = true;

    this.employeeService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.notificationService.showToast('Failed to load employees', 'error');
        this.isLoading = false;
      }
    });
  }

  get filteredEmployees(): Employee[] {
    return this.employees.filter(emp => {
      const skillMatch = this.filterSkill === 'all' || emp.skill === this.filterSkill;
      return skillMatch;
    });
  }

  // -----------------
  // CREATE / EDIT
  // -----------------

  openAddEmployeeModal(): void {
    this.formMode = 'create';
    this.resetEmployeeForm();
    this.isEmployeeFormModalOpen = true;
  }

  openEditEmployeeModal(employee: Employee): void {
    if (!employee) return;

    this.formMode = 'edit';
    this.editingEmployeeId = employee.id;

    const dto: UpdateEmployeeDto = {
      fullName: employee.fullName,
      email: employee.email,
      skill: employee.skill
    };

    this.employeeForm = { ...dto };

    this.deselectEmployee();
    this.isEmployeeFormModalOpen = true;
  }

  closeEmployeeFormModal(): void {
    this.isEmployeeFormModalOpen = false;
    this.editingEmployeeId = null;
    this.resetEmployeeForm();
  }

  resetEmployeeForm(): void {
    this.employeeForm = {
      fullName: '',
      email: '',
      skill: 'DRIVER'
    };
  }

  onSubmitEmployeeForm(): void {
    if (this.formMode === 'create') {
      // ✅ Use AuthService.signup instead of EmployeeService
      const signupPayload = {
        fullName: this.employeeForm.fullName,
        email: this.employeeForm.email,
        skill: this.employeeForm.skill,
        roles: ['user'] // optional
      };

      console.log('Signup payload', signupPayload);

      this.authService.signup(signupPayload).subscribe({
        next: () => {
          this.notificationService.showToast('Employee added successfully', 'success');
          this.loadEmployees(); // reload list since signup might not return the new employee
          this.closeEmployeeFormModal();
        },
        error: (err) => {
          console.error('Error adding employee:', err);
          this.notificationService.showToast('Failed to add employee', 'error');
        }
      });

      return;
    }

    // --- keep EDIT branch as-is ---
    if (!this.editingEmployeeId) {
      console.error('No employee ID to edit');
      return;
    }

    const payload: UpdateEmployeeDto = { ...this.employeeForm };

    this.employeeService.updateEmployee(this.editingEmployeeId, payload).subscribe({
      next: (updated) => {
        this.employees = this.employees.map(e =>
          e.id === this.editingEmployeeId ? { ...e, ...updated } : e
        );
        this.notificationService.showToast('Employee updated successfully', 'success');
        this.closeEmployeeFormModal();
      },
      error: (err) => {
        console.error('Error updating employee:', err);
        this.notificationService.showToast('Failed to update employee', 'error');
      }
    });
  }

  // -----------------
  // DETAILS / DELETE
  // -----------------

  selectEmployee(employee: Employee): void {
    this.selectedEmployee = employee;
  }

  deselectEmployee(): void {
    this.selectedEmployee = null;
  }

  onDeleteEmployee(employeeId: string): void {
    if (!confirm('Are you sure you want to delete this employee?')) return;

    this.employeeService.deleteEmployee(employeeId).subscribe({
      next: () => {
        this.employees = this.employees.filter(e => e.id !== employeeId);
        if (this.selectedEmployee?.id === employeeId) this.deselectEmployee();
        this.notificationService.showToast('Employee deleted successfully', 'success');
      },
      error: (err) => {
        console.error('Error deleting employee:', err);
        this.notificationService.showToast('Failed to delete employee', 'error');
      }
    });
  }

  // -----------------
  // UI HELPERS
  // -----------------

  getSkillLabel(skill: EmployeeSkill): string {
    switch (skill) {
      case 'DRIVER': return 'Driver';
      case 'AGENT': return 'Agent';
      default: return skill;
    }
  }

  getSkillIcon(skill: EmployeeSkill): string {
    switch (skill) {
      case 'DRIVER': return '🚛';
      case 'AGENT': return '🧹';
      default: return '👤';
    }
  }

  formatDate(iso?: string): string {
    if (!iso) return 'N/A';
    return new Date(iso).toLocaleString();
  }

  goToDashboard(): void {
    const path = this.authService.isAdmin()
      ? '/admin/dashboard'
      : this.authService.isUser()
      ? '/user/dashboard'
      : '/landing';
    this.router.navigate([path]);
  }
}
