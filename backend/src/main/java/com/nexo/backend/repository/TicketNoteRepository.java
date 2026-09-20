package com.nexo.backend.repository;

import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketNoteRepository extends JpaRepository<TicketNote, Long> {
    List<TicketNote> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
