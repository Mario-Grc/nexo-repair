import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CustomerService } from '../../../core/services/customer.service';
import { Customer } from '../../../core/models/customer';
import { CustomerFormDialog } from '../customer-form-dialog/customer-form-dialog';

@Component({
  selector: 'app-customer-list',
  imports: [TableModule, ButtonModule, CustomerFormDialog],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.css',
})
export class CustomerList implements OnInit {
  private customerService = inject(CustomerService);
  private router = inject(Router);

  customers = signal<Customer[]>([]);
  dialogVisible = signal(false);

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.customerService.getCustomers().subscribe(list => this.customers.set(list));
  }

  openNew(): void {
    this.dialogVisible.set(true);
  }

  openDetail(customer: Customer): void {
    this.router.navigate(['/customers', customer.id]);
  }
}
