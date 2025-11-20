import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api';
import { Bin, CreateBinDto, UpdateBinDto } from '../../shared/models/bin.model';

@Injectable({
  providedIn: 'root'
})
export class BinService {
  constructor(private api: ApiService) {}

  getBins(): Observable<Bin[]> {
    return this.api.get<Bin[]>('/bins');
  }

  getBinById(id: string): Observable<Bin> {
    return this.api.get<Bin>(`/bins/${id}`);
  }

  createBin(bin: CreateBinDto): Observable<Bin> {
    return this.api.post<Bin>('/bins', bin);
  }

  updateBin(id: string, bin: UpdateBinDto): Observable<Bin> {
    return this.api.put<Bin>(`/bins/${id}`, bin);
  }

  deleteBin(id: string): Observable<void> {
    return this.api.delete<void>(`/bins/${id}`);
  }

  // Mock data for development
  getMockBins(): Observable<Bin[]> {
    const mockBins: Bin[] = [
      { id: '1', type: 'plastic', latitude: 40.7128, longitude: -74.0060, fillLevel: 85, status: 'active', zone: 'Zone A' },
      { id: '2', type: 'organic', latitude: 40.7589, longitude: -73.9851, fillLevel: 45, status: 'active', zone: 'Zone B' },
      { id: '3', type: 'glass', latitude: 40.7614, longitude: -73.9776, fillLevel: 92, status: 'active', zone: 'Zone A' },
      { id: '4', type: 'paper', latitude: 40.7484, longitude: -73.9857, fillLevel: 30, status: 'active', zone: 'Zone C' },
      { id: '5', type: 'general', latitude: 40.7549, longitude: -73.9840, fillLevel: 67, status: 'active', zone: 'Zone B' }
    ];
    return of(mockBins);
  }
}
