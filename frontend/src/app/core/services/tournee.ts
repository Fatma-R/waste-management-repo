import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { Tournee } from '../../shared/models/tournee.model';
import { TrashType } from '../../shared/models/bin.model';

@Injectable({
  providedIn: 'root'
})
export class TourneeService {
  constructor(private api: ApiService) {}

  getTournees(): Observable<Tournee[]> {
    return this.api.get<Tournee[]>('/tournees');
  }

  getTourneeById(id: string): Observable<Tournee> {
    return this.api.get<Tournee>(`/tournees/${id}`);
  }

  /**
   * Planifie une tournée via VROOM.
   * Correspond à POST /api/v1/tournees/plan?type=...&threshold=...
   */
  planTournee(type: TrashType, threshold: number): Observable<Tournee[]> {
    const url = `/tournees/plan?type=${type}&threshold=${threshold}`;
    // body vide, on passe tout en query params
    return this.api.post<Tournee[]>(url, {});
  }

  deleteTournee(id: string): Observable<void> {
    return this.api.delete<void>(`/tournees/${id}`);
  }
}
