import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CardComponent } from '../../shared/components/card/card';
import { KpiComponent } from '../../shared/components/kpi/kpi';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

import { EmployeeService } from '../../core/services/employee';
import { BinService } from '../../core/services/bin';
import { CollectionPointService } from '../../core/services/collection-point';
import { NotificationService } from '../../core/services/notification';

import { Employee } from '../../shared/models/employee.model';
import { Bin } from '../../shared/models/bin.model';
import { Router, RouterModule } from '@angular/router';
import { VehicleService } from '../../core/services/vehicle';
import { IncidentService } from '../../core/services/incident';
import { Incident } from '../../shared/models/incident.model';
import { AlertService } from '../../core/services/alert';
import { Alert, AlertType } from '../../shared/models/alert.model';
import { AutoPlanningService } from '../../core/services/auto-planning';
import { TourneeService } from '../../core/services/tournee';
import { AutoMode } from '../../shared/models/AutoMoode.model';

// type alias réutilisable
type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

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
type VehicleFuelType = 'DIESEL' | 'GASOLINE' | 'ELECTRIC' | 'HYBRID';

interface VehicleView {
  id: string;
  plateNumber: string;
  capacityVolumeL: number;
  status: VehicleStatus;
  fuelType: VehicleFuelType;
}

type IncidentType =
  | 'BLOCKED_STREET'
  | 'TRAFFIC_ACCIDENT'
  | 'POLICE_ACTIVITY'
  | 'ROAD_MAINTENANCE'
  | 'PUBLIC_EVENT'
  | 'NATURAL_OBSTRUCTION'
  | 'FIRE_BLOCKAGE'
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
  //reportedBy?: string;
}

export interface AlertView {
  id: string;
  message: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  createdAt: Date;
  resolved: boolean;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    KpiComponent,
    LoadingSpinnerComponent,
    RouterModule
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.scss']
})
export class AdminDashboardComponent implements OnInit {
  AutoMode = AutoMode;

  isLoading = true;

  employees: Employee[] = [];
  bins: Bin[] = [];

  collectionPoints: CollectionPointView[] = [];
  todayTournees: TourneeView[] = [];
  vehicles: VehicleView[] = [];
  recentIncidents: IncidentView[] = [];
  activityLogs: ActivityLog[] = [];

  incidents: Incident[] = [];
  alerts: AlertView[] = [];

  autoMode: AutoMode | null = null;
  isModeLoading = false;
  runLoading = {
    scheduled: false,
    emergency: false
  };

  get autoModeLabel(): string {
    switch (this.autoMode) {
      case AutoMode.OFF:
        return 'Off';
      case AutoMode.EMERGENCIES_ONLY:
        return 'Emergencies only';
      case AutoMode.FULL:
        return 'Full automation';
      default:
        return '...';
    }
  }

  // KPI values
  totalEmployees = 0;
  totalBins = 0;
  totalCollectionPoints = 0;
  totalVehicles = 0;
  activeTournees = 0;
  openIncidentsCount = 0;
  avgNetworkFillPct = 0;
  totalAlerts = 0;
  co2Last7Days = 0;

  // Modals
  isDeleteEmployeeModalOpen = false;
  isDeleteBinModalOpen = false;
  selectedEmployeeId: string | null = null;
  selectedBinId: string | null = null;

  // internal loading flags
  private employeesLoaded = false;
  private binsLoaded = false;
  private collectionPointsLoaded = false;

  tournees: any;
  assignSuccessMessage: string | null = null;
  assignErrorMessage: string | null = null;

  selectedVehicleId: string | null = null;
  isDeleteVehicleModalOpen: boolean = false;

  constructor(
    private employeeService: EmployeeService,
    private binService: BinService,
    private collectionPointService: CollectionPointService,
    private notificationService: NotificationService,
    private router: Router,
    private vehicleService: VehicleService,
    private incidentService: IncidentService,
    private alertService: AlertService,
    private autoPlanningService: AutoPlanningService,
    private tourneeService: TourneeService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadAutoMode();
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

    // Vehicle from backend
    this.vehicleService.getVehicles().subscribe({
      next: (vehicles) => {
        this.vehicles = vehicles.map((v) => ({
          id: v.id,
          plateNumber: v.plateNumber,
          capacityVolumeL: v.capacityVolumeL,
          fuelType: v.fuelType,
          status: v.status
        }));
        this.totalVehicles = this.vehicles.length;
      },
      error: (err) => {
        console.error(err);
        this.notificationService.showToast('Erreur lors du chargement des véhicules', 'error');
      }
    });

    // Incidents from backend
    this.incidentService.getIncidents().subscribe({
      next: (incidents) => {
        this.recentIncidents = incidents
          .map((i) => ({
            id: i.id,
            type: i.type,
            severity: i.severity,
            status: i.status,
            reportedAt: i.reportedAt ? new Date(i.reportedAt) : new Date(),
            description: i.description || 'Aucune description',
            contextLabel: i.location
              ? `Lat: ${i.location.coordinates[1]}, Lng: ${i.location.coordinates[0]}`
              : 'Localisation inconnue'
            //reportedBy: i.reportedBy || 'Inconnu',
          }))
          .sort((a, b) => b.reportedAt.getTime() - a.reportedAt.getTime())
          .slice(0, 6); // garder seulement les 6 plus récents

        // KPI : incidents ouverts ou en cours
        this.openIncidentsCount = this.recentIncidents.filter(
          (i) => i.status === 'OPEN' || i.status === 'IN_PROGRESS'
        ).length;
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement des incidents', err);
        this.notificationService.showToast('Erreur lors du chargement des incidents', 'error');
      }
    });

    // 👉 Charge les ALERTS POUR DE VRAI
    this.alertService.getAlerts().subscribe({
      next: (alerts: Alert[]) => {
        this.alerts = alerts
          .map((a) => this.mapAlertToView(a))
          .sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
          .slice(0, 6);

        this.totalAlerts = alerts.length;
      },
      error: () => {
        this.notificationService.showToast('Erreur chargement Alertes', 'error');
      }
    });

    // Active Tournees
    this.tourneeService.getInProgressTournees().subscribe({
      next: (tournees) => {
        this.activeTournees = tournees?.length ?? 0;
      },
      error: () => {
        this.activeTournees = 0; // or keep old value
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
        this.collectionPoints = collectionPointsData.map((cp) => ({
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

    this.loadCo2Last7Days();

    // Local mock data for other features
    //this.loadMockTournees();
    //this.loadMockVehicles();
    //this.loadMockIncidents();
    this.loadActivityLogs();
  }

  private loadCo2Last7Days(): void {
    this.tourneeService.getCo2Last7Days().subscribe({
      next: (co2Kg) => {
        this.co2Last7Days = co2Kg;
      },
      error: (err) => {
        console.error('Error loading CO2 for last 7 days:', err);
      }
    });
  }

  private checkLoadingComplete(): void {
    if (this.employeesLoaded && this.binsLoaded && this.collectionPointsLoaded) {
      this.isLoading = false;
    }
  }

  // ========= MOCK DATA (pure frontend for now) =========

  //private loadMockCollectionPoints(): void {
  // No longer needed - data loaded from API in loadDashboardData()}

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
    const withAnyFill = (this.bins as any[]).map((b) => b.fillLevel).filter((v) => typeof v === 'number');

    if (withAnyFill.length > 0) {
      const avg = withAnyFill.reduce((sum, v) => sum + v, 0) / (withAnyFill.length || 1);
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
        this.employees = this.employees.filter((e) => e.id !== this.selectedEmployeeId);
        this.totalEmployees = this.employees.length;
        this.notificationService.showToast('Employee deleted successfully', 'success');
        this.addActivityLog('delete', 'Employee removed from system');
        this.closeDeleteEmployeeModal();
      },
      error: (err) => {
        console.error('Error deleting employee:', err);
        this.notificationService.showToast('Failed to delete employee', 'error');
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
        this.notificationService.showToast('Bin deleted successfully', 'success');
        this.addActivityLog('delete', 'Bin removed from system');
        this.closeDeleteBinModal();
      },
      error: (err) => {
        console.error('Error deleting bin:', err);
        this.notificationService.showToast('Failed to delete bin', 'error');
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
    this.notificationService.showToast(`Maintenance for vehicle ${vehicle.plateNumber} (UI not implemented yet)`, 'info');
  }

  openDeleteVahicleModal(vehicleId: string): void {
    this.selectedVehicleId = vehicleId;
    this.isDeleteVehicleModalOpen = true;
  }

  openDeleteVehicleModal(vehicleId: string): void {
    this.selectedVehicleId = vehicleId;
    this.isDeleteVehicleModalOpen = true;
  }

  confirmDeleteVehicle(): void {
    if (!this.selectedVehicleId) return;

    // Appel à ton service pour supprimer le véhicule
    this.vehicleService.deleteVehicle(this.selectedVehicleId).subscribe({
      next: () => {
        // Supprime le véhicule de la liste locale
        this.vehicles = this.vehicles.filter((v) => v.id !== this.selectedVehicleId);
        this.isDeleteVehicleModalOpen = false;
        this.selectedVehicleId = null;
      },
      error: (err) => {
        console.error('Error deleting vehicle:', err);
      }
    });
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
      case 'IN_SERVICE':
        return 'status-in-service'; // vert
      case 'MAINTENANCE':
        return 'status-maintenance'; // rouge
      case 'AVAILABLE':
        return 'status-available'; // bleu
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
      case 'TRAFFIC_ACCIDENT':
        return '🚧'; // accident / route bloquée
      case 'BLOCKED_STREET':
        return '⛔'; // rue bloquée
      case 'POLICE_ACTIVITY':
        return '🚓'; // police
      case 'ROAD_MAINTENANCE':
        return '🛠️';
      case 'PUBLIC_EVENT':
        return '🎉';
      case 'NATURAL_OBSTRUCTION':
        return '🌳';
      case 'FIRE_BLOCKAGE':
        return '🔥';
      case 'OTHER':
      default:
        return '❓';
    }
  }

  private mapAlertToView(alert: Alert): AlertView {
    return {
      id: alert.id,
      message: this.getAlertMessage(alert),
      severity: this.getSeverity(alert.type),
      createdAt: new Date(alert.ts),
      resolved: alert.cleared
    };
  }

  private getAlertMessage(alert: Alert): string {
    switch (alert.type) {
      case 'LEVEL_HIGH':
        return `Niveau élevé détecté dans la poubelle ${alert.binId}`;
      case 'LEVEL_LOW':
        return `Niveau bas détecté dans la poubelle ${alert.binId}`;
      case 'LEVEL_CRITICAL':
        return `⚠️ Niveau critique dans la poubelle ${alert.binId}`;
      case 'BATTERY_LOW':
        return `Batterie faible pour le capteur de la poubelle ${alert.binId}`;
      case 'SENSOR_ANOMALY':
        return `Anomalie détectée sur un capteur (poubelle ${alert.binId})`;
      case 'THRESHOLD':
        return `Seuil dépassé dans la poubelle ${alert.binId}`;
      default:
        return `Alerte détectée (poubelle ${alert.binId})`;
    }
  }

  private getSeverity(type: AlertType): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    switch (type) {
      case 'LEVEL_LOW':
        return 'LOW';
      case 'BATTERY_LOW':
        return 'MEDIUM';
      case 'LEVEL_HIGH':
        return 'HIGH';
      case 'LEVEL_CRITICAL':
      case 'SENSOR_ANOMALY':
        return 'CRITICAL';
      default:
        return 'MEDIUM';
    }
  }

  private showAssignSuccess(message: string): void {
    this.assignSuccessMessage = message;
    this.assignErrorMessage = null;

    setTimeout(() => {
      this.assignSuccessMessage = null;
    }, 3000);
  }

  private showAssignError(message: string): void {
    this.assignErrorMessage = message;
    this.assignSuccessMessage = null;

    setTimeout(() => {
      this.assignErrorMessage = null;
    }, 3000);
  }

  // ========= AUTO PLANNING =========

  loadAutoMode(): void {
    this.isModeLoading = true;
    this.autoPlanningService.getMode().subscribe({
      next: (mode) => {
        this.autoMode = mode;
        this.isModeLoading = false;
      },
      error: () => {
        this.isModeLoading = false;
        this.notificationService.showToast('Erreur lors du chargement du mode auto', 'error');
      }
    });
  }

  setAutoMode(mode: AutoMode): void {
    if (this.isModeLoading || this.autoMode === mode) return;
    this.isModeLoading = true;
    this.autoPlanningService.setMode(mode).subscribe({
      next: () => {
        this.autoMode = mode;
        this.isModeLoading = false;
        this.notificationService.showToast(
          `Mode basculé sur ${mode.replace('_', ' ').toLowerCase()}`,
          'success'
        );
      },
      error: () => {
        this.isModeLoading = false;
        this.notificationService.showToast('Impossible de changer le mode auto', 'error');
      }
    });
  }

  runScheduled(): void {
    if (this.runLoading.scheduled) return;
    this.runLoading.scheduled = true;
    this.autoPlanningService.runScheduledCycle().subscribe({
      next: () => {
        this.runLoading.scheduled = false;
        this.notificationService.showToast('Cycle quotidien déclenché', 'success');
        this.showAssignSuccess('Full loop triggered successfully.');
      },
      error: () => {
        this.runLoading.scheduled = false;
        this.notificationService.showToast('Échec du déclenchement du cycle', 'error');
        this.assignErrorMessage = 'Failed to trigger full loop.';
      }
    });
  }

  runEmergency(): void {
    if (this.runLoading.emergency) return;
    this.runLoading.emergency = true;
    this.autoPlanningService.runEmergencyLoop().subscribe({
      next: () => {
        this.runLoading.emergency = false;
        this.notificationService.showToast('Boucle urgence déclenchée', 'success');
        this.showAssignSuccess('Emergency loop triggered successfully.');
      },
      error: () => {
        this.runLoading.emergency = false;
        this.notificationService.showToast('Échec du déclenchement urgence', 'error');
        this.assignErrorMessage = 'Failed to trigger emergency loop.';
      }
    });
  }

  // ========= CONTROL BUTTON HANDLERS =========

  onRecalculateTournees(): void {
    console.log('Recalculate tournées');
    this.notificationService.showToast('Recalculate tournées (backend integration pending).', 'info');
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
    this.notificationService.showToast('Export KPI report (backend integration pending).', 'info');
  }

  onExportIncidentReport(): void {
    this.incidentService.getIncidents().subscribe({
      next: (incidents) => {
        const headers = ['id', 'type', 'severity', 'status', 'description', 'longitude', 'latitude', 'reportedAt'];
        const toCell = (v: unknown) => `"${String(v ?? '').replace(/"/g, '""')}"`;

        const rows = incidents.map((i: any) => [
          i.id,
          i.type,
          i.severity,
          i.status,
          i.description,
          i.location?.coordinates?.[0],
          i.location?.coordinates?.[1],
          i.reportedAt ?? i['reportedAt']
        ]);

        const csv = [headers.join(','), ...rows.map((r) => r.map(toCell).join(','))].join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });

        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'incidents.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      },
      error: () => {
        this.assignErrorMessage = 'Failed to export incidents.';
      }
    });
  }

  onSyncWithSensors(): void {
    console.log('Sync with sensors');
    this.notificationService.showToast('Sync with sensors / IoT (backend integration pending).', 'info');
  }

  onOpenSystemSettings(): void {
    this.router.navigate(['/admin/admins']);
  }

  goToVehicles(): void {
    this.router.navigate(['/admin/vehicles']);
  }

  goToIncidents(): void {
    this.router.navigate(['/admin/incidents']);
  }

  goToTourneeMap(): void {
    this.router.navigate(['/admin/tournee-map']);
  }

  goToPlanning(): void {
    this.router.navigate(['/admin/tournees']);
  }

  onExportCollectionPointsReport(): void {
    this.collectionPointService.getCollectionPoints().subscribe({
      next: (points) => {
        const headers = [
          'collectionPointId',
          'address',
          'cpLongitude',
          'cpLatitude',
          'active',
          'binId',
          'binType',
          'binFillPct'
        ];

        const toCell = (v: unknown) => `"${String(v ?? '').replace(/"/g, '""')}"`;

        const rows: unknown[][] = [];

        (points as any[]).forEach((cp) => {
          const cpLon = cp.location?.coordinates?.[0];
          const cpLat = cp.location?.coordinates?.[1];

          const bins = Array.isArray(cp.bins) ? cp.bins : [];

          // if no bins, still export the CP row with empty bin columns
          if (bins.length === 0) {
            rows.push([cp.id, cp.adresse ?? cp.address ?? '', cpLon, cpLat, cp.active, '', '', '']);
            return;
          }

          // 1 row per bin
          bins.forEach((b: any) => {
            rows.push([
              cp.id,
              cp.adresse ?? cp.address ?? '',
              cpLon,
              cpLat,
              cp.active,
              b.id,
              b.type ?? b.trashType ?? '',
              b.fillPct ?? b.fillLevelPct ?? b.fillLevel ?? ''
            ]);
          });
        });

        const csv = [headers.join(','), ...rows.map((r) => r.map(toCell).join(','))].join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });

        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'collection-points-with-bins.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      },
      error: () => {
        this.assignErrorMessage = 'Failed to export collection points.';
      }
    });
  }
}
