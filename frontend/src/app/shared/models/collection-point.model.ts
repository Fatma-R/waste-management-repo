export interface GeoJSONPoint {
  type: string;
  coordinates: [number, number];
}

export interface Bin {
  id: string;
  collectionPointId: string;
  active: boolean;
  type: string;
  readingIds: string[];
  alertIds: string[];
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
