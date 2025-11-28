export interface BinReading {
  id: string;
  binId: string;
  ts: Date;
  fillPct: number;
  batteryPct: number;
  temperatureC: number;
  signalDbm: number;
}

export interface CreateBinReadingDto {
  binId: string;
  ts: Date;
  fillPct: number;
  batteryPct: number;
  temperatureC: number;
  signalDbm: number;
}

export interface UpdateBinReadingDto {
  binId: string;
  ts: Date;
  fillPct: number;
  batteryPct: number;
  temperatureC: number;
  signalDbm: number;
}
