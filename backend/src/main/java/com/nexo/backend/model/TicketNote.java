package com.nexo.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_notes")
public class TicketNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Employee author;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public TicketNote() {
    }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public Employee getAuthor() { return author; }
    public void setAuthor(Employee author) { this.author = author; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Instant getCreatedAt() { return createdAt; }
}
