import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Hello } from '../../../core/services/hello';


@Component({
  selector: 'app-hello-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 1.5rem">
      <h1>Test /api/hello</h1>

      <p *ngIf="error" style="color: red; margin-top: 1rem;">
        Error: {{ error }}
      </p>

      <p *ngIf="message && !error" style="margin-top: 1rem;">
        Backend says: <strong>{{ message }}</strong>
      </p>
    </div>
  `
})
export class HelloPage implements OnInit {

  message = '';
  error = '';
  loading = false;

  constructor(private hello: Hello) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.hello.getHello().subscribe({
      next: msg => {
        this.message = msg;
        this.loading = false;
      },
      error: err => {
        this.error = err.message ?? 'Unknown error';
        this.loading = false;
      }
    });
  }
}
