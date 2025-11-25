export type TrashType = 'PLASTIC' | 'ORGANIC' | 'PAPER' | 'GLASS' | 'METAL';
// adapt this to your actual enum from backend

export interface Bin {
  id: string;
  collectionPointId: string;
  active: boolean;
  type: TrashType;
  readingIds: string[];
  alertIds: string[];
}

export interface CreateBinDto {
  collectionPointId: string;
  active: boolean;
  type: TrashType;
  readingIds: string[];
  alertIds: string[];
}

export interface UpdateBinDto {
  collectionPointId: string;
  active: boolean;
  type: TrashType;
  readingIds: string[];
  alertIds: string[];
}
