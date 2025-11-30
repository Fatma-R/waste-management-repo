import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BinReading, CreateBinReadingDto, UpdateBinReadingDto } from '../../shared/models/bin-reading.model';
import { BinReadingService } from '../../core/services/bin-reading';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-bin-reading',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './bin-reading.html',
  styleUrls: ['./bin-reading.scss']
})
export class BinReadingComponent implements OnInit {
  
  binReadings: BinReading[] = [];
  filteredBinReadings: BinReading[] = [];

  selectedReading: BinReading | null = null;
  isLoading = true;

  // Bin specific view
  binId: string | null = null;
  cpId: string | null = null;

  // Modal form
  isFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingReadingId: string | null = null;

  binReadingForm: CreateBinReadingDto = {
    binId: '',
    ts: new Date(),
    fillPct: 0,
    batteryPct: 0,
    temperatureC: 0,
    signalDbm: 0
  };

  constructor(
    private binReadingService: BinReadingService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.cpId = params['cpId'];
      this.binId = params['binId'];
      if (this.binId) {
        this.loadReadingsForBin(this.binId);
      }
    });
  }

  loadReadingsForBin(binId: string): void {
    this.isLoading = true;
    this.binReadingService.getBinReadings().subscribe({
      next: (readings) => {
        // Filter readings by binId
        this.binReadings = readings.filter(r => r.binId === binId);
        this.filteredBinReadings = this.binReadings;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading bin readings:', err);
        this.isLoading = false;
      }
    });
  }

  // -----------------
  // CREATE / EDIT
  // -----------------
  openCreateReadingModal(): void {
    this.formMode = 'create';
    this.resetForm();
    // Auto-fill binId if in bin view
    if (this.binId) {
      this.binReadingForm.binId = this.binId;
    }
    this.isFormModalOpen = true;
  }

  openEditReadingModal(reading: BinReading): void {
    this.formMode = 'edit';
    this.editingReadingId = reading.id;
    this.binReadingForm = { ...reading };
    this.selectedReading = null;
    this.isFormModalOpen = true;
  }

  closeFormModal(): void {
    this.isFormModalOpen = false;
    this.editingReadingId = null;
    this.resetForm();
  }

  resetForm(): void {
    this.binReadingForm = {
      binId: '',
      ts: new Date(),
      fillPct: 0,
      batteryPct: 0,
      temperatureC: 0,
      signalDbm: 0
    };
  }

  onSubmitForm(): void {
    if (this.formMode === 'create') {
      this.binReadingService.createBinReading(this.binReadingForm).subscribe({
        next: (reading) => {
          this.binReadings.push(reading);
          this.filteredBinReadings = this.binReadings;
          this.closeFormModal();
        },
        error: (err) => console.error('Error creating bin reading', err)
      });
      return;
    }

    if (!this.editingReadingId) return;

    this.binReadingService.updateBinReading(this.editingReadingId, this.binReadingForm as UpdateBinReadingDto).subscribe({
      next: (updated) => {
        this.binReadings = this.binReadings.map(r => r.id === this.editingReadingId ? updated : r);
        this.filteredBinReadings = this.binReadings;
        this.closeFormModal();
      },
      error: (err) => console.error('Error updating bin reading', err)
    });
  }

  // -----------------
  // DELETE
  // -----------------
  selectReading(reading: BinReading): void {
    this.selectedReading = reading;
  }

  deselectReading(): void {
    this.selectedReading = null;
  }

  onDeleteReading(readingId: string): void {
    if (!confirm('Are you sure you want to delete this reading?')) return;
    this.binReadingService.deleteBinReading(readingId).subscribe({
      next: () => {
        this.binReadings = this.binReadings.filter(r => r.id !== readingId);
        this.filteredBinReadings = this.binReadings;
        if (this.selectedReading?.id === readingId) this.deselectReading();
      },
      error: (err) => console.error('Error deleting bin reading', err)
    });
  }

  // -----------------
  // UI HELPERS
  // -----------------
  formatDate(date: Date): string {
    return new Date(date).toLocaleString();
  }

  getStatusClass(percentage: number, type: 'fill' | 'battery'): string {
    if (type === 'fill') {
      if (percentage >= 75) return 'status-red';      // Critical
      if (percentage >= 50) return 'status-orange';   // Warning
      return 'status-green';                          // Good
    } else {
      // Battery
      if (percentage <= 20) return 'status-red';      // Critical
      if (percentage <= 50) return 'status-orange';   // Warning
      return 'status-green';                          // Good
    }
  }

  // -----------------
  // NAVIGATION
  // -----------------
  goToDashboard(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  goBackToBins(): void {
    if (this.cpId) {
      this.router.navigate(['/admin/collection-points', this.cpId, 'bins']);
    }
  }
}
