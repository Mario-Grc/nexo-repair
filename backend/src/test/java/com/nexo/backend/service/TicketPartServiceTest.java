package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.AddPartDto;
import com.nexo.backend.dto.PartsDto;
import com.nexo.backend.dto.TicketPartDto;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.TicketClosedException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketPart;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit tests for the two rules. Written before the implementation.
@ExtendWith(MockitoExtension.class)
class TicketPartServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketPartRepository partRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TicketPartService partService;

    @Captor
    private ArgumentCaptor<TicketPart> partCaptor;

    // Handwritten table. Only DELIVERED and CANCELLED are closed.
    static Stream<Arguments> allStatuses() {
        return Stream.of(
                Arguments.of(TicketStatus.PENDING, false),
                Arguments.of(TicketStatus.IN_PROGRESS, false),
                Arguments.of(TicketStatus.WAITING_FOR_PARTS, false),
                Arguments.of(TicketStatus.COMPLETED, false),
                Arguments.of(TicketStatus.DELIVERED, true),
                Arguments.of(TicketStatus.CANCELLED, true));
    }

    @ParameterizedTest(name = "add in {0}")
    @MethodSource("allStatuses")
    void add_eachStatus_onlyClosedTicketsReject(TicketStatus status, boolean closed) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(status);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);

        if (closed) {
            assertThatThrownBy(() -> partService.add(publicId, new AddPartDto("Brake pads", 2, null), 7L))
                    .isInstanceOf(TicketClosedException.class);
            verify(partRepository, never()).save(any());
        } else {
            when(employeeRepository.findById(7L)).thenReturn(Optional.of(author));
            when(partRepository.save(any(TicketPart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TicketPartDto result = partService.add(publicId, new AddPartDto("  Brake pads  ", 2, null), 7L);

            verify(partRepository).save(partCaptor.capture());
            TicketPart saved = partCaptor.getValue();
            assertThat(saved.getDescription()).isEqualTo("Brake pads");
            assertThat(saved.getTicket()).isSameAs(ticket);
            assertThat(saved.getAddedBy()).isSameAs(author);
            assertThat(result.description()).isEqualTo("Brake pads");
            assertThat(result.addedByName()).isEqualTo(author.getName());
        }
    }

    @Test
    void remove_visiblePart_marksRemovedByAndAt() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        Employee remover = TestData.employee(8L, EmployeeRole.TECHNICIAN);
        TicketPart part = new TicketPart(ticket, "Chain", 1, null, author);
        ReflectionTestUtils.setField(part, "id", 5L);
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));
        when(employeeRepository.findById(8L)).thenReturn(Optional.of(remover));

        partService.remove(publicId, 5L, 8L);

        assertThat(part.isRemoved()).isTrue();
        assertThat(part.getRemovedBy()).isSameAs(remover);
        assertThat(part.getRemovedAt()).isNotNull();
    }

    @Test
    void remove_closedTicket_throwsAndChangesNothing() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.DELIVERED);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> partService.remove(publicId, 5L, 8L))
                .isInstanceOf(TicketClosedException.class);
        verify(partRepository, never()).findById(any());
    }

    @Test
    void remove_partFromOtherTicket_throwsNotFound() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Ticket otherTicket = TestData.ticket(TicketStatus.IN_PROGRESS);
        otherTicket.setId(99L);
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        TicketPart part = new TicketPart(otherTicket, "Chain", 1, null, author);
        ReflectionTestUtils.setField(part, "id", 5L);
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> partService.remove(publicId, 5L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(part.isRemoved()).isFalse();
    }

    @Test
    void remove_alreadyRemovedPart_throwsNotFound() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        TicketPart part = new TicketPart(ticket, "Chain", 1, null, author);
        ReflectionTestUtils.setField(part, "id", 5L);
        part.markRemoved(author, Instant.now());
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> partService.remove(publicId, 5L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_moneyMath_ignoresLinesWithoutPrice() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        TicketPart priced = new TicketPart(ticket, "Brake pads", 2, new BigDecimal("10.50"), author);
        TicketPart free = new TicketPart(ticket, "Labor check", 1, null, author);
        when(partRepository.findByTicketAndRemovedAtIsNullOrderByAddedAtAsc(ticket))
                .thenReturn(List.of(priced, free));

        PartsDto result = partService.list(publicId);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(result.items().get(1).lineTotal()).isNull();
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("21.00"));
    }

    @Test
    void list_noPricedLines_totalIsNull() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        ticket.setId(10L);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee author = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        TicketPart free = new TicketPart(ticket, "Labor check", 1, null, author);
        when(partRepository.findByTicketAndRemovedAtIsNullOrderByAddedAtAsc(ticket))
                .thenReturn(List.of(free));

        PartsDto result = partService.list(publicId);

        assertThat(result.total()).isNull();
    }
}
