package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.TicketDto;
import com.nexo.backend.dto.UpdateTicketDetailsDto;
import com.nexo.backend.exception.TicketClosedException;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.repository.CustomerRepository;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketAssignmentChangeRepository;
import com.nexo.backend.repository.TicketNoteRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import com.nexo.backend.repository.TicketStatusChangeRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rules for editing the problem description. Written before the implementation.
@ExtendWith(MockitoExtension.class)
class TicketServiceDetailsTest {

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

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "closed {0} rejects edits")
    @EnumSource(value = TicketStatus.class, names = {"DELIVERED", "CANCELLED"})
    void updateDetails_closedTicket_throwsClosedAndSavesNothing(TicketStatus status) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(status);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateDetails(publicId, "New description"))
                .isInstanceOf(TicketClosedException.class);
        verify(ticketRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void updateDetails_blankOrNull_throwsGuardAndSavesNothing(String input) {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.PENDING);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateDetails(publicId, input))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateDetails_tooLong_throwsGuardAndSavesNothing() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        String tooLong = "a".repeat(501);

        assertThatThrownBy(() -> ticketService.updateDetails(publicId, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateDetails_validInput_savesTrimmedValue() {
        UUID publicId = UUID.randomUUID();
        Ticket ticket = TestData.ticket(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByPublicId(publicId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDto result = ticketService.updateDetails(publicId, "  Screen flickers on boot  ");

        assertThat(ticket.getProblemDescription()).isEqualTo("Screen flickers on boot");
        assertThat(result.problemDescription()).isEqualTo("Screen flickers on boot");
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateDetailsDto_blankDescription_producesViolation() {
        assertThat(validator.validate(new UpdateTicketDetailsDto(""))).isNotEmpty();
        assertThat(validator.validate(new UpdateTicketDetailsDto("   "))).isNotEmpty();
    }

    @Test
    void updateDetailsDto_nullDescription_producesViolation() {
        assertThat(validator.validate(new UpdateTicketDetailsDto(null))).isNotEmpty();
    }

    @Test
    void updateDetailsDto_tooLongDescription_producesViolation() {
        assertThat(validator.validate(new UpdateTicketDetailsDto("a".repeat(501)))).isNotEmpty();
    }

    @Test
    void updateDetailsDto_validDescription_producesNoViolations() {
        assertThat(validator.validate(new UpdateTicketDetailsDto("Screen stays black"))).isEmpty();
    }
}
