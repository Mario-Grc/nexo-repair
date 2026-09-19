import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TicketService } from '../ticket.service';

@Component({
  selector: 'app-ticket-list',
  templateUrl: './ticket-list.html',
})
export class TicketList {
  private ticketService = inject(TicketService);

  ticketsResource = rxResource({
    stream: () => this.ticketService.getTickets(),
  });
}
