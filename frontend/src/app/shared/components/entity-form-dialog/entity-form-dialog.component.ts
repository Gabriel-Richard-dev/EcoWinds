import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

export interface EntityFormOption {
  label: string;
  value: string | number | boolean | null;
}



export interface EntityFormField {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'checkbox' | 'time' | 'datetime-local';
  placeholder?: string;
  hint?: string;
  required?: boolean;
  full?: boolean;
  options?: readonly EntityFormOption[];
}

@Component({
  selector: 'app-entity-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './entity-form-dialog.component.html',
  styleUrl: './entity-form-dialog.component.scss',
})
export class EntityFormDialogComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() description = '';
  @Input() submitLabel = 'Salvar';
  @Input() loading = false;
  @Input({ alias: 'readonly' }) viewOnly = false;
  @Input() error = '';
  @Input() fields: readonly EntityFormField[] = [];
  @Input({ required: true }) form!: FormGroup;

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();

  protected fieldInvalid(field: EntityFormField): boolean {
    const control = this.form.get(field.key);
    return Boolean(control?.invalid && (control.dirty || control.touched));
  }

  protected fieldError(field: EntityFormField): string {
    const control = this.form.get(field.key);

    if (!control?.errors) {
      return '';
    }

    if ('required' in control.errors) {
      return `${field.label} é obrigatório.`;
    }

    if ('email' in control.errors) {
      return 'Informe um e-mail válido.';
    }

    return 'Valor inválido.';
  }
}
