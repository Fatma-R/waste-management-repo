import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  constructor(public authService: AuthService, private router: Router) {}

  logout() {
    //method loggedIn call in a variable:
    const loggedIn = this.authService.loggedIn;
    this.authService.logout();
    this.router.navigate(['/landing']); // redirect after logout
  }
}
