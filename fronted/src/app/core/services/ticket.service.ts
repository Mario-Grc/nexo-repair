import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NewTicket, Ticket } from '../models/ticket';
import { TicketStatus } from '../models/ticket-status';
import { NewPart, PartsResponse, TicketPart } from '../models/ticket-part';
import { TimelineEntry } from '../models/timeline-entry';
import { environment } from '../../../environments/environment';
import type { TicketFilters } from '../models/ticket-filters';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type TicketQuery = Partial<TicketFilters> & { customerId?: number };

@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/tickets`;

  getTickets(query: TicketQuery): Observable<PageResponse<Ticket>> {
    let params = new HttpParams();
    for (const status of query.status ?? []) params = params.append('status', status);
    if (query.assignee === 'unassigned') {
      params = params.set('unassigned', 'true');
    } else if (typeof query.assignee === 'number') {
      params = params.set('assignedEmployeeId', String(query.assignee));
    }
    if (query.createdFrom) params = params.set('createdFrom', query.createdFrom);
    if (query.createdTo) params = params.set('createdTo', query.createdTo);
    const q = query.q?.trim();
    if (q) params = params.set('q', q);
    if (query.customerId != null) params = params.set('customerId', String(query.customerId));
    if (query.page != null) params = params.set('page', String(query.page));
    if (query.size != null) params = params.set('size', String(query.size));
    return this.http.get<PageResponse<Ticket>>(this.baseUrl, { params });
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

  updateDetails(publicId: string, dto: { problemDescription: string }): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.baseUrl}/${publicId}/details`, dto);
  }

  addNote(publicId: string, dto: { text: string }): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${publicId}/notes`, dto);
  }

  getTimeline(publicId: string): Observable<TimelineEntry[]> {
    return this.http.get<TimelineEntry[]>(`${this.baseUrl}/${publicId}/timeline`);
  }

  getParts(publicId: string): Observable<PartsResponse> {
    return this.http.get<PartsResponse>(`${this.baseUrl}/${publicId}/parts`);
  }

  addPart(publicId: string, part: NewPart): Observable<TicketPart> {
    return this.http.post<TicketPart>(`${this.baseUrl}/${publicId}/parts`, part);
  }

  removePart(publicId: string, partId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${publicId}/parts/${partId}`);
  }
}