import { Component, inject, input, model, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { TicketService } from '../../../core/services/ticket.service';
import { Customer } from '../../../core/models/customer';
import { Ticket } from '../../../core/models/ticket';
import { DEVICE_TYPE_OPTIONS } from '../../../core/models/device-type';

@Component({
  selector: 'app-ticket-form-dialog',
  imports: [DialogModule, InputTextModule, TextareaModule, SelectModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './ticket-form-dialog.html',
})
export class TicketFormDialog {
  private ticketService = inject(TicketService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `dialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Customer options for the dropdown, loaded by the parent list. */
  customers = input<Customer[]>([]);

  saved = output<Ticket>();

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  deviceTypeOptions = DEVICE_TYPE_OPTIONS;

  form = this.fb.nonNullable.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    problemDescription: ['', [Validators.required, Validators.maxLength(500)]],
    device: this.fb.nonNullable.group({
      type: ['OTHER' as const, Validators.required],
      brand: [''],
      model: [''],
      identifier: [''],
    }),
  });

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({
      customerId: null,
      problemDescription: '',
      device: { type: 'OTHER', brand: '', model: '', identifier: '' },
    });
  }

  save(): void {
    if (this.form.invalid || this.isSaving()) return;
    const customerId = this.form.controls.customerId.value;
    if (customerId === null) return;

    const raw = this.form.getRawValue();
    this.isSaving.set(true);
    this.saveError.set(null);
    this.ticketService
      .createTicket({
        customerId,
        problemDescription: raw.problemDescription,
        device: { ...raw.device, identifier: raw.device.identifier || null },
      })
      .subscribe({
        next: ticket => {
          this.isSaving.set(false);
          this.saved.emit(ticket);
          this.visible.set(false);
        },
        error: () => {
          this.isSaving.set(false);
          this.saveError.set('Could not create the ticket.');
        },
      });
  }
}
