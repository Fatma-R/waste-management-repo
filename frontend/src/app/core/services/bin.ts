  import { Injectable } from '@angular/core';
  import { Observable } from 'rxjs';
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

    createBin(payload: CreateBinDto): Observable<Bin> {
      return this.api.post<Bin>('/bins', payload);
    }
    

    updateBin(id: string, payload: UpdateBinDto): Observable<Bin> {
      return this.api.put<Bin>(`/bins/${id}`, payload);
    }

    deleteBin(id: string): Observable<void> {
      return this.api.delete<void>(`/bins/${id}`);
    }
  }
