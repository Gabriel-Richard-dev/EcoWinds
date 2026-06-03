import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { EspDevice } from '../../models/esp-device.model';
import { EspDevicesService } from '../../services/esp-devices.service';
import { RoomsService } from '../../services/rooms.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  DataTableComponent,
  TableAction,
  TableColumn,
} from '../../shared/components/data-table/data-table.component';
import {
  EntityFormDialogComponent,
  EntityFormField,
  EntityFormOption,
} from '../../shared/components/entity-form-dialog/entity-form-dialog.component';

@Component({
  selector: 'app-devices-page',
  standalone: true,
  imports: [ConfirmDialogComponent, DataTableComponent, EntityFormDialogComponent],
  templateUrl: './devices.page.html',
})
export class DevicesPageComponent implements OnInit {
  private readonly devicesService = inject(EspDevicesService);
  private readonly roomsService = inject(RoomsService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly notificationService = inject(NotificationService);

  protected devices: EspDevice[] = [];
  protected roomOptions: EntityFormOption[] = [];
  protected loading = false;
  protected saving = false;
  protected deleting = false;
  protected error = '';
  protected dialogError = '';
  protected search = '';
  protected page = 0;
  protected pageSize = 10;
  protected totalPages = 0;
  protected totalElements = 0;
  protected dialogOpen = false;
  protected dialogReadonly = false;
  protected selectedDevice: EspDevice | null = null;
  protected deviceToDelete: EspDevice | null = null;
  protected readonly actions: readonly TableAction[] = ['view', 'edit', 'apikey', 'delete'];
  protected apiKeyResult: { deviceId: string; apiKey: string } | null = null;
  protected rotatingKey = false;
  protected readonly columns: readonly TableColumn[] = [
    { key: 'ipAddress', label: 'IP' },
    { key: 'macAddress', label: 'MAC Address' },
    { key: 'connectionStatus', label: 'Conexão', kind: 'boolean' },
    { key: 'infraredFrequency', label: 'Frequência IR' },
    { key: 'roomIdentification', label: 'Sala', fallback: '--' },
  ];
  protected readonly form = this.formBuilder.group({
    macAddress: ['', Validators.required],
    ipAddress: ['', Validators.required],
    connectionStatus: [false, Validators.required],
    infraredFrequency: ['', Validators.required],
    roomId: [null as number | null],
  });

  protected get fields(): readonly EntityFormField[] {
    return [
      {
        key: 'macAddress',
        label: 'MAC Address',
        type: 'text',
        placeholder: 'A0:B1:C2:D3:E4:F5',
        required: true,
      },
      { key: 'ipAddress', label: 'Endereço IP', type: 'text', placeholder: '192.168.0.10', required: true },
      {
        key: 'infraredFrequency',
        label: 'Frequência infravermelha',
        type: 'text',
        placeholder: '38kHz',
        required: true,
      },
      {
        key: 'roomId',
        label: 'Sala vinculada',
        type: 'select',
        options: this.roomOptions,
        hint: 'A API permite um dispositivo por sala.',
      },
      { key: 'connectionStatus', label: 'Dispositivo conectado', type: 'checkbox', full: true },
    ];
  }

  ngOnInit(): void {
    this.loadDevices();
    this.loadRoomOptions();
  }

  protected onSearch(term: string): void {
    this.search = term;
    this.page = 0;
    this.loadDevices();
  }

  protected onPageChange(page: number): void {
    this.page = page;
    this.loadDevices();
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.page = 0;
    this.loadDevices();
  }

  protected openCreate(): void {
    this.selectedDevice = null;
    this.dialogReadonly = false;
    this.dialogError = '';
    this.form.enable();
    this.form.reset({
      macAddress: '',
      ipAddress: '',
      connectionStatus: false,
      infraredFrequency: '',
      roomId: null,
    });
    this.dialogOpen = true;
  }

  protected openView(row: unknown): void {
    this.populateDialog(row as EspDevice, true);
  }

  protected openEdit(row: unknown): void {
    this.populateDialog(row as EspDevice, false);
  }

  protected closeDialog(): void {
    this.dialogOpen = false;
    this.dialogReadonly = false;
    this.dialogError = '';
    this.form.enable();
  }

  protected saveDevice(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: Partial<EspDevice> = {
      macAddress: raw.macAddress ?? '',
      ipAddress: raw.ipAddress ?? '',
      connectionStatus: Boolean(raw.connectionStatus),
      infraredFrequency: raw.infraredFrequency ?? '',
      roomId: this.toNumberOrNull(raw.roomId),
    };
    const request = this.selectedDevice
      ? this.devicesService.update(this.selectedDevice.id, payload)
      : this.devicesService.create(payload);

    this.saving = true;
    this.dialogError = '';

    request.pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => {
        this.notificationService.success(
          this.selectedDevice
            ? 'Microcontrolador atualizado com sucesso.'
            : 'Microcontrolador cadastrado com sucesso.',
        );
        this.closeDialog();
        this.loadDevices();
      },
      error: () => {
        this.dialogError = 'Não foi possível salvar o microcontrolador.';
      },
    });
  }

  protected confirmDelete(row: unknown): void {
    this.deviceToDelete = row as EspDevice;
  }

  protected rotateApiKey(row: unknown): void {
    const device = row as EspDevice;
    if (!confirm(`Gerar nova API key para o dispositivo ${device.ipAddress}? A chave anterior deixará de funcionar.`)) {
      return;
    }
    this.rotatingKey = true;
    this.devicesService
      .rotateApiKey(device.id)
      .pipe(finalize(() => (this.rotatingKey = false)))
      .subscribe({
        next: (res) => {
          this.apiKeyResult = { deviceId: res.deviceId, apiKey: res.apiKey };
        },
        error: () => this.notificationService.error('Falha ao gerar a API key.'),
      });
  }

  protected dismissApiKey(): void {
    this.apiKeyResult = null;
  }

  protected copyApiKey(): void {
    if (!this.apiKeyResult) return;
    navigator.clipboard
      ?.writeText(this.apiKeyResult.apiKey)
      .then(() => this.notificationService.success('Chave copiada para a área de transferência.'))
      .catch(() => this.notificationService.info('Copie manualmente a chave exibida.'));
  }

  protected cancelDelete(): void {
    this.deviceToDelete = null;
  }

  protected deleteDevice(): void {
    if (!this.deviceToDelete) {
      return;
    }

    this.deleting = true;

    this.devicesService
      .delete(this.deviceToDelete.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notificationService.success('Microcontrolador excluído com sucesso.');
          this.deviceToDelete = null;
          if (this.devices.length === 1 && this.page > 0) {
            this.page -= 1;
          }
          this.loadDevices();
        },
        error: () => {
          this.notificationService.error('Não foi possível excluir o microcontrolador.');
        },
      });
  }

  private loadDevices(): void {
    this.loading = true;
    this.error = '';

    this.devicesService
      .search(this.search, this.page, this.pageSize)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (page) => {
          this.devices = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
        },
        error: () => {
          this.error = 'Não foi possível carregar os microcontroladores.';
        },
      });
  }

  private loadRoomOptions(): void {
    this.roomsService.search('', 0, 100).subscribe({
      next: (page) => {
        this.roomOptions = page.content.map((room) => ({
          label: `${room.identification} - ${room.block}`,
          value: room.id,
        }));
      },
      error: () => {
        this.notificationService.info('Não foi possível carregar as salas para vínculo.');
      },
    });
  }

  private populateDialog(device: EspDevice, readonly: boolean): void {
    this.selectedDevice = device;
    this.dialogReadonly = readonly;
    this.dialogError = '';
    this.form.enable();
    this.form.reset({
      macAddress: device.macAddress,
      ipAddress: device.ipAddress,
      connectionStatus: device.connectionStatus,
      infraredFrequency: device.infraredFrequency,
      roomId: device.roomId,
    });

    if (readonly) {
      this.form.disable();
    }

    this.dialogOpen = true;
  }

  private toNumberOrNull(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    return Number(value);
  }
}
