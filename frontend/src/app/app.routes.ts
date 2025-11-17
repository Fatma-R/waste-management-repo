import { Routes } from '@angular/router';
import { HelloPage } from './features/hello/hello-page/hello-page';
import { Login } from './features/auth/login/login';
import { Signup } from './features/auth/signup/signup';
import { AuthGuard } from './core/auth/auth-guard';
import { LoginGuard } from './core/auth/login-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'hello', pathMatch: 'full' },

  { path: 'hello', component: HelloPage, canActivate: [AuthGuard] }, // protected

  { path: 'login', component: Login, canActivate: [LoginGuard] },      // for non-logged in users
  { path: 'signup', component: Signup, canActivate: [LoginGuard] },    // for non-logged in users

  { path: '**', redirectTo: 'hello' } // fallback
];
