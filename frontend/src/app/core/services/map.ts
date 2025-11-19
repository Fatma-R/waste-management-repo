import { Injectable } from '@angular/core';
import { Bin } from '../../shared/models/bin.model';

export interface MarkerData {
  id: string;
  position: { lat: number; lng: number };
  color: 'green' | 'amber' | 'red';
  type: string;
  fillLevel: number;
}

@Injectable({
  providedIn: 'root'
})
export class MapService {
  
  transformBinsToMarkers(bins: Bin[]): MarkerData[] {
    return bins.map(bin => ({
      id: bin.id,
      position: { lat: bin.latitude, lng: bin.longitude },
      color: this.getFillLevelColor(bin.fillLevel),
      type: bin.type,
      fillLevel: bin.fillLevel
    }));
  }

  getFillLevelColor(fillLevel: number): 'green' | 'amber' | 'red' {
    if (fillLevel >= 80) return 'red';
    if (fillLevel >= 50) return 'amber';
    return 'green';
  }

  getFillLevelLabel(fillLevel: number): string {
    if (fillLevel >= 80) return 'Full';
    if (fillLevel >= 50) return 'Half Full';
    return 'Empty';
  }
}
