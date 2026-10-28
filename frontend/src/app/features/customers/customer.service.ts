import { Injectable } from '@angular/core';
import { CustomerService as CoreCustomerService } from '../../core/services/customer.service';

@Injectable({
  providedIn: 'root'
})
export class CustomerService extends CoreCustomerService {}