import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { JwtResponse, SignupRequest } from './user';
import { environment } from '../../environment/environment.development';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl + '/auth';
  private _loggedIn = new BehaviorSubject<boolean>(!!this.getToken());
  public loggedIn$ = this._loggedIn.asObservable();

  constructor(private http: HttpClient) { }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  login(email: string, password: string): Observable<JwtResponse> {
  return this.http.post<JwtResponse>(`${this.apiUrl}/signin`, { email, password })
    .pipe(
      tap(response => {
        localStorage.setItem('access_token', response.token);
        localStorage.setItem('user_email', response.email);
        localStorage.setItem('user_roles', JSON.stringify(response.roles));
        this._loggedIn.next(true);
      })
    );
  }


  signup(signupData: SignupRequest): Observable<any> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/signup`, signupData);
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_roles');
    this._loggedIn.next(false);
  }

  get loggedIn(): boolean {
    return this._loggedIn.value;
  }

  getUserEmail(): string | null {
    return localStorage.getItem('user_email');
  }

  getUserRoles(): string[] {
    const roles = localStorage.getItem('user_roles');
    return roles ? JSON.parse(roles) : [];
  }

  hasRole(role: string): boolean {
    const roles = this.getUserRoles();
    return roles.includes(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  isUser(): boolean {
    return this.hasRole('ROLE_USER');
  }

  get currentUser(): any | null {
    const token = this.getToken();
    if (!token) return null;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        email: payload.sub,
        roles: this.getUserRoles()
      };
    } catch {
      return null;
    }
  }
    getHomeRoute(): string {
    if (!this.loggedIn) return '/landing';

    if (this.isAdmin()) return '/admin/dashboard';
    if (this.isUser()) return '/user/dashboard';

    return '/landing';
  }

}