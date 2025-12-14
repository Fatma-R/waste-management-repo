import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { IncidentService } from '../../core/services/incident';
import { NotificationService } from '../../core/services/notification';

import { Incident, CreateIncidentDto, IncidentSeverity, IncidentStatus, IncidentType } from '../../shared/models/incident.model';

@Component({
  selector: 'app-incidents',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './incident.html',
  styleUrls: ['./incident.scss']
})
export class IncidentsComponent implements OnInit {
  incidents: Incident[] = [];
  selectedIncident: Incident | null = null;
  isLoading = true;

  isIncidentFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingIncidentId: string | null = null;

  // Form model
  incidentForm: CreateIncidentDto = {
    type: 'OTHER',
    severity: 'LOW',
    status: 'OPEN',
    description: '',
    location: { type: "Point", coordinates: [0, 0] }
  };

  // Coordinates for the form
  incidentFormCoordinates = { longitude: 0, latitude: 0 };

  filterStatus: 'all' | IncidentStatus = 'all';

  constructor(
    private incidentService: IncidentService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadIncidents();
  }

  loadIncidents(): void {
    this.isLoading = true;
    this.incidentService.getIncidents().subscribe({
      next: (data) => {
        this.incidents = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading incidents:', err);
        this.notificationService.showToast('Failed to load incidents', 'error');
        this.isLoading = false;
      }
    });
  }

  get filteredIncidents(): Incident[] {
    return this.incidents.filter(i => this.filterStatus === 'all' || i.status === this.filterStatus);
  }

  openAddIncidentModal(): void {
    this.formMode = 'create';
    this.resetIncidentForm();
    this.isIncidentFormModalOpen = true;
  }

  openEditIncidentModal(incident: Incident): void {
    this.formMode = 'edit';
    this.editingIncidentId = incident.id;

    this.incidentForm = {
      type: incident.type,
      severity: incident.severity,
      status: incident.status,
      description: incident.description,
      location: { type: "Point", coordinates: [0, 0] } // will set from coordinates
    };

    // Fill form coordinates
    this.incidentFormCoordinates = {
      longitude: incident.location.coordinates[0],
      latitude: incident.location.coordinates[1]
    };

    this.selectedIncident = null;
    this.isIncidentFormModalOpen = true;
  }

  closeIncidentFormModal(): void {
    this.isIncidentFormModalOpen = false;
    this.editingIncidentId = null;
    this.resetIncidentForm();
  }

  resetIncidentForm(): void {
    this.incidentForm = {
      type: 'OTHER',
      severity: 'LOW',
      status: 'OPEN',
      description: '',
      location: { type: "Point", coordinates: [0, 0] }
    };
    this.incidentFormCoordinates = { longitude: 0, latitude: 0 };
  }

  onSubmitIncidentForm(): void {
    // Reconstruct GeoJSON from form coordinates
    this.incidentForm.location = {
      type: "Point",
      coordinates: [
        this.incidentFormCoordinates.longitude,
        this.incidentFormCoordinates.latitude
      ]
    };

    if (this.formMode === 'create') {
      this.incidentService.createIncident(this.incidentForm).subscribe({
        next: (incident) => {
          this.incidents.push(incident);
          this.notificationService.showToast('Incident added successfully', 'success');
          this.closeIncidentFormModal();
        },
        error: (err) => {
          console.error('Error creating incident:', err);
          this.notificationService.showToast('Failed to add incident', 'error');
        }
      });
      return;
    }

    if (!this.editingIncidentId) return;

    this.incidentService.updateIncident(this.editingIncidentId, this.incidentForm).subscribe({
      next: (updated: Incident) => {
        this.incidents = this.incidents.map(i => i.id === this.editingIncidentId ? updated : i);
        this.notificationService.showToast('Incident updated successfully', 'success');
        this.closeIncidentFormModal();
      },
      error: (err) => {
        console.error('Error updating incident:', err);
        this.notificationService.showToast('Failed to update incident', 'error');
      }
    });
  }

  selectIncident(incident: Incident): void {
    this.selectedIncident = incident;
  }

  deselectIncident(): void {
    this.selectedIncident = null;
  }

  onDeleteIncident(id: string): void {
    if (!confirm('Are you sure you want to delete this incident?')) return;
    this.incidentService.deleteIncident(id).subscribe({
      next: () => {
        this.incidents = this.incidents.filter(i => i.id !== id);
        if (this.selectedIncident?.id === id) this.deselectIncident();
        this.notificationService.showToast('Incident deleted successfully', 'success');
      },
      error: (err) => {
        console.error('Error deleting incident:', err);
        this.notificationService.showToast('Failed to delete incident', 'error');
      }
    });
  }

  getSeverityLabel(severity: IncidentSeverity): string {
    return severity.charAt(0) + severity.slice(1).toLowerCase();
  }

  formatDate(iso?: string): string {
    if (!iso) return 'N/A';
    return new Date(iso).toLocaleString();
  }

  getLatitude(incident: Incident | null): number | null {
    return incident?.location?.coordinates ? incident.location.coordinates[1] : null;
  }

  getLongitude(incident: Incident | null): number | null {
    return incident?.location?.coordinates ? incident.location.coordinates[0] : null;
  }

  goToDashboard(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  resolveIncident(id: string): void {
    this.incidentService.resolveIncident(id).subscribe({
      next: (updated: Incident) => {
        this.incidents = this.incidents.map(i => i.id === id ? updated : i);
        if (this.selectedIncident?.id === id) this.selectedIncident = updated;
        this.notificationService.showToast('Incident resolved successfully', 'success');
      },
      error: (err) => {
        console.error('Error resolving incident:', err);
        this.notificationService.showToast('Failed to resolve incident', 'error');
      }
    });
  }

  exportIncidentsCsv(): void {
    const headers = ['id','type','severity','status','description','longitude','latitude','reportedAt'];
    const toCell = (v: unknown) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const rows = this.incidents.map(i => [
      i.id, i.type, i.severity, i.status, i.description,
      i.location?.coordinates?.[0], i.location?.coordinates?.[1], i['reportedAt']
    ]);
    const csv = [headers.join(','), ...rows.map(r => r.map(toCell).join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'incidents.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
