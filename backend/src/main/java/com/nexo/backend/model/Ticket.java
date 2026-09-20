package com.nexo.backend.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false)
    private String problemDescription;

    @Embedded
    private DeviceInfo device;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee assignedEmployee;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Ticket() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getPublicId() { return publicId; }
    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    public DeviceInfo getDevice() { return device; }
    public void setDevice(DeviceInfo device) { this.device = device; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Employee getAssignedEmployee() { return assignedEmployee; }
    public void setAssignedEmployee(Employee assignedEmployee) { this.assignedEmployee = assignedEmployee; }
    public Instant getCreatedAt() { return createdAt; }
}
