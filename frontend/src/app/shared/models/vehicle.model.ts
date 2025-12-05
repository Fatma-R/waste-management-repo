import { GeoJSONPoint } from "./collection-point.model";

export type VehicleStatus = 'AVAILABLE' | 'IN_SERVICE' | 'MAINTENANCE';
export type FuelType = 'DIESEL' | 'GASOLINE' | 'ELECTRIC' | 'HYBRID';

export interface Vehicle {
  id: string;
  plateNumber: string;
  capacityVolumeL: number;
  fuelType: FuelType;
  status: VehicleStatus;
  type?: string;        
  zoneLabel?: string;

  // Backend format
  currentLocation: GeoJSONPoint;

  createdAt?: string;
  updatedAt?: string;
}

// What frontend sends to backend
export interface CreateVehicleDto {
  plateNumber: string;
  capacityVolumeL: number;
  currentLocation: GeoJSONPoint;
  fuelType: FuelType;
  status: VehicleStatus;
}

export interface UpdateVehicleDto {
  plateNumber?: string;
  capacityVolumeL?: number;
  currentLocation?: GeoJSONPoint;
  fuelType?: FuelType;
  status?: VehicleStatus;
}
