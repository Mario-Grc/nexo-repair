import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs/internal/Observable";
import { Customer, NewCustomer } from "./customer.model";

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
    private http = inject(HttpClient);
    private readonly apiUrl = 'http://localhost:8080/api/customers';

    getCustomers(): Observable<Customer[]> {
        return this.http.get<Customer[]>(this.apiUrl);
    }

    createCustomer(customer: NewCustomer): Observable<Customer> { 
        return this.http.post<Customer>(this.apiUrl, customer)
    }
}