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

  planTournees(types: TrashType[], threshold: number): Observable<Tournee[]> {
    // Build query string with repeated ?types=...
    const params = new URLSearchParams();
    types.forEach(t => params.append('types', t)); // PLASTIC, ORGANIC, ...
    params.append('threshold', String(threshold));
    const url = `/tournees/plan?${params.toString()}`;
    // Body is still empty, everything is in query params
    return this.api.post<Tournee[]>(url, {});
  }

  deleteTournee(id: string): Observable<void> {
    return this.api.delete<void>(`/tournees/${id}`);
  }
}
