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
import { VehiclesComponent } from './features/vehicle/vehicle';
import { IncidentsComponent } from './features/incident/incident';
import { AlertsComponent } from './features/alert/alert';

export const routes: Routes = [

  // ====== REDIRECT ROOT ======
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'redirector'
  },

  // ====== EMPTY ROUTE DECIDER ======
  {
    path: 'redirector',
    canActivate: [AuthGuard],
    children: [],
  },

  // AUTH ROUTES (no layout)
  {
    path: '',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login, canActivate: [LoginGuard] },
      { path: 'signup', component: Signup, canActivate: [LoginGuard] },
    ]
  },

  // MAIN ROUTES
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
          { path: 'incidents', component: IncidentsComponent },
          { path: 'vehicles', component: VehiclesComponent },
          { path: 'alerts', component: AlertsComponent } 


        ]
      },

      {
        path: 'user',
        canActivate: [RoleGuard],
        data: { roles: ['ROLE_USER', 'ROLE_ADMIN'] },
        children: [
          { path: 'dashboard', component: DashboardComponent },
          { path: 'home', component: HelloPage }
        ]
      },

      { path: '**', redirectTo: 'landing' }
    ]
  }
];
