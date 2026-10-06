package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.TimelineEntryDto;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketPart;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.repository.CustomerRepository;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketAssignmentChangeRepository;
import com.nexo.backend.repository.TicketNoteRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import com.nexo.backend.repository.TicketStatusChangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Parts in the timeline. Written before the implementation.
@ExtendWith(MockitoExtension.class)
class TicketServiceTimelinePartsTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TicketStatusChangeRepository statusChangeRepository;
    @Mock
    private TicketNoteRepository noteRepository;
    @Mock
    private TicketAssignmentChangeRepository assignmentChangeRepository;
    @Mock
    private TicketPartRepository partRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void getTimeline_addedAndRemovedPart_producesTwoOrderedEntries() {
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        when(statusChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of());
        when(noteRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of());
        when(assignmentChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of());

        Employee ana = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        Employee bob = TestData.employee(8L, EmployeeRole.TECHNICIAN);
        TicketPart part = new TicketPart(ticket, "Brake pads", 2, new BigDecimal("10.50"), ana);
        ReflectionTestUtils.setField(part, "addedAt", Instant.parse("2026-03-04T10:00:00Z"));
        part.markRemoved(bob, Instant.parse("2026-03-05T10:00:00Z"));
        when(partRepository.findByTicketOrderByAddedAtAsc(ticket)).thenReturn(List.of(part));

        List<TimelineEntryDto> timeline = ticketService.getTimeline(ticket.getPublicId());

        assertThat(timeline).hasSize(2);
        assertThat(timeline).extracting(TimelineEntryDto::type)
                .containsExactly("PART_ADDED", "PART_REMOVED");
        assertThat(timeline).extracting(TimelineEntryDto::occurredAt).isSorted();
    }

    @Test
    void getTimeline_visiblePart_producesSingleAddedEntry() {
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        when(statusChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of());
        when(noteRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of());
        when(assignmentChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of());

        Employee ana = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        TicketPart part = new TicketPart(ticket, "Chain", 1, null, ana);
        ReflectionTestUtils.setField(part, "addedAt", Instant.parse("2026-03-04T10:00:00Z"));
        when(partRepository.findByTicketOrderByAddedAtAsc(ticket)).thenReturn(List.of(part));

        List<TimelineEntryDto> timeline = ticketService.getTimeline(ticket.getPublicId());

        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).type()).isEqualTo("PART_ADDED");
    }
}
