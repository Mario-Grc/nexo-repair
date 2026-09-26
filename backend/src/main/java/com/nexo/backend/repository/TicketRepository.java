package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByPublicId(UUID publicId);

    List<Ticket> findByCustomerId(Long customerId);
}

