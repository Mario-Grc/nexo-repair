import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NewTicket, Ticket } from '../models/ticket';
import { TicketStatus } from '../models/ticket-status';
import { TimelineEntry } from '../models/timeline-entry';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/tickets`;

  getTickets(customerId?: number): Observable<Ticket[]> {
    let params = new HttpParams();
    if (customerId != null) params = params.set('customerId', customerId);
    return this.http.get<Ticket[]>(this.baseUrl, { params });
  }

  getTicket(publicId: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.baseUrl}/${publicId}`);
  }

  createTicket(ticket: NewTicket): Observable<Ticket> {
    return this.http.post<Ticket>(this.baseUrl, ticket);
  }

  assignEmployee(publicId: string, dto: { employeeId: number | null }): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.baseUrl}/${publicId}/assign`, dto);
  }

  changeStatus(publicId: string, dto: { newStatus: TicketStatus; note: string | null }): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.baseUrl}/${publicId}/status`, dto);
  }

  addNote(publicId: string, dto: { text: string }): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${publicId}/notes`, dto);
  }

  getTimeline(publicId: string): Observable<TimelineEntry[]> {
    return this.http.get<TimelineEntry[]>(`${this.baseUrl}/${publicId}/timeline`);
  }
}