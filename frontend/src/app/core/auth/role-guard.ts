import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRoles = route.data['roles'] as string[];

    if (!this.authService.loggedIn) {
      this.router.navigate(['/login']);
      return false;
    }

    const userRoles = this.authService.getUserRoles();
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));

    if (!hasRequiredRole) {
      alert('Access denied: You do not have permission to access this page');
      this.router.navigate(['/home']);
      return false;
    }

    return true;
  }
}