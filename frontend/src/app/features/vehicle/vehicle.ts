import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { VehicleService } from '../../core/services/vehicle';
import { NotificationService } from '../../core/services/notification';
import { Vehicle, CreateVehicleDto, UpdateVehicleDto, VehicleStatus, FuelType} from '../../shared/models/vehicle.model';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    ModalComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './vehicle.html',
  styleUrls: ['./vehicle.scss']
})
export class VehiclesComponent implements OnInit {
  vehicles: Vehicle[] = [];
  selectedVehicle: Vehicle | null = null;
  isLoading = true;

  isVehicleFormModalOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingVehicleId: string | null = null;

  // Form model - reste en Coordinates pour le formulaire
  vehicleForm: CreateVehicleDto = {
    plateNumber: '',
    capacityVolumeL: 0,
    currentLocation: { coordinates: [0, 0], type: 'Point' }, // <-- utilisé pour formulaire
    fuelType: 'DIESEL',
    status: 'AVAILABLE'
  };

  filterStatus: 'all' | VehicleStatus = 'all';

  constructor(
    private vehicleService: VehicleService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.isLoading = true;
    this.vehicleService.getVehicles().subscribe({
      next: (vehicles) => {
        // On garde GeoJSON pour Vehicle
        this.vehicles = vehicles;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading vehicles:', err);
        this.notificationService.showToast('Failed to load vehicles', 'error');
        this.isLoading = false;
      }
    });
  }

  get filteredVehicles(): Vehicle[] {
    return this.vehicles.filter(v => this.filterStatus === 'all' || v.status === this.filterStatus);
  }

  openAddVehicleModal(): void {
    this.formMode = 'create';
    this.resetVehicleForm();
    this.isVehicleFormModalOpen = true;
  }

  openEditVehicleModal(vehicle: Vehicle): void {
    if (!vehicle) return;
    console.log('Editing vehicle:', vehicle); // Debugging vehicle data

    this.formMode = 'edit';
    this.editingVehicleId = vehicle.id;

    this.vehicleForm = {
      plateNumber: vehicle.plateNumber,
      capacityVolumeL: vehicle.capacityVolumeL,
      currentLocation: {
        coordinates: [vehicle.currentLocation?.coordinates[0] || 0, vehicle.currentLocation?.coordinates[1] || 0],
        type: 'Point'
      },
      fuelType: vehicle.fuelType,
      status: vehicle.status
    };

    this.deselectVehicle();
    this.isVehicleFormModalOpen = true;
  }

  closeVehicleFormModal(): void {
    this.isVehicleFormModalOpen = false;
    this.editingVehicleId = null;
    this.resetVehicleForm();
  }

  resetVehicleForm(): void {
    this.vehicleForm = {
      plateNumber: '',
      capacityVolumeL: 0,
      currentLocation: { coordinates: [0, 0], type: 'Point' },
      fuelType: 'DIESEL',
      status: 'AVAILABLE'
    };
  }
  onSubmitVehicleForm(): void {
    console.log('Submitting vehicle form:', this.vehicleForm); // Debugging form data

    if (this.formMode === 'create') {
      this.vehicleService.createVehicle(this.vehicleForm).subscribe({
        next: (v) => {
          console.log('Vehicle created successfully:', v); // Debugging success response
          this.vehicles.push(v);
          this.notificationService.showToast('Vehicle added successfully', 'success');
          this.closeVehicleFormModal();
        },
        error: (err) => {
          console.error('Error adding vehicle:', err);
          this.notificationService.showToast('Failed to add vehicle', 'error');
        }
      });
      return;
    }

    if (!this.editingVehicleId) return;

    this.vehicleService.updateVehicle(this.editingVehicleId, this.vehicleForm).subscribe({
      next: (updated) => {
        console.log('Vehicle updated successfully:', updated); // Debugging success response
        this.vehicles = this.vehicles.map(v =>
          v.id === this.editingVehicleId ? updated : v
        );
        this.notificationService.showToast('Vehicle updated successfully', 'success');
        this.closeVehicleFormModal();
      },
      error: (err) => {
        console.error('Error updating vehicle:', err);
        this.notificationService.showToast('Failed to update vehicle', 'error');
      }
    });
  }

  selectVehicle(vehicle: Vehicle): void {
    this.selectedVehicle = vehicle;
  }

  deselectVehicle(): void {
    this.selectedVehicle = null;
  }

  onDeleteVehicle(vehicleId: string): void {
    if (!confirm('Are you sure you want to delete this vehicle?')) return;
    this.vehicleService.deleteVehicle(vehicleId).subscribe({
      next: () => {
        this.vehicles = this.vehicles.filter(v => v.id !== vehicleId);
        if (this.selectedVehicle?.id === vehicleId) this.deselectVehicle();
        this.notificationService.showToast('Vehicle deleted successfully', 'success');
      },
      error: (err) => {
        console.error('Error deleting vehicle:', err);
        this.notificationService.showToast('Failed to delete vehicle', 'error');
      }
    });
  }

  getStatusLabel(status: VehicleStatus): string {
    switch (status) {
      case 'AVAILABLE': return 'Available';
      case 'IN_SERVICE': return 'In Service';
      case 'MAINTENANCE': return 'Maintenance';
      default: return status;
    }
  }

  getFuelLabel(fuel: FuelType): string {
    switch (fuel) {
      case 'DIESEL': return 'Diesel';
      case 'GASOLINE': return 'Gasoline';
      case 'ELECTRIC': return 'Electric';
      case 'HYBRID': return 'Hybrid';
      default: return fuel;
    }
  }

  formatDate(iso?: string): string {
    if (!iso) return 'N/A';
    return new Date(iso).toLocaleString();
  }
  getLatitude(vehicle: Vehicle | null): number | null {
    if (!vehicle || !vehicle.currentLocation || !vehicle.currentLocation.coordinates) return null;
    return vehicle.currentLocation.coordinates[1]; // Latitude est à l'index 1
  }

  getLongitude(vehicle: Vehicle | null): number | null {
    if (!vehicle || !vehicle.currentLocation || !vehicle.currentLocation.coordinates) return null;
    return vehicle.currentLocation.coordinates[0]; // Longitude est à l'index 0
  }

}