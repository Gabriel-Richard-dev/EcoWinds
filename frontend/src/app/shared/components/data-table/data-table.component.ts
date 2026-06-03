import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export type TableCellKind = 'text' | 'badge' | 'boolean' | 'date' | 'datetime' | 'time';
export type TableAction = 'view' | 'edit' | 'delete' | 'apikey' | 'toggle';

export interface TableColumn {
  key: string;
  label: string;
  kind?: TableCellKind;
  align?: 'left' | 'center' | 'right';
  fallback?: string;
  valueMap?: Record<string, string>;
  trueLabel?: string;
  falseLabel?: string;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class DataTableComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() searchPlaceholder = 'Buscar...';
  @Input() createLabel = 'Cadastrar';
  @Input() showSearch = true;
  @Input() showCreateButton = true;
  @Input() loading = false;
  @Input() error = '';
  @Input() emptyMessage = 'Nenhum registro encontrado.';
  @Input() searchTerm = '';
  @Input() page = 0;
  @Input() pageSize = 10;
  @Input() totalPages = 0;
  @Input() totalElements = 0;
  @Input() columns: readonly TableColumn[] = [];
  @Input() rows: readonly unknown[] = [];
  @Input() actions: readonly TableAction[] = [];
  @Input() pageSizeOptions: readonly number[] = [10, 20, 50];
  @Input() toggleKey = '';

  @Output() searchChange = new EventEmitter<string>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();
  @Output() createRecord = new EventEmitter<void>();
  @Output() view = new EventEmitter<unknown>();
  @Output() edit = new EventEmitter<unknown>();
  @Output() delete = new EventEmitter<unknown>();
  @Output() apikey = new EventEmitter<unknown>();
  @Output() toggle = new EventEmitter<unknown>();

  protected readonly skeletonRows = Array.from({ length: 8 }, (_, index) => index);

  protected get hasActions(): boolean {
    return this.actions.length > 0;
  }

  protected get computedTotalPages(): number {
    if (this.totalPages > 0) {
      return this.totalPages;
    }

    return this.totalElements > 0 ? Math.ceil(this.totalElements / this.pageSize) : 0;
  }

  protected get startItem(): number {
    return this.totalElements === 0 ? 0 : this.page * this.pageSize + 1;
  }

  protected get endItem(): number {
    return Math.min((this.page + 1) * this.pageSize, this.totalElements);
  }

  protected onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchChange.emit(input.value);
  }

  protected onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSizeChange.emit(Number(select.value));
  }

  protected previousPage(): void {
    if (this.page > 0) {
      this.pageChange.emit(this.page - 1);
    }
  }

  protected nextPage(): void {
    if (this.page + 1 < this.computedTotalPages) {
      this.pageChange.emit(this.page + 1);
    }
  }

  protected rowValue(row: unknown, key: string): unknown {
    if (row && typeof row === 'object' && key in row) {
      return (row as Record<string, unknown>)[key];
    }

    return null;
  }

  protected trackRow(index: number, row: unknown): unknown {
    const id = this.rowValue(row, 'id');
    return id ?? index;
  }

  protected cellClass(column: TableColumn): string {
    return column.align ? `cell--${column.align}` : '';
  }

  protected formatValue(value: unknown, column: TableColumn): string {
    if (value === null || value === undefined || value === '') {
      return column.fallback ?? '--';
    }

    if (column.valueMap && typeof value === 'string') {
      return column.valueMap[value] ?? value;
    }

    if (column.kind === 'boolean') {
      return value ? (column.trueLabel ?? 'Conectado') : (column.falseLabel ?? 'Desconectado');
    }

    if (column.kind === 'date' || column.kind === 'datetime') {
      return this.formatDate(String(value), column.kind === 'datetime');
    }

    if (column.kind === 'time') {
      return String(value).slice(0, 5);
    }

    return String(value);
  }

  protected badgeClass(value: unknown): string {
    const normalized = String(value).toLowerCase();

    if (value === true || ['active', 'ligar', 'connected', 'programado'].includes(normalized)) {
      return 'status-chip--success';
    }

    if (value === false || ['inactive', 'desligar', 'disconnected', 'erro'].includes(normalized)) {
      return 'status-chip--danger';
    }

    if (['maintenance', 'manual', 'pendente'].includes(normalized)) {
      return 'status-chip--warning';
    }

    return 'status-chip--neutral';
  }

  protected actionLabel(action: TableAction): string {
    const labels: Record<TableAction, string> = {
      view: 'Visualizar',
      edit: 'Editar',
      delete: 'Excluir',
      apikey: 'Gerar API key',
      toggle: 'Ligar/Desligar',
    };

    return labels[action];
  }

  protected actionIcon(action: TableAction): string {
    const icons: Record<TableAction, string> = {
      view: 'pi pi-eye',
      edit: 'pi pi-pencil',
      delete: 'pi pi-trash',
      apikey: 'pi pi-key',
      toggle: 'pi pi-power-off',
    };

    return icons[action];
  }

  protected emitAction(action: TableAction, row: unknown): void {
    if (action === 'view') {
      this.view.emit(row);
      return;
    }

    if (action === 'edit') {
      this.edit.emit(row);
      return;
    }

    if (action === 'apikey') {
      this.apikey.emit(row);
      return;
    }

    if (action === 'toggle') {
      this.toggle.emit(row);
      return;
    }

    this.delete.emit(row);
  }

  protected toggleActive(row: unknown): boolean {
    return Boolean(this.toggleKey && this.rowValue(row, this.toggleKey));
  }

  private formatDate(value: string, withTime: boolean): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    const options: Intl.DateTimeFormatOptions = {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    };

    if (withTime) {
      options.hour = '2-digit';
      options.minute = '2-digit';
    }

    return new Intl.DateTimeFormat('pt-BR', options).format(date);
  }
}
