import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Bin, TrashType, CreateBinDto, UpdateBinDto } from '../../shared/models/bin.model';
import { BinService } from '../../core/services/bin';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-bin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './bin.html',
  styleUrls: ['./bin.scss']
})
export class BinComponent implements OnInit {
  
  bins: Bin[] = [];
  filteredBins: Bin[] = [];

  selectedBin: Bin | null = null;
  isLoading = true;

  // Filters
  filterType: string = 'all';
  filterStatus: string = 'all';

  // Modal form
  isBinFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingBinId: string | null = null;

  binForm: CreateBinDto = {
    collectionPointId: '',
    active: true,
    type: TrashType.PLASTIC,
    readingIds: [],
    alertIds: []
  };

  // Enum lists
  trashTypes = Object.values(TrashType);

  constructor(private binService: BinService) {}

  ngOnInit(): void {
    this.loadBins();
  }

  loadBins(): void {
    this.isLoading = true;
    this.binService.getBins().subscribe({
      next: (bins) => {
        this.bins = bins;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading bins:', err);
        this.isLoading = false;
      }
    });
  }

  // -----------------
  // FILTER LOGIC
  // -----------------
  applyFilters(): void {
    this.filteredBins = this.bins.filter(bin => {
      const typeMatch = this.filterType === 'all' || bin.type === this.filterType;
      const statusMatch = this.filterStatus === 'all' ||
                          (this.filterStatus === 'active' && bin.active) ||
                          (this.filterStatus === 'inactive' && !bin.active);
      return typeMatch && statusMatch;
    });
  }

  // trigger filters when selects change
  onFilterChange(): void {
    this.applyFilters();
  }

  // -----------------
  // CREATE / EDIT
  // -----------------
  openCreateBinModal(): void {
    this.formMode = 'create';
    this.resetBinForm();
    this.isBinFormModalOpen = true;
  }

  openEditBinModal(bin: Bin): void {
    this.formMode = 'edit';
    this.editingBinId = bin.id;
    this.binForm = { ...bin };
    this.selectedBin = null;
    this.isBinFormModalOpen = true;
  }

  closeBinFormModal(): void {
    this.isBinFormModalOpen = false;
    this.editingBinId = null;
    this.resetBinForm();
  }

  resetBinForm(): void {
    this.binForm = {
      collectionPointId: '',
      active: true,
      type: TrashType.PLASTIC,
      readingIds: [],
      alertIds: []
    };
  }

  onSubmitBinForm(): void {
    if (this.formMode === 'create') {
      this.binService.createBin(this.binForm).subscribe({
        next: (bin) => {
          this.bins.push(bin);
          this.applyFilters();
          this.closeBinFormModal();
        },
        error: (err) => console.error('Error creating bin', err)
      });
      return;
    }

    if (!this.editingBinId) return;

    this.binService.updateBin(this.editingBinId, this.binForm).subscribe({
      next: (updated) => {
        this.bins = this.bins.map(b => b.id === this.editingBinId ? updated : b);
        this.applyFilters();
        this.closeBinFormModal();
      },
      error: (err) => console.error('Error updating bin', err)
    });
  }

  // -----------------
  // DELETE
  // -----------------
  selectBin(bin: Bin): void {
    this.selectedBin = bin;
  }

  deselectBin(): void {
    this.selectedBin = null;
  }

  onDeleteBin(binId: string): void {
    if (!confirm('Are you sure you want to delete this bin?')) return;
    this.binService.deleteBin(binId).subscribe({
      next: () => {
        this.bins = this.bins.filter(b => b.id !== binId);
        this.applyFilters();
        if (this.selectedBin?.id === binId) this.deselectBin();
      },
      error: (err) => console.error('Error deleting bin', err)
    });
  }

  // -----------------
  // UI HELPERS
  // -----------------
  getBinTypeLabel(type: TrashType): string {
    const labels: Record<TrashType, string> = {
      PLASTIC: 'Plastic',
      ORGANIC: 'Organic',
      PAPER: 'Paper',
      GLASS: 'Glass',
    };
    return labels[type];
  }

  getBinTypeIcon(type: TrashType): string {
    const icons: Record<TrashType, string> = {
      PLASTIC: '♻️',
      ORGANIC: '🌱',
      PAPER: '📄',
      GLASS: '🧪',
    };
    return icons[type];
  }
}
