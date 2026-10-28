import { Component, inject, input, model, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { TicketService } from '../../../core/services/ticket.service';
import { Ticket } from '../../../core/models/ticket';

@Component({
  selector: 'app-edit-description-dialog',
  imports: [DialogModule, TextareaModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './edit-description-dialog.html',
})
export class EditDescriptionDialog {
  private ticketService = inject(TicketService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `descriptionDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Public id of the ticket being edited. */
  publicId = input<string>('');
  /** Current description used to prefill the form every time the dialog opens. */
  currentDescription = input<string>('');

  saved = output<Ticket>();

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  // Max length mirrors the ticket creation form so both stay consistent.
  form = this.fb.nonNullable.group({
    problemDescription: ['', [Validators.required, Validators.maxLength(500), nonBlankText]],
  });

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({ problemDescription: this.currentDescription() });
  }

  save(): void {
    const publicId = this.publicId();
    if (this.form.invalid || this.isSaving() || !publicId) return;
    const problemDescription = this.form.getRawValue().problemDescription.trim();
    if (!problemDescription) return;

    this.isSaving.set(true);
    this.saveError.set(null);
    this.ticketService.updateDetails(publicId, { problemDescription }).subscribe({
      next: ticket => {
        this.isSaving.set(false);
        this.saved.emit(ticket);
        this.visible.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);
        if (error.status === 409) {
          this.saveError.set('The ticket is closed, the description cannot be changed.');
        } else if (error.status === 400) {
          this.saveError.set('The description must be between 1 and 500 characters.');
        } else {
          this.saveError.set('Could not update the description.');
        }
      },
    });
  }
}

// Whitespace only counts as blank, so the save button stays disabled.
function nonBlankText(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim() === '' ? { required: true } : null;
}
