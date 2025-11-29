// GEOJSON format from backend
export interface GeoJSONPoint {
  type: 'Point';
  coordinates: [number, number]; // [longitude, latitude]
}

// Friendly frontend structure
export interface Coordinates {
  longitude: number;
  latitude: number;
}

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
  coordinates: GeoJSONPoint;

  createdAt?: string;
  updatedAt?: string;
}

// What frontend sends to backend
export interface CreateVehicleDto {
  plateNumber: string;
  capacityVolumeL: number;
  coordinates: Coordinates; // frontend format
  fuelType: FuelType;
  status: VehicleStatus;
}

export interface UpdateVehicleDto {
  plateNumber?: string;
  capacityVolumeL?: number;
  coordinates?: Coordinates; // frontend format
  fuelType?: FuelType;
  status?: VehicleStatus;
}
