package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketStatusChangeRepository extends JpaRepository<TicketStatusChange, Long> {
    List<TicketStatusChange> findByTicketOrderByChangedAtAsc(Ticket ticket);
}