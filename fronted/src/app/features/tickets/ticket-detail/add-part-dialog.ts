import { Component, inject, input, model, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { TicketService } from '../../../core/services/ticket.service';
import { NewPart, TicketPart } from '../../../core/models/ticket-part';

@Component({
  selector: 'app-add-part-dialog',
  imports: [DialogModule, InputTextModule, InputNumberModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './add-part-dialog.html',
})
export class AddPartDialog {
  private partService = inject(TicketService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `partDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Public id of the ticket the part belongs to. */
  publicId = input<string>('');

  saved = output<TicketPart>();

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  form = this.fb.group({
    description: ['', [Validators.required, Validators.maxLength(200), nonBlankText]],
    quantity: [1, [Validators.required, Validators.min(1), Validators.max(999)]],
    unitPrice: this.fb.control<number | null>(null, { validators: [Validators.min(0)] }),
  });

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({ description: '', quantity: 1, unitPrice: null });
  }

  save(): void {
    const publicId = this.publicId();
    if (this.form.invalid || this.isSaving() || !publicId) return;
    const raw = this.form.getRawValue();
    const description = (raw.description ?? '').trim();
    const quantity = raw.quantity ?? 1;
    if (!description) return;
    const payload: NewPart = { description, quantity, unitPrice: raw.unitPrice ?? null };

    this.isSaving.set(true);
    this.saveError.set(null);
    this.partService.addPart(publicId, payload).subscribe({
      next: part => {
        this.isSaving.set(false);
        this.saved.emit(part);
        this.visible.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.saveError.set(error.status === 409 ? 'The ticket is closed, parts cannot be added.' : 'Could not add the part.');
      },
    });
  }
}

// Whitespace only counts as blank, so the save button stays disabled.
function nonBlankText(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim() === '' ? { required: true } : null;
}
