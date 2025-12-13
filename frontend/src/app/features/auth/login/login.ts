import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class Login {
  email: string = '';
  password: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        console.log('Login successful:', response);
        
        // Route based on role
        if (this.authService.isAdmin()) {
          window.location.href = '/admin/dashboard';
        } else {
          window.location.href = '/user/dashboard';
        }

      },
      error: (err) => {
        console.error('Login failed:', err);
        const errorMsg = err.error?.error || 'Invalid email or password';
        alert(errorMsg);
      }
    });
  }
}