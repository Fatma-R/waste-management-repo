export enum TrashType {
  PLASTIC = 'PLASTIC',
  ORGANIC = 'ORGANIC',
  PAPER = 'PAPER',
  GLASS = 'GLASS',
}


export interface Bin {
  id: string;
  collectionPointId: string;
  active: boolean;
  type: TrashType;
}

export interface CreateBinDto {
  collectionPointId: string;
  active: boolean;
  type: TrashType;
}

export interface UpdateBinDto {
  collectionPointId: string;
  active: boolean;
  type: TrashType;
}
