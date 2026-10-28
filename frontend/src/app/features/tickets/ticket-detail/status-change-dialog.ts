import { Component, computed, inject, input, model, output } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { DEFAULT_STATUS_NOTE, STATUS_NOTE, TICKET_STATUS_LABELS, TicketStatus } from '../../../core/models/ticket-status';

// Kept from the old confirm flow so terminal steps still warn before running.
const STATUS_HINT: Partial<Record<TicketStatus, string>> = {
  COMPLETED: 'Once completed, the ticket can only be delivered. It cannot go back to In progress.',
  DELIVERED: 'This closes the ticket permanently. Continue?',
  CANCELLED: 'The ticket will be removed from the flow permanently. Continue?',
};

@Component({
  selector: 'app-status-change-dialog',
  imports: [DialogModule, TextareaModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './status-change-dialog.html',
})
export class StatusChangeDialog {
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `statusDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Destination status, or null when the ticket is closed and no change applies. */
  status = input<TicketStatus | null>(null);

  confirmed = output<string | null>();
  closed = output<void>();

  noteConfig = computed(() => {
    const target = this.status();
    return (target && STATUS_NOTE[target]) ?? DEFAULT_STATUS_NOTE;
  });
  dialogHeader = computed(() => {
    const target = this.status();
    return target ? `Mark as ${TICKET_STATUS_LABELS[target]}?` : 'Change status';
  });
  confirmLabel = computed(() => {
    const target = this.status();
    return target ? `Mark as ${TICKET_STATUS_LABELS[target]}` : 'Confirm';
  });
  hint = computed(() => {
    const target = this.status();
    return (target && STATUS_HINT[target]) ?? null;
  });
  isDanger = computed(() => this.status() === 'CANCELLED');

  form = this.fb.nonNullable.group({
    note: [''],
  });

  private justConfirmed = false;

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    const config = this.noteConfig();
    const control = this.form.controls.note;
    control.setValidators(
      config.required ? [Validators.required, Validators.maxLength(255), nonBlankText] : [Validators.maxLength(255)],
    );
    control.updateValueAndValidity();
    this.form.reset({ note: '' });
    this.justConfirmed = false;
  }

  // onHide also runs after confirm, so skip the closed event in that case.
  onHide(): void {
    if (this.justConfirmed) {
      this.justConfirmed = false;
      return;
    }
    this.closed.emit();
  }

  cancel(): void {
    this.visible.set(false);
  }

  confirm(): void {
    if (this.form.invalid) return;
    const raw = this.form.controls.note.value.trim();
    this.justConfirmed = true;
    this.confirmed.emit(raw ? raw : null);
    this.visible.set(false);
  }
}

// Whitespace only is blank too, so the confirm button stays disabled.
// It reports as required so the template needs a single error check.
function nonBlankText(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim() === '' ? { required: true } : null;
}
