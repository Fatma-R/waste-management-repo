import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-fill-indicator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fill-indicator.html',
  styleUrls: ['./fill-indicator.scss']
})
export class FillIndicatorComponent {
  @Input() fillLevel = 0;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showLabel = true;

  get fillColor(): string {
    if (this.fillLevel >= 80) return 'red';
    if (this.fillLevel >= 50) return 'amber';
    return 'green';
  }

  get fillLabel(): string {
    if (this.fillLevel >= 80) return 'Full';
    if (this.fillLevel >= 50) return 'Half Full';
    return 'Empty';
  }
}
