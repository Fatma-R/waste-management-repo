// src/app/features/alerts/alerts.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { AlertService } from '../../core/services/alert';
import { NotificationService } from '../../core/services/notification';
import { Alert, CreateAlertDto, UpdateAlertDto, AlertType } from '../../shared/models/alert.model';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './alert.html',
  styleUrls: ['./alert.scss']
})
export class AlertsComponent implements OnInit {
  alerts: Alert[] = [];
  selectedAlert: Alert | null = null;
  isLoading = true;

  isAlertFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingAlertId: string | null = null;

  // form model
  alertForm: CreateAlertDto = {
    binId: '',
    ts: new Date().toISOString(),
    type: 'THRESHOLD',
    value: 0,
    cleared: false
  };

  // filters
  filterType: 'all' | AlertType = 'all';
  filterCleared: 'all' | 'cleared' | 'not_cleared' = 'all';

  constructor(
    private alertService: AlertService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.alertService.getAlerts().subscribe({
      next: (alerts: Alert[]) => {
        this.alerts = alerts;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error loading alerts:', err);
        this.notificationService.showToast('Failed to load alerts', 'error');
        this.isLoading = false;
      }
    });
  }

  get filteredAlerts(): Alert[] {
    return this.alerts.filter(a => {
      const byType = this.filterType === 'all' || a.type === this.filterType;
      const byCleared =
        this.filterCleared === 'all' ||
        (this.filterCleared === 'cleared' && a.cleared) ||
        (this.filterCleared === 'not_cleared' && !a.cleared);
      return byType && byCleared;
    });
  }

  openAddAlertModal(): void {
    this.formMode = 'create';
    this.resetAlertForm();
    this.isAlertFormModalOpen = true;
  }

  openEditAlertModal(alert: Alert): void {
    if (!alert) return;
    this.formMode = 'edit';
    this.editingAlertId = alert.id;

    this.alertForm = {
      binId: alert.binId,
      ts: alert.ts,
      type: alert.type,
      value: alert.value,
      cleared: alert.cleared
    };

    this.deselectAlert();
    this.isAlertFormModalOpen = true;
  }

  closeAlertFormModal(): void {
    this.isAlertFormModalOpen = false;
    this.editingAlertId = null;
    this.resetAlertForm();
  }

  resetAlertForm(): void {
    this.alertForm = {
      binId: '',
      ts: new Date().toISOString(),
      type: 'THRESHOLD',
      value: 0,
      cleared: false
    };
  }

  onSubmitAlertForm(): void {
    if (this.formMode === 'create') {
      this.alertService.createAlert(this.alertForm).subscribe({
        next: (a: Alert) => {
          this.alerts.push(a);
          this.notificationService.showToast('Alert added successfully', 'success');
          this.closeAlertFormModal();
        },
        error: (err: any) => {
          console.error('Error adding alert:', err);
          this.notificationService.showToast('Failed to add alert', 'error');
        }
      });
      return;
    }

    if (!this.editingAlertId) return;

    const payload: UpdateAlertDto = { ...this.alertForm };
    this.alertService.updateAlert(this.editingAlertId, payload).subscribe({
      next: (updated: any) => {
        this.alerts = this.alerts.map(a => a.id === this.editingAlertId ? updated : a);
        this.notificationService.showToast('Alert updated successfully', 'success');
        this.closeAlertFormModal();
      },
      error: (err: any) => {
        console.error('Error updating alert:', err);
        this.notificationService.showToast('Failed to update alert', 'error');
      }
    });
  }

  selectAlert(alert: Alert): void {
    this.selectedAlert = alert;
  }

  deselectAlert(): void {
    this.selectedAlert = null;
  }

  onDeleteAlert(alertId: string): void {
    if (!confirm('Are you sure you want to delete this alert?')) return;
    this.alertService.deleteAlert(alertId).subscribe({
      next: () => {
        this.alerts = this.alerts.filter(a => a.id !== alertId);
        if (this.selectedAlert?.id === alertId) this.deselectAlert();
        this.notificationService.showToast('Alert deleted successfully', 'success');
      },
      error: (err: any) => {
        console.error('Error deleting alert:', err);
        this.notificationService.showToast('Failed to delete alert', 'error');
      }
    });
  }

  formatDate(iso?: string): string {
    if (!iso) return 'N/A';
    return new Date(iso).toLocaleString();
  }

  getTypeLabel(type: AlertType): string {
    // friendly labels
    switch (type) {
      case 'THRESHOLD': return 'Threshold';
      case 'SENSOR_ANOMALY': return 'Sensor Anomaly';
      case 'LEVEL_HIGH': return 'Level High';
      case 'LEVEL_LOW': return 'Level Low';
      case 'LEVEL_CRITICAL': return 'Level Critical';
      case 'LEVEL': return 'Level';
      case 'BATTERY_LOW': return 'Battery Low';
      default: return type;
    }
  }

  getAlertBadgeClass(alert: Alert): string {
    // color coding similar style approach to vehicle status
    if (alert.type === 'LEVEL_CRITICAL' || alert.type === 'SENSOR_ANOMALY') return 'badge-critical';
    if (alert.type === 'LEVEL_HIGH' || alert.type === 'BATTERY_LOW') return 'badge-high';
    if (alert.type === 'LEVEL_LOW') return 'badge-low';
    return 'badge-default';
  }

  toggleCleared(alert: Alert): void {
    const payload: UpdateAlertDto = { cleared: !alert.cleared };
    this.alertService.updateAlert(alert.id, payload).subscribe({
      next: (updated: any) => {
        this.alerts = this.alerts.map(a => a.id === alert.id ? updated : a);
        this.notificationService.showToast('Alert status updated', 'success');
      },
      error: (err: any) => {
        console.error('Error toggling cleared:', err);
        this.notificationService.showToast('Failed to update alert', 'error');
      }
    });
  }
  convertToISOString(value: string | null | undefined): string 
  {
    if (!value) return '';
    return new Date(value).toISOString();
  }

  goToDashboard(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  

  


}
