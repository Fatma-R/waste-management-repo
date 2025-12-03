import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { BinReading, CreateBinReadingDto, UpdateBinReadingDto } from '../../shared/models/bin-reading.model';

@Injectable({
  providedIn: 'root'
})
export class BinReadingService {
  constructor(private api: ApiService) {}

  getBinReadings(): Observable<BinReading[]> {
    return this.api.get<BinReading[]>('/bin-readings');
  }

  getBinReadingById(id: string): Observable<BinReading> {
    return this.api.get<BinReading>(`/bin-readings/${id}`);
  }

  createBinReading(payload: CreateBinReadingDto): Observable<BinReading> {
    return this.api.post<BinReading>('/bin-readings', payload);
  }

  updateBinReading(id: string, payload: UpdateBinReadingDto): Observable<BinReading> {
    return this.api.put<BinReading>(`/bin-readings/${id}`, payload);
  }

  deleteBinReading(id: string): Observable<void> {
    return this.api.delete<void>(`/bin-readings/${id}`);
  }

  getLatestBinReadingForBin(binId: string): Observable<BinReading> {
    return this.api.get<BinReading>(`/bin-readings/bin/${binId}/latest`);
  }
}
