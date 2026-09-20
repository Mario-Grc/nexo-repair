package com.nexo.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    @Column(nullable = false)
    private boolean active = true;

    public Employee() {
    }

    public Employee(String name, String email, String passwordHash, EmployeeRole role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
