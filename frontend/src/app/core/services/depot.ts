import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { Depot, UpsertDepotDto } from '../../shared/models/depot.model';

@Injectable({
  providedIn: 'root'
})
export class DepotService {
  constructor(private api: ApiService) {}

  getMainDepot(): Observable<Depot> {
    // backend: GET /api/v1/depots/main
    return this.api.get<Depot>('/depots/main');
  }

  upsertMainDepot(payload: UpsertDepotDto): Observable<Depot> {
    // backend: PUT /api/v1/depots/main
    return this.api.put<Depot>('/depots/main', payload);
  }

  // additional methods to be added (cruds and such)
}
