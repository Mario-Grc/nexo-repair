package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketPartRepository extends JpaRepository<TicketPart, Long> {
    // Only the visible lines, oldest first.
    List<TicketPart> findByTicketAndRemovedAtIsNullOrderByAddedAtAsc(Ticket ticket);

    // Every line including removed ones, for the timeline.
    List<TicketPart> findByTicketOrderByAddedAtAsc(Ticket ticket);
}
