import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { AcAction, AcSchedule, DayOfWeek } from '../../models/ac-schedule.model';
import { AcSchedulesService } from '../../services/ac-schedules.service';

const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Segunda-feira',
  TUESDAY: 'Terça-feira',
  WEDNESDAY: 'Quarta-feira',
  THURSDAY: 'Quinta-feira',
  FRIDAY: 'Sexta-feira',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

@Component({
  selector: 'app-ac-schedules-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ac-schedules.page.html',
})
export class AcSchedulesPageComponent implements OnInit {
  private readonly service = inject(AcSchedulesService);
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly items = signal<AcSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly days: DayOfWeek[] = [
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
  ];

  protected readonly form = this.fb.group({
    dayOfWeek: ['MONDAY' as DayOfWeek, Validators.required],
    time: ['07:00', Validators.required],
    action: ['ON' as AcAction, Validators.required],
    enabled: [true],
  });

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.service
      .findAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (list) => this.items.set(list),
        error: () => this.notify.error('Falha ao carregar agendamentos.'),
      });
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.service
      .create({
        dayOfWeek: raw.dayOfWeek as DayOfWeek,
        time: raw.time + ':00',
        action: raw.action as AcAction,
        enabled: raw.enabled ?? true,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.notify.success('Agendamento criado.');
          this.form.reset({ dayOfWeek: 'MONDAY', time: '07:00', action: 'ON', enabled: true });
          this.reload();
        },
        error: () => this.notify.error('Falha ao criar agendamento.'),
      });
  }

  protected toggleEnabled(item: AcSchedule): void {
    this.service.update(item.id, { ...item, enabled: !item.enabled }).subscribe({
      next: () => this.reload(),
      error: () => this.notify.error('Falha ao atualizar agendamento.'),
    });
  }

  protected remove(item: AcSchedule): void {
    if (!confirm(`Excluir agendamento ${this.dayLabel(item.dayOfWeek)} às ${this.formatTime(item.time)}?`)) return;
    this.service.delete(item.id).subscribe({
      next: () => {
        this.notify.success('Agendamento removido.');
        this.reload();
      },
      error: () => this.notify.error('Falha ao remover agendamento.'),
    });
  }

  protected dayLabel(day: DayOfWeek): string {
    return DAY_LABELS[day];
  }

  protected formatTime(value: string): string {
    return value ? value.slice(0, 5) : '--';
  }
}
