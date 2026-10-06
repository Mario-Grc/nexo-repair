package com.nexo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

// One row per used part. Removal is logical so the timeline keeps both events.
@Entity
@Table(name = "ticket_parts")
public class TicketPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private int quantity;

    // What the customer is charged. Null means no price yet.
    // Never use float or double for money.
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "added_by_id", nullable = false)
    private Employee addedBy;

    @Column(nullable = false)
    private Instant addedAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "removed_by_id")
    private Employee removedBy;

    private Instant removedAt;

    public TicketPart() {
    }

    public TicketPart(Ticket ticket, String description, int quantity, BigDecimal unitPrice, Employee addedBy) {
        this.ticket = ticket;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.addedBy = addedBy;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public void markRemoved(Employee by, Instant at) {
        this.removedBy = by;
        this.removedAt = at;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Employee getAddedBy() {
        return addedBy;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public Employee getRemovedBy() {
        return removedBy;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }
}
