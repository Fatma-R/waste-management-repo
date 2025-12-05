import { Routes } from '@angular/router';
import { HelloPage } from './features/hello/hello-page/hello-page';
import { Login } from './features/auth/login/login';
import { Signup } from './features/auth/signup/signup';
import { AuthGuard } from './core/auth/auth-guard';
import { LoginGuard } from './core/auth/login-guard';
import { RoleGuard } from './core/auth/role-guard';
import { AuthLayout } from './core/layouts/auth-layout/auth-layout';
import { MainLayout } from './core/layouts/main-layout/main-layout';
import { Landing } from './features/landing/landing';
import { DashboardComponent } from './features/dashboard/dashboard';
import { AdminDashboardComponent } from './features/admin-dashboard/admin-dashboard';
import { EmployeesComponent } from './features/employees/employees';
import { AdminsComponent } from './features/admins/admins';

// --- From your feature/frontend branch ---
import { VehiclesComponent } from './features/vehicle/vehicle';
import { IncidentsComponent } from './features/incident/incident';
import { AlertsComponent } from './features/alert/alert';
// --- From main branch ---
import { BinComponent } from './features/bin/bin';
import { CollectionPointComponent } from './features/collection-point/collection-point';
import { BinReadingComponent } from './features/bin-reading/bin-reading';
import { TourneeMapComponent } from './features/tournee-map/tournee-map';
import { EmployeeAlertsComponent } from './features/employee-alerts/employee-alerts';


export const routes: Routes = [

  { path: '', pathMatch: 'full', redirectTo: 'redirector' },

  {
    path: 'redirector',
    canActivate: [AuthGuard],
    children: [],
  },

  // AUTH (no layout)
  {
    path: '',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login, canActivate: [LoginGuard] },
      { path: 'signup', component: Signup, canActivate: [LoginGuard] },
    ]
  },

  // MAIN LAYOUT
  {
    path: '',
    component: MainLayout,
    children: [
      { path: 'landing', component: Landing },
      { path: 'home', component: HelloPage, canActivate: [AuthGuard] },

      {
        path: 'admin',
        canActivate: [RoleGuard],
        data: { roles: ['ROLE_ADMIN'] },
        children: [
          { path: 'dashboard', component: AdminDashboardComponent },
          { path: 'employees', component: EmployeesComponent },
          { path: 'admins', component: AdminsComponent },

          // ---- YOUR NEW FRONTEND MODULES ----
          { path: 'incidents', component: IncidentsComponent },
          { path: 'vehicles', component: VehiclesComponent },
          { path: 'alerts', component: AlertsComponent },
          { path: 'tournee', component: AlertsComponent },

          // ---- Existing MAIN branch routes ----
          { path: 'bins', component: BinComponent },
          { path: 'collection-points', component: CollectionPointComponent },
          { path: 'collection-points/:cpId/bins', component: BinComponent },
          { path: 'collection-points/:cpId/bins/:binId/readings', component: BinReadingComponent },
          { path: 'tournee-map', component: TourneeMapComponent }
        ]
      },

     {
  path: 'user',
  canActivate: [RoleGuard],
  data: { roles: ['ROLE_USER', 'ROLE_ADMIN'] },
  children: [
    { path: 'dashboard', component: DashboardComponent },
    { path: 'home', component: HelloPage },
    { path: 'alerts', component: AlertsComponent },
    { path: 'employee-alerts', component: EmployeeAlertsComponent },
    { path: 'tournee-map', component: TourneeMapComponent }  // si nécessaire
  ]
}
 main
        ]
      },

      { path: '**', redirectTo: 'landing' }
    ]
  }
];
