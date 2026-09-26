package com.nexo.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_status_changes")
public class TicketStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    private TicketStatus previousStatus; // null means ticket creation

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus newStatus;

    @ManyToOne
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Employee changedBy;

    @Column(nullable = false)
    private Instant changedAt = Instant.now();

    private String note;

    public TicketStatusChange() {
    }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public TicketStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(TicketStatus previousStatus) { this.previousStatus = previousStatus; }
    public TicketStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TicketStatus newStatus) { this.newStatus = newStatus; }
    public Employee getChangedBy() { return changedBy; }
    public void setChangedBy(Employee changedBy) { this.changedBy = changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
