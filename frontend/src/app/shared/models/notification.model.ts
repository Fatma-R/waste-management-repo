export interface Notification {
  id: string;
  type: 'alert' | 'info' | 'warning' | 'success';
  message: string;
  binId?: string;
  timestamp: Date;
  read: boolean;
}
