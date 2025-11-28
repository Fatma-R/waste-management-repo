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
import { CollectionPointService } from '../../core/services/collection-point';
import { NotificationService } from '../../core/services/notification';

import { Employee } from '../../shared/models/employee.model';
import { Bin } from '../../shared/models/bin.model';
import { Router, RouterModule } from '@angular/router';

interface ActivityLog {
  id: string;
  timestamp: Date;
  user: string;
  action: string;
  details: string;
  type: 'create' | 'update' | 'delete' | 'assign';
}

interface CollectionPointView {
  id: string;
  adresse: string;
  active: boolean;
  binsCount: number;
  avgFillPct: number;
  alertsCount: number;
}

type TourneeStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELED';

interface TourneeView {
  id: string;
  label: string;
  tourneeType: 'GLASS' | 'PLASTIC' | 'ORGANIC' | 'PAPER';
  status: TourneeStatus;
  plannedKm: number;
  plannedCO2: number;
  zoneLabel: string;
  stepsCount: number;
  plannedDate: Date;
}

type VehicleStatus = 'AVAILABLE' | 'IN_SERVICE' | 'MAINTENANCE';

interface VehicleView {
  id: string;
  plateNumber: string;
  capacityVolumeL: number;
  type: string;
  status: VehicleStatus;
  zoneLabel: string;
}

type IncidentType =
  | 'VEHICLE_BREAKDOWN'
  | 'BLOCKED_STREET'
  | 'BIN_DAMAGED'
  | 'OTHER';

type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

interface IncidentView {
  id: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  reportedAt: Date;
  description: string;
  contextLabel: string;
  reportedBy?: string;
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
    ModalComponent,
    RouterModule
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.scss']
})
export class AdminDashboardComponent implements OnInit {
  isLoading = true;

  employees: Employee[] = [];
  bins: Bin[] = [];

  collectionPoints: CollectionPointView[] = [];
  todayTournees: TourneeView[] = [];
  vehicles: VehicleView[] = [];
  recentIncidents: IncidentView[] = [];
  activityLogs: ActivityLog[] = [];

  // KPI values
  totalEmployees = 0;
  totalBins = 0;
  totalCollectionPoints = 0;
  totalVehicles = 0;
  activeTournees = 0;
  openIncidentsCount = 0;
  avgNetworkFillPct = 0;

  // Modals
  isDeleteEmployeeModalOpen = false;
  isDeleteBinModalOpen = false;
  selectedEmployeeId: string | null = null;
  selectedBinId: string | null = null;

  // internal loading flags
  private employeesLoaded = false;
  private binsLoaded = false;
  private collectionPointsLoaded = false;

  constructor(
    private employeeService: EmployeeService,
    private binService: BinService,
    private collectionPointService: CollectionPointService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.employeesLoaded = false;
    this.binsLoaded = false;
    this.collectionPointsLoaded = false;

    // Employees from backend
    this.employeeService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.totalEmployees = employees.length;
        this.employeesLoaded = true;
        this.checkLoadingComplete();
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.employeesLoaded = true;
        this.checkLoadingComplete();
      }
    });

    // Bins from service
    this.binService.getBins().subscribe({
      next: (bins) => {
        this.bins = bins;
        this.totalBins = bins.length;
        this.binsLoaded = true;
        this.checkLoadingComplete();
        this.computeAvgFillFromBins();
      },
      error: (err) => {
        console.error('Error loading bins:', err);
        this.binsLoaded = true;
        this.checkLoadingComplete();
      }
    });

    // Collection Points from API
    this.collectionPointService.getCollectionPoints().subscribe({
      next: (collectionPointsData) => {
        this.collectionPoints = collectionPointsData.map(cp => ({
          id: cp.id,
          adresse: cp.adresse,
          active: cp.active,
          binsCount: cp.bins?.length || 0,
          avgFillPct: this.calculateAvgFillForCP(cp.bins || []),
          alertsCount: 0 // TODO: Calculate from bin readings if available
        }));
        this.totalCollectionPoints = this.collectionPoints.length;
        this.collectionPointsLoaded = true;
        this.checkLoadingComplete();
        this.updateNetworkAvgFill();
      },
      error: (err) => {
        console.error('Error loading collection points:', err);
        this.collectionPointsLoaded = true;
        this.checkLoadingComplete();
      }
    });

    // Local mock data for other features
    this.loadMockTournees();
    this.loadMockVehicles();
    this.loadMockIncidents();
    this.loadActivityLogs();
  }

  private checkLoadingComplete(): void {
    if (this.employeesLoaded && this.binsLoaded && this.collectionPointsLoaded) {
      this.isLoading = false;
    }
  }

  // ========= MOCK DATA (pure frontend for now) =========

  private loadMockCollectionPoints(): void {
    // No longer needed - data loaded from API in loadDashboardData()
  }

  private calculateAvgFillForCP(bins: Bin[]): number {
    if (bins.length === 0) return 0;
    // TODO: Calculate average fill percentage from bin readings
    // For now, return 0 as a placeholder
    return 0;
  }

  private updateNetworkAvgFill(): void {
    if (this.collectionPoints.length === 0) return;
    const totalFill = this.collectionPoints.reduce((sum, cp) => sum + cp.avgFillPct, 0);
    this.avgNetworkFillPct = Math.round(totalFill / this.collectionPoints.length);
  }

  private loadMockTournees(): void {
    const now = new Date();
    this.todayTournees = [
      {
        id: 't1',
        label: 'Morning Tour · Zone A (PLASTIC)',
        tourneeType: 'PLASTIC',
        status: 'IN_PROGRESS',
        plannedKm: 18.5,
        plannedCO2: 24.2,
        zoneLabel: 'Zone A',
        stepsCount: 12,
        plannedDate: now
      },
      {
        id: 't2',
        label: 'Midday Tour · Zone B (ORGANIC)',
        tourneeType: 'ORGANIC',
        status: 'PLANNED',
        plannedKm: 14.0,
        plannedCO2: 19.5,
        zoneLabel: 'Zone B',
        stepsCount: 9,
        plannedDate: new Date(now.getTime() + 2 * 60 * 60 * 1000)
      },
      {
        id: 't3',
        label: 'Evening Tour · Zone C (GLASS)',
        tourneeType: 'GLASS',
        status: 'PLANNED',
        plannedKm: 11.3,
        plannedCO2: 15.0,
        zoneLabel: 'Zone C',
        stepsCount: 7,
        plannedDate: new Date(now.getTime() + 6 * 60 * 60 * 1000)
      },
      {
        id: 't4',
        label: 'Night Tour · Mixed (PAPER)',
        tourneeType: 'PAPER',
        status: 'COMPLETED',
        plannedKm: 9.2,
        plannedCO2: 11.8,
        zoneLabel: 'Mixed zones',
        stepsCount: 5,
        plannedDate: new Date(now.getTime() - 8 * 60 * 60 * 1000)
      }
    ];

    this.activeTournees = this.todayTournees.filter((t) =>
      ['PLANNED', 'IN_PROGRESS'].includes(t.status)
    ).length;
  }

  private loadMockVehicles(): void {
    this.vehicles = [
      {
        id: 'v1',
        plateNumber: 'TUN-123-AB',
        capacityVolumeL: 8000,
        type: 'Compactor Truck',
        status: 'IN_SERVICE',
        zoneLabel: 'Assigned to Zone A'
      },
      {
        id: 'v2',
        plateNumber: 'TUN-456-CD',
        capacityVolumeL: 10000,
        type: 'Compactor Truck',
        status: 'MAINTENANCE',
        zoneLabel: 'Depot · Maintenance'
      },
      {
        id: 'v3',
        plateNumber: 'TUN-789-EF',
        capacityVolumeL: 4000,
        type: 'Support Vehicle',
        status: 'AVAILABLE',
        zoneLabel: 'Unassigned'
      }
    ];

    this.totalVehicles = this.vehicles.length;
  }

  private loadMockIncidents(): void {
    const now = new Date();
    this.recentIncidents = [
      {
        id: 'i1',
        type: 'BIN_DAMAGED',
        severity: 'HIGH',
        status: 'OPEN',
        reportedAt: new Date(now.getTime() - 30 * 60 * 1000),
        description: 'Damaged lid on Bin #105 (PLASTIC).',
        contextLabel: 'Bin #105 · Avenue Habib Bourguiba',
        reportedBy: 'sensor'
      },
      {
        id: 'i2',
        type: 'VEHICLE_BREAKDOWN',
        severity: 'CRITICAL',
        status: 'IN_PROGRESS',
        reportedAt: new Date(now.getTime() - 90 * 60 * 1000),
        description: 'Truck TUN-456-CD breakdown during organic tour.',
        contextLabel: 'Tournée ORGANIC · Zone B',
        reportedBy: 'driver'
      },
      {
        id: 'i3',
        type: 'BLOCKED_STREET',
        severity: 'MEDIUM',
        status: 'OPEN',
        reportedAt: new Date(now.getTime() - 2 * 60 * 60 * 1000),
        description: 'Street blocked, route step must be skipped or re-routed.',
        contextLabel: 'Rue de la Liberté · Zone B',
        reportedBy: 'planning system'
      },
      {
        id: 'i4',
        type: 'OTHER',
        severity: 'LOW',
        status: 'RESOLVED',
        reportedAt: new Date(now.getTime() - 4 * 60 * 60 * 1000),
        description: 'Low battery on sensor for Bin #54.',
        contextLabel: 'Bin #54 · Zone C',
        reportedBy: 'technician'
      }
    ];

    this.openIncidentsCount = this.recentIncidents.filter((i) =>
      ['OPEN', 'IN_PROGRESS'].includes(i.status)
    ).length;
  }

  private loadActivityLogs(): void {
    this.activityLogs = [
      {
        id: 'log1',
        timestamp: new Date(),
        user: 'Admin',
        action: 'Recalculated tournées for tomorrow',
        details: 'Optimized routes based on latest fill predictions.',
        type: 'update'
      },
      {
        id: 'log2',
        timestamp: new Date(Date.now() - 45 * 60 * 1000),
        user: 'Planner',
        action: 'Assigned driver to Tour ORGANIC · Zone B',
        details: 'Employee John Doe assigned to Tour t2.',
        type: 'assign'
      },
      {
        id: 'log3',
        timestamp: new Date(Date.now() - 90 * 60 * 1000),
        user: 'Admin',
        action: 'Marked incident i4 as resolved',
        details: 'Sensor battery replaced at Bin #54.',
        type: 'update'
      },
      {
        id: 'log4',
        timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000),
        user: 'Admin',
        action: 'Created new collection point',
        details: 'Cité El Ghazela · Zone D with 6 bins.',
        type: 'create'
      }
    ];
  }

  private computeAvgFillFromBins(): void {
    // If your Bin model carries a `fillLevel` or similar, you can compute here.
    // For now we only recompute if the model has that field.
    const withAnyFill = (this.bins as any[])
      .map((b) => b.fillLevel)
      .filter((v) => typeof v === 'number');

    if (withAnyFill.length > 0) {
      const avg =
        withAnyFill.reduce((sum, v) => sum + v, 0) /
        (withAnyFill.length || 1);
      this.avgNetworkFillPct = Math.round(avg);
    }
  }

  // ========= EMPLOYEE ACTIONS =========

  openDeleteEmployeeModal(employeeId: string): void {
    this.selectedEmployeeId = employeeId;
    this.isDeleteEmployeeModalOpen = true;
  }

  closeDeleteEmployeeModal(): void {
    this.isDeleteEmployeeModalOpen = false;
    this.selectedEmployeeId = null;
  }

  confirmDeleteEmployee(): void {
    if (!this.selectedEmployeeId) return;

    this.employeeService.deleteEmployee(this.selectedEmployeeId).subscribe({
      next: () => {
        this.employees = this.employees.filter(
          (e) => e.id !== this.selectedEmployeeId
        );
        this.totalEmployees = this.employees.length;
        this.notificationService.showToast(
          'Employee deleted successfully',
          'success'
        );
        this.addActivityLog('delete', 'Employee removed from system');
        this.closeDeleteEmployeeModal();
      },
      error: (err) => {
        console.error('Error deleting employee:', err);
        this.notificationService.showToast(
          'Failed to delete employee',
          'error'
        );
      }
    });
  }

  openAssignEmployeeModal(employee: Employee): void {
    console.log('Assign employee to tournee:', employee);
    this.notificationService.showToast(
      `Assigning ${employee.fullName} to a tournée (UI not implemented yet)`,
      'info'
    );
  }

  // ========= BIN / COLLECTION POINT ACTIONS =========

  openDeleteBinModal(binId: string): void {
    this.selectedBinId = binId;
    this.isDeleteBinModalOpen = true;
  }

  closeDeleteBinModal(): void {
    this.isDeleteBinModalOpen = false;
    this.selectedBinId = null;
  }

  confirmDeleteBin(): void {
    if (!this.selectedBinId) return;

    this.binService.deleteBin(this.selectedBinId).subscribe({
      next: () => {
        this.bins = this.bins.filter((b) => b.id !== this.selectedBinId);
        this.totalBins = this.bins.length;
        this.notificationService.showToast(
          'Bin deleted successfully',
          'success'
        );
        this.addActivityLog('delete', 'Bin removed from system');
        this.closeDeleteBinModal();
      },
      error: (err) => {
        console.error('Error deleting bin:', err);
        this.notificationService.showToast(
          'Failed to delete bin',
          'error'
        );
      }
    });
  }

  openCollectionPointDetails(cp: CollectionPointView): void {
    console.log('Open collection point details:', cp);
    this.notificationService.showToast(
      `Collection point details for ${cp.adresse} (UI not implemented yet)`,
      'info'
    );
  }

  navigateToBins(collectionPointId: string): void {
    this.router.navigate(['/admin/collection-points', collectionPointId, 'bins']);
  }

  // ========= VEHICLE ACTIONS =========

  openVehicleMaintenanceModal(vehicle: VehicleView): void {
    console.log('Register maintenance for vehicle:', vehicle);
    this.notificationService.showToast(
      `Maintenance for vehicle ${vehicle.plateNumber} (UI not implemented yet)`,
      'info'
    );
  }

  // ========= ACTIVITY LOG HELPERS =========

  private addActivityLog(type: ActivityLog['type'], details: string): void {
    const log: ActivityLog = {
      id: Date.now().toString(),
      timestamp: new Date(),
      user: 'Admin',
      action: this.getActionLabel(type),
      details,
      type
    };
    this.activityLogs.unshift(log);
  }

  private getActionLabel(type: ActivityLog['type']): string {
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

  // ========= BADGE / STATUS HELPERS =========

  getTourneeStatusClass(status: TourneeStatus): string {
    // Reuses your existing `status-badge status-...` pattern
    switch (status) {
      case 'IN_PROGRESS':
        return 'status-in-progress';
      case 'PLANNED':
        return 'status-planned';
      case 'COMPLETED':
        return 'status-completed';
      case 'CANCELED':
        return 'status-canceled';
      default:
        return '';
    }
  }

  getVehicleStatusClass(status: VehicleStatus): string {
    switch (status) {
      case 'AVAILABLE':
        return 'status-available';
      case 'IN_SERVICE':
        return 'status-active';
      case 'MAINTENANCE':
        return 'status-warning';
      default:
        return '';
    }
  }

  getIncidentSeverityClass(severity: IncidentSeverity): string {
    switch (severity) {
      case 'LOW':
        return 'status-low';
      case 'MEDIUM':
        return 'status-medium';
      case 'HIGH':
        return 'status-high';
      case 'CRITICAL':
        return 'status-critical';
      default:
        return '';
    }
  }

  getIncidentStatusClass(status: IncidentStatus): string {
    switch (status) {
      case 'OPEN':
        return 'status-open';
      case 'IN_PROGRESS':
        return 'status-in-progress';
      case 'RESOLVED':
        return 'status-resolved';
      case 'CLOSED':
        return 'status-closed';
      default:
        return '';
    }
  }

  getIncidentTypeIcon(type: IncidentType): string {
    switch (type) {
      case 'BIN_DAMAGED':
        return '🗑️';
      case 'VEHICLE_BREAKDOWN':
        return '🚚';
      case 'BLOCKED_STREET':
        return '🚧';
      case 'OTHER':
      default:
        return '❓';
    }
  }

  // ========= CONTROL BUTTON HANDLERS =========

  onRecalculateTournees(): void {
    console.log('Recalculate tournées');
    this.notificationService.showToast(
      'Recalculate tournées (backend integration pending).',
      'info'
    );
  }

  onRebalanceBins(): void {
    console.log('Rebalance collection points / bins');
    this.notificationService.showToast(
      'Rebalance collection points (backend integration pending).',
      'info'
    );
  }

  onExportKpiReport(): void {
    console.log('Export KPI report');
    this.notificationService.showToast(
      'Export KPI report (backend integration pending).',
      'info'
    );
  }

  onExportIncidentReport(): void {
    console.log('Export incident report');
    this.notificationService.showToast(
      'Export incident log (backend integration pending).',
      'info'
    );
  }

  onSyncWithSensors(): void {
    console.log('Sync with sensors');
    this.notificationService.showToast(
      'Sync with sensors / IoT (backend integration pending).',
      'info'
    );
  }

  onOpenSystemSettings(): void {
    this.router.navigate(['/admin/admins']);
  }
}
