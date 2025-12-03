export interface GeoJSONPoint {
  type: 'Point';
  coordinates: [number, number];
}

export interface GeoJSONPoint {
  type: "Point";
  coordinates: [number, number]; // [longitude, latitude]
}


export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type IncidentType =
  | 'BLOCKED_STREET'
  | 'TRAFFIC_ACCIDENT'
  | 'POLICE_ACTIVITY'
  | 'ROAD_MAINTENANCE'
  | 'PUBLIC_EVENT'
  | 'NATURAL_OBSTRUCTION'
  | 'FIRE_BLOCKAGE'
  | 'OTHER';

export interface Incident {
  id: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  reportedAt?: string;
  resolvedAt?: string;
  //reportedBy?: string;
  description?: string;
  location: GeoJSONPoint;
  createdAt?: string;
  updatedAt?: string;
  
}

export interface CreateIncidentDto {
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  reportedAt?: string;
  resolvedAt?: string;
  //reportedBy?: string;
  description?: string;
  location: GeoJSONPoint;
  
}

export interface UpdateIncidentDto {
  type?: IncidentType;
  severity?: IncidentSeverity;
  status?: IncidentStatus;
  reportedAt?: string;
  resolvedAt?: string;
  //reportedBy?: string;
  description?: string;
  location?: GeoJSONPoint;
}
