// src/app/core/services/alert.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { Alert, CreateAlertDto, UpdateAlertDto } from '../../shared/models/alert.model';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  constructor(private api: ApiService) {}

  getAlerts(): Observable<Alert[]> {
    return this.api.get<Alert[]>('/alerts');
  }

  getAlertById(id: string): Observable<Alert> {
    return this.api.get<Alert>(`/alerts/${id}`);
  }

  createAlert(payload: CreateAlertDto): Observable<Alert> {
    
    const body = {
      ...payload,
      ts: payload.ts ? payload.ts : new Date().toISOString(),
      cleared: payload.cleared ?? false
    };
    return this.api.post<Alert>('/alerts', body);
  }

  updateAlert(id: string, payload: UpdateAlertDto): Observable<Alert> {
    const body = {
      ...payload,
      ts: payload.ts ? payload.ts : undefined
    };
    return this.api.put<Alert>(`/alerts/${id}`, body);
  }

  deleteAlert(id: string): Observable<void> {
    return this.api.delete<void>(`/alerts/${id}`);
  }

  // Backend endpoints provided in your controller:
  getAlertsByBin(binId: string) {
    return this.api.get<Alert[]>(`/alerts/bin/${binId}`);
  }

  getAlertsByType(type: string) {
    return this.api.get<Alert[]>(`/alerts/type/${type}`);
  }

  getAlertsByCleared(cleared: boolean) {
    return this.api.get<Alert[]>(`/alerts/cleared/${cleared}`);
  }
}
