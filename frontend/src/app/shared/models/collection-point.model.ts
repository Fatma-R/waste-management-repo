import { Bin } from "./bin.model";

export interface GeoJSONPoint {
  type?: string;
  coordinates: [number, number];
}

export interface CollectionPoint {
  id: string;
  location: GeoJSONPoint;
  active: boolean;
  adresse: string;
  bins: Bin[];
}

export interface CreateCollectionPointDto {
  location: GeoJSONPoint;
  active: boolean;
  adresse: string;
  binIds: string[];
}

export interface UpdateCollectionPointDto {
  location: GeoJSONPoint;
  active: boolean;
  adresse: string;
  binIds: string[];
}
