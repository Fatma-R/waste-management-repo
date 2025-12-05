// src/app/shared/models/alert.model.ts

export type AlertType =
  | 'THRESHOLD'
  | 'SENSOR_ANOMALY'
  | 'LEVEL_HIGH'
  | 'LEVEL_LOW'
  | 'LEVEL_CRITICAL'
  | 'LEVEL'
  | 'BATTERY_LOW';

export interface Alert {
message: any;
resolved: any;
severity: any;
  id: string;
  binId: string;
  ts: string; // ISO date string from backend (Date serialized)
  type: AlertType;
  value: number;
  cleared: boolean;

  createdAt?: string;
  updatedAt?: string;
}

// DTOs used by frontend to send data to backend
export interface CreateAlertDto {
  binId: string;
  ts?: string; // optional ISO string, backend will set if omitted
  type: AlertType;
  value: number;
  cleared?: boolean;
}

export interface UpdateAlertDto {
  binId?: string;
  ts?: string;
  type?: AlertType;
  value?: number;
  cleared?: boolean;
}
