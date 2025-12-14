import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiComponent } from '../../shared/components/kpi/kpi';
import { CardComponent } from '../../shared/components/card/card';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { BinService } from '../../core/services/bin';
import { DashboardStats } from '../../shared/models/dashbaord-stats.model';
import { Alert } from '../../shared/models/alert.model';
import { AlertService } from '../../core/services/alert';
import { Router, RouterModule } from '@angular/router';
import { TourneeService } from '../../core/services/tournee';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    KpiComponent,
    CardComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {

formatDate(arg0: string) {
throw new Error('Method not implemented.');
}

  stats: DashboardStats = {
    totalBins: 0,
    binsFull: 0,
    activeRoutes: 12,
    co2Saved: 0
  };
  kpiStats: { alertsCount: number; activeRoutes: number; co2Last7Days: number } = {
    alertsCount: 0,
    activeRoutes: 12,  // fake
    co2Last7Days: 0
  };


  isLoading = true;
  isAddBinModalOpen = false;

  alerts: Alert[] = []; 

  constructor(
    private binService: BinService,
    private alertService: AlertService,
    private router: Router,
    private tourneeService: TourneeService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadAlerts();
  }

  loadDashboardData(): void {
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

    // Active routes (ONLY this employee)
    this.tourneeService.getInProgressTournees().pipe(
      catchError(err => {
        console.error('Error loading active routes KPI:', err);
        return of([]);
      })
    ).subscribe((tournees: any[]) => {
      const employeeId = localStorage.getItem('employeeId'); // adjust if you store it differently

      const mine = (tournees ?? []).filter(t =>
        t.employeeId === employeeId ||
        t.assignedToId === employeeId ||
        (Array.isArray(t.employeeIds) && t.employeeIds.includes(employeeId)) ||
        (Array.isArray(t.assignedEmployeeIds) && t.assignedEmployeeIds.includes(employeeId)) ||
        (Array.isArray(t.assignedEmployees) && t.assignedEmployees.some((e: any) => e?.id === employeeId)) ||
        (t.assignment?.employeeId === employeeId) ||
        (Array.isArray(t.assignment?.employees) && t.assignment.employees.some((e: any) => e?.id === employeeId))
      );

      this.kpiStats.activeRoutes = mine.length;
    });


      this.tourneeService.getCo2Last7Days().pipe(
      catchError(err => {
        console.error('Error loading CO2 KPI:', err);
        return of(null);
      })
    ).subscribe((co2Kg) => {
      if (co2Kg !== null && co2Kg !== undefined) {
        this.kpiStats.co2Last7Days = co2Kg;
      }
    });
  }

  // -------------------------------------------------------
  // ✅ VERSION FINALE : Charger les 3 dernières alertes réelles
  // -------------------------------------------------------
  
  loadAlerts(): void {
    this.isLoading = true;
    this.alertService.getAlerts().subscribe({
      next: (alerts: Alert[]) => {
        this.alerts = alerts.slice(0, 3).map(alert => ({
          ...alert,
          resolved: alert.resolved ?? alert.cleared,
         
          message: alert.message || alert.type,
        
          createdAt: alert.createdAt || alert.ts
        }));
        this.kpiStats.alertsCount = alerts.length; 
        this.isLoading = false;

      },
      error: (err) => {
        console.error('Error loading alerts:', err);
        this.isLoading = false;
      }
    });
  }
   

  formatAlertDate(ts?: string): string {
    if (!ts) return 'N/A';
    return new Date(ts).toLocaleString();
  }

  

  openAddBinModal(): void {
    this.isAddBinModalOpen = true;
  }

  closeAddBinModal(): void {
    this.isAddBinModalOpen = false;
  }

  

  goToAllAlerts() {
    this.router.navigate(['/user/employee-alerts']);
  }
  goToBinsDashboard(): void {
    this.router.navigate(['/user/bins']); 
  }
  goToDashboard(): void {
    this.router.navigate(['/user/dashboard']);
  }


}
