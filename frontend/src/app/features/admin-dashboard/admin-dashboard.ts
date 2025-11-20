import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { KpiComponent } from '../../shared/components/kpi/kpi';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { ModalComponent } from '../../shared/components/modal/modal';
import { EmployeeService } from '../../core/services/employee';
import { BinService } from '../../core/services/bin';
import { NotificationService } from '../../core/services/notification';
import { Employee } from '../../shared/models/employee.model';
import { Bin } from '../../shared/models/bin.model';

interface ActivityLog {
  id: string;
  timestamp: Date;
  user: string;
  action: string;
  details: string;
  type: 'create' | 'update' | 'delete' | 'assign';
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    KpiComponent,
    LoadingSpinnerComponent,
    ModalComponent
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.scss']
})
export class AdminDashboardComponent implements OnInit {
  isLoading = true;
  employees: Employee[] = [];
  bins: Bin[] = [];
  activityLogs: ActivityLog[] = [];
  
  // Stats
  totalEmployees = 0;
  totalBins = 0;
  activeRoutes = 0;
  systemUptime = '99.8%';

  // Modals
  isDeleteEmployeeModalOpen = false;
  isDeleteBinModalOpen = false;
  selectedEmployeeId: string | null = null;
  selectedBinId: string | null = null;

  constructor(
    private employeeService: EmployeeService,
    private binService: BinService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadAdminData();
  }

  loadAdminData(): void {
    this.isLoading = true;
    
    // Load employees
    this.employeeService.getMockEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.totalEmployees = employees.length;
        this.checkLoadingComplete();
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.checkLoadingComplete();
      }
    });

    // Load bins
    this.binService.getMockBins().subscribe({
      next: (bins) => {
        this.bins = bins;
        this.totalBins = bins.length;
        this.checkLoadingComplete();
      },
      error: (err) => {
        console.error('Error loading bins:', err);
        this.checkLoadingComplete();
      }
    });

    // Load mock activity logs
    this.loadActivityLogs();
    this.activeRoutes = 3; // Mock value
  }

  checkLoadingComplete(): void {
    if (this.employees.length > 0 && this.bins.length > 0) {
      this.isLoading = false;
    }
  }

  loadActivityLogs(): void {
    // Mock activity logs
    this.activityLogs = [
      {
        id: '1',
        timestamp: new Date(),
        user: 'Admin',
        action: 'Created new bin',
        details: 'Bin #105 added in Zone A',
        type: 'create'
      },
      {
        id: '2',
        timestamp: new Date(Date.now() - 3600000),
        user: 'Admin',
        action: 'Updated employee',
        details: 'Changed John Smith role to Supervisor',
        type: 'update'
      },
      {
        id: '3',
        timestamp: new Date(Date.now() - 7200000),
        user: 'Admin',
        action: 'Assigned zones',
        details: 'Sarah Johnson assigned to Zone C',
        type: 'assign'
      },
      {
        id: '4',
        timestamp: new Date(Date.now() - 10800000),
        user: 'Admin',
        action: 'Deleted bin',
        details: 'Bin #98 removed from system',
        type: 'delete'
      }
    ];
  }

  openDeleteEmployeeModal(employeeId: string): void {
    this.selectedEmployeeId = employeeId;
    this.isDeleteEmployeeModalOpen = true;
  }

  closeDeleteEmployeeModal(): void {
    this.isDeleteEmployeeModalOpen = false;
    this.selectedEmployeeId = null;
  }

  confirmDeleteEmployee(): void {
    if (this.selectedEmployeeId) {
      this.employeeService.deleteEmployee(this.selectedEmployeeId).subscribe({
        next: () => {
          this.employees = this.employees.filter(e => e.id !== this.selectedEmployeeId);
          this.totalEmployees = this.employees.length;
          this.notificationService.showToast('Employee deleted successfully', 'success');
          this.closeDeleteEmployeeModal();
          this.addActivityLog('delete', 'Deleted employee from system');
        },
        error: (err) => {
          console.error('Error deleting employee:', err);
          this.notificationService.showToast('Failed to delete employee', 'error');
        }
      });
    }
  }

  openDeleteBinModal(binId: string): void {
    this.selectedBinId = binId;
    this.isDeleteBinModalOpen = true;
  }

  closeDeleteBinModal(): void {
    this.isDeleteBinModalOpen = false;
    this.selectedBinId = null;
  }

  confirmDeleteBin(): void {
    if (this.selectedBinId) {
      this.binService.deleteBin(this.selectedBinId).subscribe({
        next: () => {
          this.bins = this.bins.filter(b => b.id !== this.selectedBinId);
          this.totalBins = this.bins.length;
          this.notificationService.showToast('Bin deleted successfully', 'success');
          this.closeDeleteBinModal();
          this.addActivityLog('delete', 'Deleted bin from system');
        },
        error: (err) => {
          console.error('Error deleting bin:', err);
          this.notificationService.showToast('Failed to delete bin', 'error');
        }
      });
    }
  }

  addActivityLog(type: ActivityLog['type'], details: string): void {
    const newLog: ActivityLog = {
      id: Date.now().toString(),
      timestamp: new Date(),
      user: 'Admin',
      action: this.getActionLabel(type),
      details: details,
      type: type
    };
    this.activityLogs.unshift(newLog);
  }

  getActionLabel(type: ActivityLog['type']): string {
    const labels: Record<ActivityLog['type'], string> = {
      create: 'Created',
      update: 'Updated',
      delete: 'Deleted',
      assign: 'Assigned'
    };
    return labels[type];
  }

  formatTimestamp(timestamp: Date): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return date.toLocaleDateString();
  }

  getActivityIcon(type: ActivityLog['type']): string {
    const icons: Record<ActivityLog['type'], string> = {
      create: '✅',
      update: '✏️',
      delete: '🗑️',
      assign: '📍'
    };
    return icons[type];
  }

  getStatusBadgeClass(status: string): string {
    return `status-badge status-${status}`;
  }
}
