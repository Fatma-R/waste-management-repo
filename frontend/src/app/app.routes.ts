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

export const routes: Routes = [

  // AUTH ROUTES (no header/footer)

  {
    path: '',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login, canActivate: [LoginGuard] },
      { path: 'signup', component: Signup, canActivate: [LoginGuard] },
    ]
  },

  // MAIN ROUTES (with layout)

  {
    path: '',
    component: MainLayout,
    children: [

      // Default landing page
      { path: '', redirectTo: 'landing', pathMatch: 'full' },

      // Public Landing page
      { path: 'landing', component: Landing },

      // Protected Home
      { path: 'home', component: HelloPage, canActivate: [AuthGuard] },

      // ADMIN ROUTES
      {
        path: 'admin',
        canActivate: [RoleGuard],
        data: { roles: ['ROLE_ADMIN'] },
        children: [
          // Add your admin routes here…
          // { path: 'dashboard', component: AdminDashboardComponent }
        ]
      },

      // USER ROUTES (USER + ADMIN)
      {
        path: 'user',
        canActivate: [RoleGuard],
        data: { roles: ['ROLE_USER', 'ROLE_ADMIN'] },
        children: [
          { path: 'home', component: HelloPage },
          // Add more user pages here…
        ]
      },

      // Wildcard (ONLY INSIDE MAIN)
      { path: '**', redirectTo: 'landing' }
    ]
  }
];
