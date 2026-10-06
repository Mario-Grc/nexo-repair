package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.CreateTicketDto;
import com.nexo.backend.dto.DeviceDto;
import com.nexo.backend.dto.TicketDto;
import com.nexo.backend.dto.TimelineEntryDto;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.Customer;
import com.nexo.backend.model.DeviceType;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketAssignmentChange;
import com.nexo.backend.model.TicketNote;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.model.TicketStatusChange;
import com.nexo.backend.repository.CustomerRepository;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketAssignmentChangeRepository;
import com.nexo.backend.repository.TicketNoteRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import com.nexo.backend.repository.TicketStatusChangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nexo.backend.model.TicketStatus.CANCELLED;
import static com.nexo.backend.model.TicketStatus.COMPLETED;
import static com.nexo.backend.model.TicketStatus.DELIVERED;
import static com.nexo.backend.model.TicketStatus.IN_PROGRESS;
import static com.nexo.backend.model.TicketStatus.PENDING;
import static com.nexo.backend.model.TicketStatus.WAITING_FOR_PARTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

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

    @Captor
    private ArgumentCaptor<TicketStatusChange> statusChangeCaptor;
    @Captor
    private ArgumentCaptor<TicketAssignmentChange> assignmentChangeCaptor;

    // Expected table written by hand from the blueprint, never read from production code.
    static Stream<Arguments> allPairs() {
        Map<TicketStatus, Set<TicketStatus>> valid = Map.of(
                PENDING, Set.of(IN_PROGRESS, CANCELLED),
                IN_PROGRESS, Set.of(WAITING_FOR_PARTS, COMPLETED, CANCELLED),
                WAITING_FOR_PARTS, Set.of(IN_PROGRESS, CANCELLED),
                COMPLETED, Set.of(DELIVERED),
                DELIVERED, Set.of(),
                CANCELLED, Set.of());
        return Arrays.stream(TicketStatus.values()).flatMap(from ->
                Arrays.stream(TicketStatus.values())
                        .map(to -> Arguments.of(from, to, valid.get(from).contains(to))));
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    void changeStatus_respectsTransitionTable(TicketStatus from, TicketStatus to, boolean allowed) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(from);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee changer = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(changer));

        if (allowed) {
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
            // COMPLETED and CANCELLED require a note since the closing rule.
            String note = (to == COMPLETED || to == CANCELLED) ? "Closing note" : null;
            TicketDto result = ticketService.changeStatus(publicId, to, 7L, note);

            assertThat(result.status()).isEqualTo(to);
            assertThat(ticket.getStatus()).isEqualTo(to);
            verify(ticketRepository).save(ticket);
            verify(statusChangeRepository).save(statusChangeCaptor.capture());
            TicketStatusChange change = statusChangeCaptor.getValue();
            assertThat(change.getPreviousStatus()).isEqualTo(from);
            assertThat(change.getNewStatus()).isEqualTo(to);
            assertThat(change.getChangedBy()).isSameAs(changer);
        } else {
            assertThatThrownBy(() -> ticketService.changeStatus(publicId, to, 7L, null))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verify(ticketRepository, never()).save(any());
            verify(statusChangeRepository, never()).save(any());
        }
    }

    @Test
    void getTicket_inProgress_returnsNextStatusesInFixedOrder() {
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));

        TicketDto dto = ticketService.getTicket(ticket.getPublicId());

        assertThat(dto.allowedNextStatuses()).containsExactly(COMPLETED, WAITING_FOR_PARTS, CANCELLED);
    }

    @ParameterizedTest(name = "{0} has no next statuses")
    @EnumSource(value = TicketStatus.class, names = {"DELIVERED", "CANCELLED"})
    void getTicket_terminalStatus_returnsEmptyNextStatuses(TicketStatus status) {
        Ticket ticket = TestData.ticket(status);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));

        TicketDto dto = ticketService.getTicket(ticket.getPublicId());

        assertThat(dto.allowedNextStatuses()).isEmpty();
    }

    @Test
    void createTicket_valid_savesTicketAndInitialHistory() {
        Customer customer = TestData.customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        Employee creator = TestData.employee(7L, EmployeeRole.RECEPTION);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(creator));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateTicketDto dto = new CreateTicketDto("Screen stays black",
                new DeviceDto(DeviceType.LAPTOP, "Lenovo", "T14", "SN1"), 1L);

        TicketDto result = ticketService.createTicket(dto, 7L);

        assertThat(result.status()).isEqualTo(PENDING);
        verify(statusChangeRepository).save(statusChangeCaptor.capture());
        TicketStatusChange change = statusChangeCaptor.getValue();
        assertThat(change.getPreviousStatus()).isNull();
        assertThat(change.getNewStatus()).isEqualTo(PENDING);
        assertThat(change.getChangedBy()).isSameAs(creator);
    }

    @Test
    void createTicket_missingCustomer_throwsAndSavesNothing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        CreateTicketDto dto = new CreateTicketDto("Screen stays black",
                new DeviceDto(DeviceType.LAPTOP, "Lenovo", "T14", "SN1"), 99L);

        assertThatThrownBy(() -> ticketService.createTicket(dto, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any());
        verify(statusChangeRepository, never()).save(any());
    }

    @Test
    void assignEmployee_firstAssignment_recordsNullPrevious() {
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        Employee next = TestData.employee(3L, EmployeeRole.TECHNICIAN);
        Employee assignedBy = TestData.employee(7L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(next));
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(assignedBy));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.assignEmployee(ticket.getPublicId(), 3L, 7L);

        assertThat(ticket.getAssignedEmployee()).isSameAs(next);
        verify(assignmentChangeRepository).save(assignmentChangeCaptor.capture());
        TicketAssignmentChange change = assignmentChangeCaptor.getValue();
        assertThat(change.getPreviousEmployee()).isNull();
        assertThat(change.getNewEmployee()).isSameAs(next);
        assertThat(change.getChangedBy()).isSameAs(assignedBy);
    }

    @Test
    void assignEmployee_reassignment_recordsPreviousBeforeOverwriting() {
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        Employee previous = TestData.employee(2L, EmployeeRole.TECHNICIAN);
        ticket.setAssignedEmployee(previous);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        Employee next = TestData.employee(3L, EmployeeRole.TECHNICIAN);
        Employee assignedBy = TestData.employee(7L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(next));
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(assignedBy));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.assignEmployee(ticket.getPublicId(), 3L, 7L);

        assertThat(ticket.getAssignedEmployee()).isSameAs(next);
        verify(assignmentChangeRepository).save(assignmentChangeCaptor.capture());
        TicketAssignmentChange change = assignmentChangeCaptor.getValue();
        assertThat(change.getPreviousEmployee()).isSameAs(previous);
        assertThat(change.getNewEmployee()).isSameAs(next);
        assertThat(change.getChangedBy()).isSameAs(assignedBy);
    }

    @Test
    void assignEmployee_unassign_recordsNullNew() {
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        Employee previous = TestData.employee(2L, EmployeeRole.TECHNICIAN);
        ticket.setAssignedEmployee(previous);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        Employee assignedBy = TestData.employee(7L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(assignedBy));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.assignEmployee(ticket.getPublicId(), null, 7L);

        assertThat(ticket.getAssignedEmployee()).isNull();
        verify(assignmentChangeRepository).save(assignmentChangeCaptor.capture());
        TicketAssignmentChange change = assignmentChangeCaptor.getValue();
        assertThat(change.getPreviousEmployee()).isSameAs(previous);
        assertThat(change.getNewEmployee()).isNull();
        assertThat(change.getChangedBy()).isSameAs(assignedBy);
    }

    @Test
    void getTimeline_entriesFromAllSources_returnedInTimeOrder() {
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
        Employee ana = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        Employee bob = TestData.employee(8L, EmployeeRole.TECHNICIAN);

        TicketStatusChange statusChange = new TicketStatusChange();
        statusChange.setTicket(ticket);
        statusChange.setPreviousStatus(PENDING);
        statusChange.setNewStatus(IN_PROGRESS);
        statusChange.setChangedBy(ana);
        ReflectionTestUtils.setField(statusChange, "changedAt", Instant.parse("2026-03-03T10:00:00Z"));

        TicketNote note = new TicketNote();
        note.setTicket(ticket);
        note.setAuthor(ana);
        note.setText("Waiting on customer approval");
        ReflectionTestUtils.setField(note, "createdAt", Instant.parse("2026-03-01T10:00:00Z"));

        TicketAssignmentChange assignment = new TicketAssignmentChange();
        assignment.setTicket(ticket);
        assignment.setPreviousEmployee(null);
        assignment.setNewEmployee(bob);
        assignment.setChangedBy(ana);
        ReflectionTestUtils.setField(assignment, "changedAt", Instant.parse("2026-03-02T10:00:00Z"));

        when(statusChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of(statusChange));
        when(noteRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of(note));
        when(assignmentChangeRepository.findByTicketOrderByChangedAtAsc(ticket)).thenReturn(List.of(assignment));
        when(partRepository.findByTicketOrderByAddedAtAsc(ticket)).thenReturn(List.of());

        List<TimelineEntryDto> timeline = ticketService.getTimeline(ticket.getPublicId());

        assertThat(timeline).extracting(TimelineEntryDto::type)
                .containsExactly("NOTE", "ASSIGNMENT_CHANGE", "STATUS_CHANGE");
        assertThat(timeline).extracting(TimelineEntryDto::occurredAt).isSorted();
    }

    @Test
    void ticketLookups_missingTicket_throwNotFound() {
        UUID publicId = UUID.randomUUID();
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicket(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ticketService.changeStatus(publicId, IN_PROGRESS, 7L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ticketService.assignEmployee(publicId, 3L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ticketService.addNote(publicId, 7L, "hi"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ticketService.getTimeline(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
