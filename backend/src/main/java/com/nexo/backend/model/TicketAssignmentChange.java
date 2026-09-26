package com.nexo.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_assignment_changes")
public class TicketAssignmentChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "previous_employee_id")
    private Employee previousEmployee; // null when nobody was assigned before

    @ManyToOne
    @JoinColumn(name = "new_employee_id")
    private Employee newEmployee; // null si se desasigna

    @ManyToOne
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Employee changedBy;

    @Column(nullable = false)
    private Instant changedAt = Instant.now();

    public TicketAssignmentChange() {
    }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public Employee getPreviousEmployee() { return previousEmployee; }
    public void setPreviousEmployee(Employee previousEmployee) { this.previousEmployee = previousEmployee; }
    public Employee getNewEmployee() { return newEmployee; }
    public void setNewEmployee(Employee newEmployee) { this.newEmployee = newEmployee; }
    public Employee getChangedBy() { return changedBy; }
    public void setChangedBy(Employee changedBy) { this.changedBy = changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
