import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { TourneeAssignment } from '../../shared/models/tournee-assignment';

@Injectable({
  providedIn: 'root'
})
export class TourneeAssignmentService {
  constructor(private api: ApiService) {}

  /**
   * Auto-assign un véhicule + 3 employés (backend)
   * POST /tournees/{tourneeId}/assignments/auto
   * (ajuste l'URL si ton endpoint est légèrement différent)
   */
  autoAssignForTournee(tourneeId: string): Observable<TourneeAssignment[]> {
    return this.api.post<TourneeAssignment[]>(
      `/tournee-assignments/${tourneeId}/assignments/auto`,
      {}
    );
  }
}
