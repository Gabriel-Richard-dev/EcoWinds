import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { finalize, forkJoin } from 'rxjs';
import { AuditLog } from '../../models/audit-log.model';
import { ClassSchedule } from '../../models/class-schedule.model';
import { EspDevice } from '../../models/esp-device.model';
import { Holiday } from '../../models/holiday.model';
import { ScheduleImport } from '../../models/schedule-import.model';
import { AuditLogsService } from '../../services/audit-logs.service';
import { ClassSchedulesService } from '../../services/class-schedules.service';
import { EspDevicesService } from '../../services/esp-devices.service';
import { HolidaysService } from '../../services/holidays.service';
import { ImportsService } from '../../services/imports.service';
import { RoomsService } from '../../services/rooms.service';
import {
  DataTableComponent,
  TableColumn,
} from '../../shared/components/data-table/data-table.component';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { CommonModule } from '@angular/common';

interface DashboardAlert {
  level: 'info' | 'warn' | 'error';
  message: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, DataTableComponent, StatCardComponent],
  templateUrl: './dashboard.page.html',
})
export class DashboardPageComponent implements OnInit {
  private readonly roomsService = inject(RoomsService);
  private readonly devicesService = inject(EspDevicesService);
  private readonly schedulesService = inject(ClassSchedulesService);
  private readonly auditLogsService = inject(AuditLogsService);
  private readonly holidaysService = inject(HolidaysService);
  private readonly importsService = inject(ImportsService);

  protected loadingStats = false;
  protected loadingLogs = false;
  protected loadingExtras = false;
  protected statsError = '';
  protected logsError = '';

  protected readonly stats = signal({
    rooms: 0,
    devicesTotal: 0,
    devicesOnline: 0,
    schedules: 0,
  });
  protected readonly holidayToday = signal<Holiday | null>(null);
  protected readonly latestImport = signal<ScheduleImport | null>(null);
  protected readonly offlineDevices = signal<EspDevice[]>([]);
  protected readonly upcomingToday = signal<ClassSchedule[]>([]);
  protected readonly loadingUpcoming = signal(false);
  protected logs: AuditLog[] = [];

  protected readonly alerts = computed<DashboardAlert[]>(() => {
    const out: DashboardAlert[] = [];
    const s = this.stats();
    const offline = s.devicesTotal - s.devicesOnline;

    if (this.holidayToday()) {
      out.push({
        level: 'info',
        icon: 'pi pi-flag-fill',
        message: `Hoje é feriado (${this.holidayToday()!.name}). Acionamentos automáticos estão bloqueados.`,
      });
    }
    if (s.devicesTotal > 0 && offline > 0) {
      out.push({
        level: 'warn',
        icon: 'pi pi-wifi',
        message: `${offline} de ${s.devicesTotal} microcontrolador(es) sem conexão.`,
      });
    }
    const imp = this.latestImport();
    if (imp && imp.status !== 'SUCCESS' && imp.status !== 'SKIPPED_DUPLICATE') {
      out.push({
        level: 'error',
        icon: 'pi pi-exclamation-triangle',
        message: `Última importação SIGEHO falhou (${imp.status}) — ${imp.filename}.`,
      });
    }
    if (s.devicesTotal === 0) {
      out.push({
        level: 'warn',
        icon: 'pi pi-microchip',
        message: 'Nenhum microcontrolador cadastrado ainda.',
      });
    }
    return out;
  });

  protected readonly logColumns: readonly TableColumn[] = [
    { key: 'timestamp', label: 'Data do envio', kind: 'datetime' },
    { key: 'action', label: 'Evento', kind: 'badge' },
    { key: 'origin', label: 'Origem' },
    { key: 'userId', label: 'Usuário', fallback: '--' },
    { key: 'roomId', label: 'Sala', fallback: '--' },
    { key: 'espDeviceId', label: 'Microcontrolador', fallback: '--' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.loadLogs();
    this.loadExtras();
    this.loadUpcoming();
  }

  private loadUpcoming(): void {
    this.loadingUpcoming.set(true);
    this.schedulesService
      .today()
      .pipe(finalize(() => this.loadingUpcoming.set(false)))
      .subscribe({
        next: (list) => {
          const now = new Date();
          const nowMinutes = now.getHours() * 60 + now.getMinutes();
          const upcoming = list
            .filter((s) => this.toMinutes(s.startTime) > nowMinutes)
            .sort((a, b) => this.toMinutes(a.startTime) - this.toMinutes(b.startTime))
            .slice(0, 8);
          this.upcomingToday.set(upcoming);
        },
        error: () => {
          // silent
        },
      });
  }

  private toMinutes(time: string): number {
    const [h, m] = time.split(':').map(Number);
    return (h ?? 0) * 60 + (m ?? 0);
  }

  protected reloadLogs(): void {
    this.loadLogs();
  }

  protected importStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'status-chip status-chip--success';
      case 'SKIPPED_DUPLICATE':
        return 'status-chip status-chip--neutral';
      default:
        return 'status-chip status-chip--danger';
    }
  }

  protected formatTimeHm(value: string): string {
    return value ? value.slice(0, 5) : '--';
  }

  protected relativeTime(value: string | null): string {
    if (!value) return 'nunca';
    const diff = Date.now() - new Date(value).getTime();
    if (Number.isNaN(diff)) return value;
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'agora há pouco';
    if (minutes < 60) return `há ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `há ${hours}h`;
    const days = Math.floor(hours / 24);
    return `há ${days}d`;
  }

  protected formatDateTime(value: string | null): string {
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

  private loadStats(): void {
    this.loadingStats = true;
    this.statsError = '';

    forkJoin({
      rooms: this.roomsService.search('', 0, 1),
      devices: this.devicesService.search('', 0, 100),
      schedules: this.schedulesService.search('', 0, 1),
    })
      .pipe(finalize(() => (this.loadingStats = false)))
      .subscribe({
        next: ({ rooms, devices, schedules }) => {
          const online = devices.content.filter((d) => d.connectionStatus).length;
          const offline = devices.content
            .filter((d) => !d.connectionStatus)
            .sort((a, b) => {
              const ta = a.lastHeartbeatAt ? new Date(a.lastHeartbeatAt).getTime() : 0;
              const tb = b.lastHeartbeatAt ? new Date(b.lastHeartbeatAt).getTime() : 0;
              return tb - ta;
            });
          this.offlineDevices.set(offline);
          this.stats.set({
            rooms: rooms.totalElements,
            devicesTotal: devices.totalElements,
            devicesOnline: online,
            schedules: schedules.totalElements,
          });
        },
        error: () => {
          this.statsError = 'Não foi possível carregar os indicadores do dashboard.';
        },
      });
  }

  private loadLogs(): void {
    this.loadingLogs = true;
    this.logsError = '';

    this.auditLogsService
      .search('', 0, 10)
      .pipe(finalize(() => (this.loadingLogs = false)))
      .subscribe({
        next: (page) => {
          this.logs = page.content;
        },
        error: () => {
          this.logsError = 'Não foi possível carregar os logs recentes.';
        },
      });
  }

  private loadExtras(): void {
    this.loadingExtras = true;
    forkJoin({
      holiday: this.holidaysService.today(),
      imports: this.importsService.list(0, 1),
    })
      .pipe(finalize(() => (this.loadingExtras = false)))
      .subscribe({
        next: ({ holiday, imports }) => {
          this.holidayToday.set(
            holiday.isHoliday && holiday.holidays.length > 0 ? holiday.holidays[0] : null,
          );
          this.latestImport.set(imports.content[0] ?? null);
        },
        error: () => {
          // silent: extras são opcionais
        },
      });
  }
}
