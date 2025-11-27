import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { CollectionPoint, CreateCollectionPointDto, UpdateCollectionPointDto } from '../../shared/models/collection-point.model';

@Injectable({
  providedIn: 'root'
})
export class CollectionPointService {
  constructor(private api: ApiService) {}

  getCollectionPoints(): Observable<CollectionPoint[]> {
    return this.api.get<CollectionPoint[]>('/collectionPoints');
  }

  getCollectionPointById(id: string): Observable<CollectionPoint> {
    return this.api.get<CollectionPoint>(`/collectionPoints/${id}`);
  }

  createCollectionPoint(payload: CreateCollectionPointDto): Observable<CollectionPoint> {
    return this.api.post<CollectionPoint>('/collectionPoints', payload);
  }

  updateCollectionPoint(id: string, payload: UpdateCollectionPointDto): Observable<CollectionPoint> {
    return this.api.put<CollectionPoint>(`/collectionPoints/${id}`, payload);
  }

  deleteCollectionPoint(id: string): Observable<void> {
    return this.api.delete<void>(`/collectionPoints/${id}`);
  }
}
