// src/app/core/services/vehicle.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api';
import { Vehicle, CreateVehicleDto, UpdateVehicleDto } from '../../shared/models/vehicle.model';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  constructor(private api: ApiService) {}

  getVehicles(): Observable<Vehicle[]> {
    return this.api.get<Vehicle[]>('/vehicles');
  }
  

  getVehicleById(id: string): Observable<Vehicle> {
    return this.api.get<Vehicle>(`/vehicles/${id}`);
  }
  createVehicle(payload: CreateVehicleDto): Observable<Vehicle> {
    const body = {
      ...payload,
      coordinates: {
        type: 'Point',
        coordinates: [payload.coordinates.longitude, payload.coordinates.latitude]
      }
    };
    return this.api.post<Vehicle>('/vehicles', body);
  }

  updateVehicle(id: string, payload: UpdateVehicleDto): Observable<Vehicle> {
    const body = {
      ...payload,
      coordinates: payload.coordinates ? {
        type: 'Point',
        coordinates: [payload.coordinates.longitude, payload.coordinates.latitude]
      } : undefined
    };
    return this.api.put<Vehicle>(`/vehicles/${id}`, body);
  }


  deleteVehicle(id: string): Observable<void> {
    return this.api.delete<void>(`/vehicles/${id}`);
  }
  
  getMockVehicles(): Observable<Vehicle[]> {
  const mockVehicles: Vehicle[] = [
    {
      id: '1',
      plateNumber: 'TN-1234',
      capacityVolumeL: 5000,
      coordinates: { type: 'Point', coordinates: [10.123, 36.456] },
      fuelType: 'DIESEL',
      status: 'AVAILABLE'
    },
    {
      id: '2',
      plateNumber: 'TN-5678',
      capacityVolumeL: 3000,
      coordinates: { type: 'Point', coordinates: [10.789, 36.789] },
      fuelType: 'GASOLINE',
      status: 'IN_SERVICE'
    }
  ];
  return of(mockVehicles);
}

}

  
