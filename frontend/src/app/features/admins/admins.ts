import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/services/notification';
import { Admin, CreateAdminDto, UpdateAdminDto } from '../../shared/models/admin.model';
import { AdminService } from '../../core/services/admin';

@Component({
  selector: 'app-admins',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './admins.html',
  styleUrls: ['./admins.scss']
})
export class AdminsComponent implements OnInit {
  admins: Admin[] = [];
  selectedAdmin: Admin | null = null;
  isLoading = true;

  // Who is logged in (email)
  currentAdminEmail: string | null = null;

  // Create/Edit form modal
  isAdminFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingAdminId: string | null = null;

  // Form model (aligned with backend DTO)
  adminForm: CreateAdminDto = {
    fullName: '',
    email: ''
  };

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.setCurrentAdminFromAuth();
    this.loadAdmins();
  }

  private setCurrentAdminFromAuth(): void {
    // Adapt this to how your AuthService works
    // Using "any" to avoid TS errors if getCurrentUser isn't typed
    const anyAuth = this.authService as any;
    const currentUser = anyAuth.getCurrentUser ? anyAuth.getCurrentUser() : null;

    if (currentUser && currentUser.email) {
      this.currentAdminEmail = currentUser.email;
    } else {
      this.currentAdminEmail = null;
    }
  }

  loadAdmins(): void {
    this.isLoading = true;

    this.adminService.getAdmins().subscribe({
      next: (admins) => {
        console.log('ADMINS FROM API', admins);
        this.admins = admins;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading admins:', err);
        this.notificationService.showToast('Failed to load admins', 'error');
        this.isLoading = false;
      }
    });
  }

  // -----------------
  // CREATE / EDIT
  // -----------------

  openAddAdminModal(): void {
    this.formMode = 'create';
    this.resetAdminForm();
    this.isAdminFormModalOpen = true;
  }

  openEditAdminModal(admin: Admin): void {
    if (!admin) return;
    this.formMode = 'edit';
    this.editingAdminId = admin.id;

    const dto: UpdateAdminDto = {
      fullName: admin.fullName,
      email: admin.email
    };

    this.adminForm = { ...dto };

    this.deselectAdmin();
    this.isAdminFormModalOpen = true;
  }

  closeAdminFormModal(): void {
    this.isAdminFormModalOpen = false;
    this.editingAdminId = null;
    this.resetAdminForm();
  }

  resetAdminForm(): void {
    this.adminForm = {
      fullName: '',
      email: ''
    };
  }

  onSubmitAdminForm(): void {
    if (this.formMode === 'create') {
      const signupPayload = {
        fullName: this.adminForm.fullName,
        email: this.adminForm.email,
        roles: ['user', 'admin']
      };

      console.log('Signup payload', signupPayload);

      this.authService.signup(signupPayload).subscribe({
        next: () => {
          this.notificationService.showToast('Admin added successfully', 'success');
          this.loadAdmins(); // reload list since signup might not return the new admin
          this.closeAdminFormModal();
        },
        error: (err) => {
          console.error('Error adding admin:', err);
          this.notificationService.showToast('Failed to add admin', 'error');
        }
      });

      return;
    }

    if (!this.editingAdminId) {
      console.error('No admin ID to edit');
      return;
    }

    const payload: UpdateAdminDto = { ...this.adminForm };

    this.adminService.updateAdmin(this.editingAdminId, payload).subscribe({
      next: (updated) => {
        this.admins = this.admins.map(e =>
          e.id === this.editingAdminId ? { ...e, ...updated } : e
        );
        this.notificationService.showToast('Admin updated successfully', 'success');
        this.closeAdminFormModal();
      },
      error: (err) => {
        console.error('Error updating admin:', err);
        this.notificationService.showToast('Failed to update admin', 'error');
      }
    });
  }

  // -----------------
  // DETAILS / DELETE
  // -----------------

  selectAdmin(admin: Admin): void {
    this.selectedAdmin = admin;
  }

  deselectAdmin(): void {
    this.selectedAdmin = null;
  }

  onDeleteAdmin(adminId: string): void {
    if (!confirm('Are you sure you want to delete this admin?')) return;
    this.adminService.deleteAdmin(adminId).subscribe({
      next: () => {
        this.admins = this.admins.filter(e => e.id !== adminId);
        if (this.selectedAdmin?.id === adminId) this.deselectAdmin();
        this.notificationService.showToast('Admin deleted successfully', 'success');
      },
      error: (err) => {
        console.error('Error deleting admin:', err);
        this.notificationService.showToast('Failed to delete admin', 'error');
      }
    });
  }

  // -----------------
  // UI HELPERS
  // -----------------

  formatDate(iso?: string): string {
    if (!iso) return 'N/A';
    return new Date(iso).toLocaleString();
  }

  isCurrentAdmin(admin: Admin | null): boolean {
    if (!admin || !admin.email || !this.currentAdminEmail) return false;
    return admin.email.toLowerCase() === this.currentAdminEmail.toLowerCase();
  }

  getAdminIcon(admin: Admin): string {
    // Crown for the logged-in admin, shield for others
    return this.isCurrentAdmin(admin) ? '👑' : '🛡️';
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
