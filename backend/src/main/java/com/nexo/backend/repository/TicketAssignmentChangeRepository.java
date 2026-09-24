package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketAssignmentChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAssignmentChangeRepository extends JpaRepository<TicketAssignmentChange, Long> {
    List<TicketAssignmentChange> findByTicketOrderByChangedAtAsc(Ticket ticket);
}
