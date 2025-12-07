import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardComponent } from '../../shared/components/card/card';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { AlertService } from '../../core/services/alert';
import { Alert, AlertType } from '../../shared/models/alert.model';

@Component({
selector: 'app-employee-alerts',
standalone: true,
imports: [
CommonModule,
FormsModule,
CardComponent,
LoadingSpinnerComponent
],
templateUrl: './employee-alerts.html',
styleUrls: ['./employee-alerts.scss']
})
export class EmployeeAlertsComponent implements OnInit {
alerts: Alert[] = [];
isLoading = true;

filterType: 'all' | AlertType = 'all';
filterCleared: 'all' | 'cleared' | 'not_cleared' = 'all';

constructor(private alertService: AlertService) {}

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
            this.isLoading = false;
        }
    });
}

get filteredAlerts(): Alert[] {
return this.alerts.filter(a => {
const byType = this.filterType === 'all' || a.severity === this.filterType;
const byCleared =
this.filterCleared === 'all' ||
(this.filterCleared === 'cleared' && a.cleared) ||
(this.filterCleared === 'not_cleared' && !a.cleared);
return byType && byCleared;
});
}

formatDate(iso?: string): string {
if (!iso) return 'N/A';
return new Date(iso).toLocaleString();
}

getTypeLabel(severity: AlertType): string {
switch (severity) {
case 'THRESHOLD': return 'Threshold';
case 'SENSOR_ANOMALY': return 'Sensor Anomaly';
case 'LEVEL_HIGH': return 'Level High';
case 'LEVEL_LOW': return 'Level Low';
case 'LEVEL_CRITICAL': return 'Level Critical';
case 'BATTERY_LOW': return 'Battery Low';
default: return severity;
}
}

getAlertBadgeClass(alert: Alert): string {
if (alert.severity === 'LEVEL_CRITICAL' || alert.severity === 'SENSOR_ANOMALY') return 'badge-critical';
if (alert.severity === 'LEVEL_HIGH' || alert.severity === 'BATTERY_LOW') return 'badge-high';
if (alert.severity === 'LEVEL_LOW') return 'badge-low';
return 'badge-default';
}

}
