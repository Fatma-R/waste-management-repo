import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { Incident, CreateIncidentDto, UpdateIncidentDto } from '../../shared/models/incident.model';

@Injectable({
  providedIn: 'root'
})
export class IncidentService {

  constructor(private api: ApiService) {}

  // Récupère tous les incidents (nouvelle méthode)
  getAllIncidents(): Observable<Incident[]> {
    return this.api.get<Incident[]>('/incidents');
  }

  // Ancienne méthode existante
  getIncidents(): Observable<Incident[]> {
    return this.api.get<Incident[]>('/incidents');
  }

  getIncidentById(id: string): Observable<Incident> {
    return this.api.get<Incident>(`/incidents/${id}`);
  }

  createIncident(dto: CreateIncidentDto): Observable<Incident> {
    return this.api.post<Incident>('/incidents', dto);
  }

  resolveIncident(id: string): Observable<Incident> {
    return this.api.post<Incident>(`/incidents/${id}/resolve`, {});
  }

  updateIncident(id: string, dto: UpdateIncidentDto): Observable<Incident> {
    return this.api.put<Incident>(`/incidents/${id}`, dto);
  }


  deleteIncident(id: string): Observable<void> {
    return this.api.delete<void>(`/incidents/${id}`);
  }
}
