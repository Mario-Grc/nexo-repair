package com.nexo.backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.nexo.backend.dto.PageResponse;
import com.nexo.backend.dto.TicketFilter;
import com.nexo.backend.exception.InvalidFilterException;
import com.nexo.backend.repository.CustomerRepository;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketAssignmentChangeRepository;
import com.nexo.backend.repository.TicketNoteRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import com.nexo.backend.repository.TicketStatusChangeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rules around getTickets without touching the database.
@ExtendWith(MockitoExtension.class)
class TicketServiceFilteringTest {

    private static final ZoneId ZONE = ZoneId.of("Atlantic/Canary");

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

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                customerRepository,
                employeeRepository,
                statusChangeRepository,
                noteRepository,
                assignmentChangeRepository,
                partRepository,
                ZONE);
    }

    private static TicketFilter emptyFilter() {
        return new TicketFilter(null, null, null, null, null, null, null);
    }

    @Test
    void getTickets_unassignedWithTechnician_throws() {
        TicketFilter filter = new TicketFilter(null, 3L, true, null, null, null, null);

        assertThatThrownBy(() -> ticketService.getTickets(filter, 0, 20))
                .isInstanceOf(InvalidFilterException.class);
        verify(ticketRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getTickets_startDateAfterEndDate_throws() {
        TicketFilter filter = new TicketFilter(
                null, null, null,
                LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 12), null, null);

        assertThatThrownBy(() -> ticketService.getTickets(filter, 0, 20))
                .isInstanceOf(InvalidFilterException.class);
        verify(ticketRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getTickets_hugeSize_clampsToOneHundred() {
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<?> response = ticketService.getTickets(emptyFilter(), 0, 1000);

        assertThat(response).isNotNull();
        verify(ticketRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getTickets_zeroSize_clampsToOne() {
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        ticketService.getTickets(emptyFilter(), 0, 0);

        verify(ticketRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getTickets_negativePage_clampsToZero() {
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        ticketService.getTickets(emptyFilter(), -1, 20);

        verify(ticketRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void getTickets_ordersByCreatedAtDescThenIdDesc() {
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        ticketService.getTickets(emptyFilter(), 0, 20);

        verify(ticketRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
        assertThat(List.of("createdAt", "id"))
                .containsExactlyElementsOf(
                        pageable.getSort().stream().map(order -> order.getProperty()).toList());
    }
}
