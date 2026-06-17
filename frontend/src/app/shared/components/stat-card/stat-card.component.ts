import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() icon = 'pi pi-chart-bar';
  @Input() loading = false;
  /** Use para valores em texto (ex.: "Operacional"), que não cabem na fonte grande de números. */
  @Input() compact = false;
}
