import { GeoJSONPoint } from './collection-point.model';

export interface Depot {
  id: string;
  name: string;
  address: string;
  location: GeoJSONPoint;
}

export interface UpsertDepotDto {
  name: string;
  address: string;
  location: GeoJSONPoint;
}
