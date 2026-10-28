export interface TicketPart {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number | null;
  lineTotal: number | null;
  addedByName: string;
  addedAt: string;
}

export interface PartsResponse {
  items: TicketPart[];
  total: number | null;
}

export interface NewPart {
  description: string;
  quantity: number;
  unitPrice: number | null;
}
