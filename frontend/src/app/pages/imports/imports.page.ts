import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { ScheduleImport } from '../../models/schedule-import.model';
import { ImportsService } from '../../services/imports.service';

@Component({
  selector: 'app-imports-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './imports.page.html',
})
export class ImportsPageComponent implements OnInit {
  private readonly importsService = inject(ImportsService);
  private readonly notify = inject(NotificationService);

  protected readonly items = signal<ScheduleImport[]>([]);
  protected readonly loading = signal(false);
  protected readonly uploading = signal(false);
  protected readonly scanning = signal(false);
  protected readonly page = signal(0);
  protected readonly pageSize = 20;
  protected readonly totalPages = signal(0);
  protected selectedFile: File | null = null;

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.importsService
      .list(this.page(), this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (p) => {
          this.items.set(p.content);
          this.totalPages.set(p.totalPages);
        },
        error: () => this.notify.error('Falha ao carregar importações.'),
      });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  protected upload(): void {
    if (!this.selectedFile) {
      this.notify.info('Selecione um arquivo JSON primeiro.');
      return;
    }
    this.uploading.set(true);
    this.importsService
      .upload(this.selectedFile)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (imp) => {
          this.notify.success(`Importação registrada: ${imp.status}`);
          this.selectedFile = null;
          (document.getElementById('file-input') as HTMLInputElement).value = '';
          this.reload();
        },
        error: () => this.notify.error('Falha no upload.'),
      });
  }

  protected triggerScan(): void {
    this.scanning.set(true);
    this.importsService
      .triggerFolderScan()
      .pipe(finalize(() => this.scanning.set(false)))
      .subscribe({
        next: (r) => {
          this.notify.success(`Pasta varrida — ${r.processed} arquivo(s) processado(s).`);
          this.reload();
        },
        error: () => this.notify.error('Falha ao acionar varredura.'),
      });
  }

  protected prev(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.reload();
    }
  }

  protected next(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.set(this.page() + 1);
      this.reload();
    }
  }

  protected statusClass(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'status-chip status-chip--success';
      case 'SKIPPED_DUPLICATE':
        return 'status-chip status-chip--neutral';
      case 'PARSE_ERROR':
      case 'VALIDATION_ERROR':
      case 'PERSISTENCE_ERROR':
        return 'status-chip status-chip--danger';
      default:
        return 'status-chip status-chip--warning';
    }
  }

  protected formatDate(value: string | null): string {
    if (!value) return '--';
    const d = new Date(value);
    return Number.isNaN(d.getTime())
      ? value
      : new Intl.DateTimeFormat('pt-BR', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        }).format(d);
  }
}
