import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  // src/app/core/auth/auth-guard.ts
canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
  const url = state.url.split('?')[0];
  const isRoot = url === '/' || url === '' || url === '/redirector';

  if (!this.authService.loggedIn) {
    return isRoot ? this.router.parseUrl('/landing') : this.router.parseUrl('/login');
  }

  if (isRoot) {
    return this.router.parseUrl(this.authService.getHomeRoute());
  }

  return true;
}

}
