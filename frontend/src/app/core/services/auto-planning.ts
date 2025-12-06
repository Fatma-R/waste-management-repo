import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { AutoMode } from '../../shared/models/AutoMoode.model';

@Injectable({ providedIn: 'root' })
export class AutoPlanningService {
  constructor(private api: ApiService) {}

  getMode(): Observable<AutoMode> {
    return this.api.get<AutoMode>('/auto-planning/mode');
  }

  setMode(mode: AutoMode): Observable<void> {
    return this.api.post<void>(`/auto-planning/mode/${mode}`, {});
  }

  runScheduledCycle(): Observable<void> {
    return this.api.post<void>('/auto-planning/run/scheduled', {});
  }

  runEmergencyLoop(): Observable<void> {
    return this.api.post<void>('/auto-planning/run/emergency', {});
  }
}
