import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { Holiday, HolidayScope } from '../../models/holiday.model';
import { HolidaysService } from '../../services/holidays.service';

@Component({
  selector: 'app-holidays-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './holidays.page.html',
})
export class HolidaysPageComponent implements OnInit {
  private readonly holidaysService = inject(HolidaysService);
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly items = signal<Holiday[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly syncing = signal(false);
  protected readonly year = signal<number>(new Date().getFullYear());

  protected readonly form = this.fb.group({
    date: ['', Validators.required],
    name: ['', Validators.required],
    scope: ['STATE' as HolidayScope, Validators.required],
    type: [''],
  });

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.holidaysService
      .list(this.year())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (list) => this.items.set(list),
        error: () => this.notify.error('Falha ao carregar feriados.'),
      });
  }

  protected changeYear(event: Event): void {
    const v = Number((event.target as HTMLInputElement).value);
    if (!Number.isNaN(v) && v > 1900) {
      this.year.set(v);
      this.reload();
    }
  }

  protected sync(): void {
    this.syncing.set(true);
    this.holidaysService
      .sync(this.year())
      .pipe(finalize(() => this.syncing.set(false)))
      .subscribe({
        next: (r) => {
          this.notify.success(`Sincronizados — ${r.upserts} feriado(s) atualizado(s) em ${r.year}.`);
          this.reload();
        },
        error: () => this.notify.error('Falha ao sincronizar com BrasilAPI.'),
      });
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.holidaysService
      .create({
        date: raw.date ?? '',
        name: raw.name ?? '',
        scope: (raw.scope ?? 'STATE') as HolidayScope,
        type: raw.type || null,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.notify.success('Feriado cadastrado.');
          this.form.reset({ date: '', name: '', scope: 'STATE', type: '' });
          this.reload();
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Falha ao cadastrar feriado.';
          this.notify.error(msg);
        },
      });
  }

  protected remove(h: Holiday): void {
    if (!confirm(`Excluir o feriado "${h.name}" em ${h.date}?`)) return;
    this.holidaysService.delete(h.id).subscribe({
      next: () => {
        this.notify.success('Feriado removido.');
        this.reload();
      },
      error: () => this.notify.error('Não foi possível remover (talvez seja nacional do BrasilAPI).'),
    });
  }

  protected scopeLabel(scope: HolidayScope): string {
    return { NATIONAL: 'Nacional', STATE: 'Estadual (CE)', MUNICIPAL: 'Municipal (Maracanaú)' }[scope];
  }

  protected formatDate(value: string): string {
    const d = new Date(value + 'T00:00:00');
    return Number.isNaN(d.getTime())
      ? value
      : new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(d);
  }
}
