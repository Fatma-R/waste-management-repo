import { Routes } from '@angular/router';
import { HelloPage } from './features/hello/hello-page/hello-page';
import { Login } from './features/auth/login/login';
import { Signup } from './features/auth/signup/signup';
import { AuthGuard } from './core/auth/auth-guard';
import { LoginGuard } from './core/auth/login-guard';
import { RoleGuard } from './core/auth/role-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },

  // Public routes (login/signup)
  { path: 'login', component: Login, canActivate: [LoginGuard] },
  { path: 'signup', component: Signup, canActivate: [LoginGuard] },

  // Protected routes (requires authentication)
  { path: 'home', component: HelloPage, canActivate: [AuthGuard] },

  // Admin-only routes
  {
    path: 'admin',
    canActivate: [RoleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    children: [
      // Add your admin routes here
      // { path: 'dashboard', component: AdminDashboardComponent },
      // { path: 'users', component: AdminUsersComponent },
    ]
  },

  // User routes (accessible by both USER and ADMIN)
  {
    path: 'user',
    canActivate: [RoleGuard],
    data: { roles: ['ROLE_USER', 'ROLE_ADMIN'] },
    children: [
      // Add your user routes here
      // { path: 'profile', component: UserProfileComponent },
    ]
  },

  { path: '**', redirectTo: 'home' }
];