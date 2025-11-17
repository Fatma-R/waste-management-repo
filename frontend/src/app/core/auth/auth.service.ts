import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { User } from './user';
import { environment } from '../../environment/environment.development';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl + '/auth';

  constructor(private http: HttpClient) { }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post<{ token: string }>(`${this.apiUrl}/signin`, { email, password })
      .pipe(
        tap(response => {
          localStorage.setItem('access_token', response.token);
        })
      );
  }

  signup(email: string, password: string): Observable<any> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/signup`, { email, password });
  }

  logout() {
    localStorage.removeItem('access_token');
  }

  get loggedIn(): boolean {
    return !!localStorage.getItem('access_token');
  }

  get currentUser(): any | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.user ?? payload.sub;
    } catch {
      return null;
    }
  }
}
