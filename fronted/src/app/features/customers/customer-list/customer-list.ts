import { Component, inject } from '@angular/core';
import { CustomerService } from '../customer.service';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { CustomerForm } from '../customer-form/customer-form';

@Component({
  selector: 'app-customer-list',
  imports: [CustomerForm],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.css',
})
export class CustomerList {
  private customerService = inject(CustomerService);

  customersResource = rxResource({
    stream: () => this.customerService.getCustomers(),
  });
}
