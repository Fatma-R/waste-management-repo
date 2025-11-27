import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { KpiComponent } from '../../shared/components/kpi/kpi';
import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { ModalComponent } from '../../shared/components/modal/modal';
import { BinService } from '../../core/services/bin';
import { NotificationService } from '../../core/services/notification';
import { DashboardStats } from '../../shared/models/dashbaord-stats.model';
import { Notification } from '../../shared/models/notification.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    KpiComponent,
    CardComponent,
    ButtonComponent,
    LoadingSpinnerComponent,
    ModalComponent
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats = {
    totalBins: 0,
    binsFull: 0,
    activeRoutes: 0,
    co2Saved: 0
  };
  
  notifications$: Observable<Notification[]>;
  isLoading = true;
  isAddBinModalOpen = false;

  constructor(
    private binService: BinService,
    private notificationService: NotificationService
  ) {
    this.notifications$ = this.notificationService.notifications$;
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    // Using mock data for development
    this.binService.getBins().subscribe({
      next: (bins) => {
        this.stats.totalBins = bins.length;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard data:', err);
        this.isLoading = false;
      }
    });
  }

  openAddBinModal(): void {
    this.isAddBinModalOpen = true;
  }

  closeAddBinModal(): void {
    this.isAddBinModalOpen = false;
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'alert': return '🚨';
      case 'warning': return '⚠️';
      case 'success': return '✅';
      default: return 'ℹ️';
    }
  }

  formatTimestamp(timestamp: Date): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return date.toLocaleDateString();
  }
}
