import { Injectable } from '@angular/core';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { ApiService } from './api';
import { Notification } from '../../shared/models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  constructor(private api: ApiService) {
    this.loadNotifications();
  }

  getNotifications(): Observable<Notification[]> {
    return this.api.get<Notification[]>('/notifications');
  }

  markAsRead(id: string): Observable<void> {
    return this.api.put<void>(`/notifications/${id}/read`, {});
  }

  private loadNotifications(): void {
    // Mock notifications for development
    const mockNotifications: Notification[] = [
      { id: '1', type: 'alert', message: 'Bin #103 is full (92%)', binId: '3', timestamp: new Date(), read: false },
      { id: '2', type: 'warning', message: 'Bin #101 needs attention (85%)', binId: '1', timestamp: new Date(Date.now() - 3600000), read: false },
      { id: '3', type: 'info', message: 'Route optimization completed', timestamp: new Date(Date.now() - 7200000), read: true }
    ];
    this.notificationsSubject.next(mockNotifications);
  }

  showToast(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    // This will be implemented with a toast component
    console.log(`[Toast ${type}]:`, message);
  }
}
