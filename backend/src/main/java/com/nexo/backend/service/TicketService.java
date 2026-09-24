package com.nexo.backend.service;

import com.nexo.backend.dto.*;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.*;
import com.nexo.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.PENDING, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.WAITING_FOR_PARTS, TicketStatus.COMPLETED, TicketStatus.CANCELLED),
            TicketStatus.WAITING_FOR_PARTS, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.COMPLETED, Set.of(TicketStatus.DELIVERED),
            TicketStatus.DELIVERED, Set.of(),
            TicketStatus.CANCELLED, Set.of()
    );

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketStatusChangeRepository statusChangeRepository;
    private final TicketNoteRepository noteRepository;
    private final TicketAssignmentChangeRepository assignmentChangeRepository;

    public TicketService(TicketRepository ticketRepository,
                         CustomerRepository customerRepository,
                         EmployeeRepository employeeRepository,
                         TicketStatusChangeRepository statusChangeRepository,
                         TicketNoteRepository noteRepository,
                         TicketAssignmentChangeRepository assignmentChangeRepository) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.statusChangeRepository = statusChangeRepository;
        this.noteRepository = noteRepository;
        this.assignmentChangeRepository = assignmentChangeRepository;
    }

    public List<TicketDto> getAllTickets() {
        return ticketRepository.findAll().stream().map(this::toDto).toList();
    }

    public TicketDto getTicket(UUID publicId) {
        return toDto(findTicketOrThrow(publicId));
    }

    public TicketDto createTicket(CreateTicketDto dto) {
        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + dto.customerId() + " no encontrado"));
        Employee createdBy = employeeRepository.findById(dto.createdByEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + dto.createdByEmployeeId() + " no encontrado"));

        Ticket ticket = new Ticket();
        ticket.setProblemDescription(dto.problemDescription());
        ticket.setDevice(toDeviceInfo(dto.device()));
        ticket.setCustomer(customer);
        ticket = ticketRepository.save(ticket);

        recordStatusChange(ticket, null, TicketStatus.PENDING, createdBy, null);

        return toDto(ticket);
    }

    public TicketDto assignEmployee(UUID publicId, Long employeeId, Long assignedByEmployeeId) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee assignedBy = employeeRepository.findById(assignedByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + assignedByEmployeeId + " no encontrado"));

        Employee previous = ticket.getAssignedEmployee();
        Employee next = null;
        if (employeeId != null) {
            next = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " no encontrado"));
        }
        ticket.setAssignedEmployee(next);
        TicketDto dto = toDto(ticketRepository.save(ticket));

        Long previousId = previous != null ? previous.getId() : null;
        Long nextId = next != null ? next.getId() : null;
        if (!Objects.equals(previousId, nextId)) {
            recordAssignmentChange(ticket, previous, next, assignedBy);
        }
        return dto;
    }

    public TicketDto changeStatus(UUID publicId, TicketStatus newStatus, Long changedByEmployeeId, String note) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee changedBy = employeeRepository.findById(changedByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + changedByEmployeeId + " no encontrado"));

        Set<TicketStatus> allowedNext = ALLOWED_TRANSITIONS.get(ticket.getStatus());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStatusTransitionException(ticket.getStatus(), newStatus);
        }

        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);

        recordStatusChange(ticket, previous, newStatus, changedBy, note);

        return toDto(ticket);
    }

    public void addNote(UUID publicId, Long authorEmployeeId, String text) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee author = employeeRepository.findById(authorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + authorEmployeeId + " no encontrado"));

        TicketNote note = new TicketNote();
        note.setTicket(ticket);
        note.setAuthor(author);
        note.setText(text);
        noteRepository.save(note);
    }

    public List<TimelineEntryDto> getTimeline(UUID publicId) {
        Ticket ticket = findTicketOrThrow(publicId);

        List<TimelineEntryDto> entries = new ArrayList<>();
        statusChangeRepository.findByTicketOrderByChangedAtAsc(ticket).forEach(c ->
                entries.add(new StatusChangeEntryDto(
                        c.getChangedAt(), c.getChangedBy().getName(),
                        c.getPreviousStatus(), c.getNewStatus(), c.getNote())));
        noteRepository.findByTicketOrderByCreatedAtAsc(ticket).forEach(n ->
                entries.add(new NoteEntryDto(n.getCreatedAt(), n.getAuthor().getName(), n.getText())));
        assignmentChangeRepository.findByTicketOrderByChangedAtAsc(ticket).forEach(c ->
                entries.add(new AssignmentChangeEntryDto(
                        c.getChangedAt(), c.getChangedBy().getName(),
                        c.getPreviousEmployee() != null ? c.getPreviousEmployee().getName() : null,
                        c.getNewEmployee() != null ? c.getNewEmployee().getName() : null)));

        entries.sort(Comparator.comparing(TimelineEntryDto::occurredAt));
        return entries;
    }

    private void recordStatusChange(Ticket ticket, TicketStatus previous, TicketStatus next, Employee changedBy, String note) {
        TicketStatusChange change = new TicketStatusChange();
        change.setTicket(ticket);
        change.setPreviousStatus(previous);
        change.setNewStatus(next);
        change.setChangedBy(changedBy);
        change.setNote(note);
        statusChangeRepository.save(change);
    }

    private void recordAssignmentChange(Ticket ticket, Employee previous, Employee next, Employee changedBy) {
        TicketAssignmentChange change = new TicketAssignmentChange();
        change.setTicket(ticket);
        change.setPreviousEmployee(previous);
        change.setNewEmployee(next);
        change.setChangedBy(changedBy);
        assignmentChangeRepository.save(change);
    }

    private Ticket findTicketOrThrow(UUID publicId) {
        return ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + publicId + " no encontrado"));
    }

    private TicketDto toDto(Ticket ticket) {
        return new TicketDto(
                ticket.getPublicId(),
                ticket.getProblemDescription(),
                toDeviceDto(ticket.getDevice()),
                ticket.getStatus(),
                ticket.getCustomer().getId(),
                ticket.getCustomer().getName(),
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getName() : null,
                ticket.getCreatedAt(),
                ALLOWED_TRANSITIONS.get(ticket.getStatus()).stream().sorted().toList()
        );
    }

    private DeviceInfo toDeviceInfo(DeviceDto dto) {
        return new DeviceInfo(dto.type(), dto.brand(), dto.model(), dto.identifier());
    }

    private DeviceDto toDeviceDto(DeviceInfo device) {
        return new DeviceDto(device.getType(), device.getBrand(), device.getModel(), device.getIdentifier());
    }
}
