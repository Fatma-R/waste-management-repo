export interface Bin {
  id: string;
  type: 'plastic' | 'organic' | 'glass' | 'paper' | 'general';
  latitude: number;
  longitude: number;
  fillLevel: number;
  status: 'active' | 'inactive' | 'maintenance';
  zone?: string;
  lastUpdated?: Date;
}

export interface CreateBinDto {
  type: Bin['type'];
  latitude: number;
  longitude: number;
  zone?: string;
}

export interface UpdateBinDto extends Partial<CreateBinDto> {
  fillLevel?: number;
  status?: Bin['status'];
}