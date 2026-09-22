import { Injectable } from '@angular/core';
import { TicketService as CoreTicketService } from '../../core/services/ticket.service';

@Injectable({
  providedIn: 'root',
})
export class TicketService extends CoreTicketService {
}
