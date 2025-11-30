// src/app/shared/models/bin.model.ts

export enum TrashType {
  PLASTIC = 'PLASTIC',
  ORGANIC = 'ORGANIC',
  PAPER = 'PAPER',
  GLASS = 'GLASS',
}

// Modèle principal utilisé dans le frontend
export interface Bin {
  id: string;
  collectionPointId?: string; // pour backend
  type: TrashType;
  latitude?: number;          // pour affichage frontend
  longitude?: number;         // pour affichage frontend
  fillLevel?: number;         // pour affichage frontend
  status?: 'active' | 'inactive' | 'maintenance';
  zone?: string;
  lastUpdated?: Date;
  active?: boolean;           // backend
}

// DTO pour création
export interface CreateBinDto {
  collectionPointId?: string; // backend
  type: TrashType;
  latitude?: number;
  longitude?: number;
  zone?: string;
  active?: boolean;
}

// DTO pour mise à jour
export interface UpdateBinDto extends Partial<CreateBinDto> {
  fillLevel?: number;
  status?: Bin['status'];
}
