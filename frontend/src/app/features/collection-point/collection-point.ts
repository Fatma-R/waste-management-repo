import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CollectionPoint, CreateCollectionPointDto, UpdateCollectionPointDto, GeoJSONPoint } from '../../shared/models/collection-point.model';
import { CollectionPointService } from '../../core/services/collection-point';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-collection-point',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './collection-point.html',
  styleUrls: ['./collection-point.scss']
})
export class CollectionPointComponent implements OnInit {
  
  collectionPoints: CollectionPoint[] = [];
  filteredCollectionPoints: CollectionPoint[] = [];

  selectedCollectionPoint: CollectionPoint | null = null;
  isLoading = true;

  // Filters
  filterStatus: string = 'all';

  // Modal form
  isFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingId: string | null = null;

  collectionPointForm: CreateCollectionPointDto = {
    location: { type: 'Point', coordinates: [0, 0] },
    active: true,
    adresse: '',
    binIds: []
  };

  constructor(private collectionPointService: CollectionPointService) {}

  ngOnInit(): void {
    this.loadCollectionPoints();
  }

  loadCollectionPoints(): void {
    this.isLoading = true;
    this.collectionPointService.getCollectionPoints().subscribe({
      next: (points) => {
        this.collectionPoints = points;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading collection points:', err);
        this.isLoading = false;
      }
    });
  }

  // -----------------
  // FILTER LOGIC
  // -----------------
  applyFilters(): void {
    this.filteredCollectionPoints = this.collectionPoints.filter(point => {
      const statusMatch = this.filterStatus === 'all' ||
                          (this.filterStatus === 'active' && point.active) ||
                          (this.filterStatus === 'inactive' && !point.active);
      return statusMatch;
    });
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  // -----------------
  // CREATE / EDIT
  // -----------------
  openCreateModal(): void {
    this.formMode = 'create';
    this.resetForm();
    this.isFormModalOpen = true;
  }

  openEditModal(point: CollectionPoint): void {
    this.formMode = 'edit';
    this.editingId = point.id;
    this.collectionPointForm = {
      location: point.location,
      active: point.active,
      adresse: point.adresse,
      binIds: point.bins.map(b => b.id)
    };
    this.selectedCollectionPoint = null;
    this.isFormModalOpen = true;
  }

  closeFormModal(): void {
    this.isFormModalOpen = false;
    this.editingId = null;
    this.resetForm();
  }

  resetForm(): void {
    this.collectionPointForm = {
      location: { type: 'Point', coordinates: [0, 0] },
      active: true,
      adresse: '',
      binIds: []
    };
  }

  onSubmitForm(): void {
    if (this.formMode === 'create') {
      this.collectionPointService.createCollectionPoint(this.collectionPointForm).subscribe({
        next: (point) => {
          this.collectionPoints.push(point);
          this.applyFilters();
          this.closeFormModal();
        },
        error: (err) => console.error('Error creating collection point', err)
      });
      return;
    }

    if (!this.editingId) return;

    this.collectionPointService.updateCollectionPoint(this.editingId, this.collectionPointForm as UpdateCollectionPointDto).subscribe({
      next: (updated) => {
        this.collectionPoints = this.collectionPoints.map(p => p.id === this.editingId ? updated : p);
        this.applyFilters();
        this.closeFormModal();
      },
      error: (err) => console.error('Error updating collection point', err)
    });
  }

  // -----------------
  // DELETE
  // -----------------
  selectCollectionPoint(point: CollectionPoint): void {
    this.selectedCollectionPoint = point;
  }

  deselectCollectionPoint(): void {
    this.selectedCollectionPoint = null;
  }

  onDeleteCollectionPoint(id: string): void {
    if (!confirm('Are you sure you want to delete this collection point?')) return;
    this.collectionPointService.deleteCollectionPoint(id).subscribe({
      next: () => {
        this.collectionPoints = this.collectionPoints.filter(p => p.id !== id);
        this.applyFilters();
        if (this.selectedCollectionPoint?.id === id) this.deselectCollectionPoint();
      },
      error: (err) => console.error('Error deleting collection point', err)
    });
  }

  // -----------------
  // UI HELPERS
  // -----------------
  getCoordinatesString(location: GeoJSONPoint): string {
    return `${location.coordinates[0]}, ${location.coordinates[1]}`;
  }

  getBinCount(point: CollectionPoint): number {
    return point.bins ? point.bins.length : 0;
  }
}
