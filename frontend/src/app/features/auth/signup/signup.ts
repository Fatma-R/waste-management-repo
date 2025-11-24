import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './signup.html',
  styleUrls: ['../login/login.scss']
})
export class Signup {
  fullName: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.password !== this.confirmPassword) {
      alert('Passwords do not match.');
      return;
    }

    this.authService.signup({
      email: this.email,
      //password: this.password,
      fullName: this.fullName
    }).subscribe({
      next: (res: any) => {
        const message = res.message || 'Signup successful!';
        alert(message);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Signup error:', err);
        const message = err.error?.error || 'Signup failed. Please try again.';
        alert(message);
      }
    });
  }
}
