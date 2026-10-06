package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.TicketDto;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.exception.NoteRequiredException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.model.TicketStatusChange;
import com.nexo.backend.repository.CustomerRepository;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketAssignmentChangeRepository;
import com.nexo.backend.repository.TicketNoteRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import com.nexo.backend.repository.TicketStatusChangeRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

// Note rule for closing tickets. Written before the implementation.
@ExtendWith(MockitoExtension.class)
class TicketServiceStatusNoteTest {

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

    // Handwritten table of valid transitions and whether the target requires a note,
    // crossed with every note variant. Each row is one fresh test with fresh mocks.
    static Stream<Arguments> validTransitionsWithNotes() {
        List<Arguments> transitions = List.of(
                Arguments.of(PENDING, IN_PROGRESS, false),
                Arguments.of(PENDING, CANCELLED, true),
                Arguments.of(IN_PROGRESS, COMPLETED, true),
                Arguments.of(IN_PROGRESS, WAITING_FOR_PARTS, false),
                Arguments.of(IN_PROGRESS, CANCELLED, true),
                Arguments.of(WAITING_FOR_PARTS, IN_PROGRESS, false),
                Arguments.of(WAITING_FOR_PARTS, CANCELLED, true),
                Arguments.of(COMPLETED, DELIVERED, false));
        List<String> notes = Arrays.asList(null, "", "   ", "  Repair done  ");
        return transitions.stream()
                .flatMap(t -> notes.stream()
                        .map(n -> Arguments.of(t.get()[0], t.get()[1], t.get()[2], n)));
    }

    @ParameterizedTest(name = "{0} -> {1} requiresNote={2} note=''{3}''")
    @MethodSource("validTransitionsWithNotes")
    void changeStatus_noteRule_perTargetStatus(TicketStatus from, TicketStatus to, boolean required, String note) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(from);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        Employee changer = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(changer));
        boolean blank = note == null || note.isBlank();

        if (required && blank) {
            assertThatThrownBy(() -> ticketService.changeStatus(publicId, to, 7L, note))
                    .isInstanceOf(NoteRequiredException.class);
            verify(ticketRepository, never()).save(any());
            verify(statusChangeRepository, never()).save(any());
        } else {
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TicketDto result = ticketService.changeStatus(publicId, to, 7L, note);

            assertThat(result.status()).isEqualTo(to);
            verify(statusChangeRepository).save(statusChangeCaptor.capture());
            String savedNote = statusChangeCaptor.getValue().getNote();
            if (blank) {
                assertThat(savedNote).isNull();
            } else {
                assertThat(savedNote).isEqualTo(note.trim());
            }
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void changeStatus_completedWithoutNote_throwsNoteRequired(String note) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(IN_PROGRESS);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        when(employeeRepository.findById(7L))
                .thenReturn(Optional.of(TestData.employee(7L, EmployeeRole.TECHNICIAN)));

        assertThatThrownBy(() -> ticketService.changeStatus(publicId, COMPLETED, 7L, note))
                .isInstanceOf(NoteRequiredException.class);
        verify(ticketRepository, never()).save(any());
        verify(statusChangeRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void changeStatus_cancelledWithoutNote_throwsNoteRequired(String note) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(PENDING);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        when(employeeRepository.findById(7L))
                .thenReturn(Optional.of(TestData.employee(7L, EmployeeRole.TECHNICIAN)));

        assertThatThrownBy(() -> ticketService.changeStatus(publicId, CANCELLED, 7L, note))
                .isInstanceOf(NoteRequiredException.class);
        verify(ticketRepository, never()).save(any());
        verify(statusChangeRepository, never()).save(any());
    }

    // Order matters. An unknown transition must fail as invalid
    // even when the note is also missing.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void changeStatus_invalidTransitionWithoutNote_throwsInvalidTransition(String note) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(PENDING);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        when(employeeRepository.findById(7L))
                .thenReturn(Optional.of(TestData.employee(7L, EmployeeRole.TECHNICIAN)));

        assertThatThrownBy(() -> ticketService.changeStatus(publicId, DELIVERED, 7L, note))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(ticketRepository, never()).save(any());
        verify(statusChangeRepository, never()).save(any());
    }
}
