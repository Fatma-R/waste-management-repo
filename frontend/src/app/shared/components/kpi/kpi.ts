import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi.html',
  styleUrls: ['./kpi.scss']
})
export class KpiComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() unit = '';
  @Input() icon = '';
  @Input() trend?: 'up' | 'down';
  @Input() color: 'green' | 'sage' | 'amber' | 'red' = 'sage';
}
