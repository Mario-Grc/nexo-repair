package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByPublicId(UUID publicId);

    // Customer and technician travel with the page so the list needs no extra query per row.
    @Override
    @EntityGraph(attributePaths = {"customer", "assignedEmployee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);
}

