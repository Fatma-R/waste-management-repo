// shared/models/tournee.model.ts
import { TrashType } from './bin.model';

export type TourneeStatus = string;  // backend: "PLANNED", etc. Refactor later?
export type StepStatus = string;     // backend: "PENDING", etc. same

export interface RouteStep {
  id: string | null;
  order: number;
  status: StepStatus;
  predictedFillPct: number;
  notes: string | null;
  collectionPointId: string;
}

export interface Tournee {
  id: string;
  tourneeType: TrashType;
  status: TourneeStatus;
  plannedKm: number;
  plannedCO2: number;
  startedAt: string | null;   // ISO dates in JSON
  finishedAt: string | null;
  steps: RouteStep[];
  geometry?: string;
}
