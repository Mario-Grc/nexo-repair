package com.nexo.backend.service;

import com.nexo.backend.dto.*;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.exception.NoteRequiredException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.*;
import com.nexo.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TicketService {

    // Ordered lists. The first element drives the primary action button,
    // so the happy path comes first and CANCELLED last.
    private static final Map<TicketStatus, List<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.PENDING, List.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, List.of(TicketStatus.COMPLETED, TicketStatus.WAITING_FOR_PARTS, TicketStatus.CANCELLED),
            TicketStatus.WAITING_FOR_PARTS, List.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.COMPLETED, List.of(TicketStatus.DELIVERED),
            TicketStatus.DELIVERED, List.of(),
            TicketStatus.CANCELLED, List.of()
    );

    private static final Set<TicketStatus> NOTE_REQUIRED = EnumSet.of(TicketStatus.COMPLETED, TicketStatus.CANCELLED);

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketStatusChangeRepository statusChangeRepository;
    private final TicketNoteRepository noteRepository;
    private final TicketAssignmentChangeRepository assignmentChangeRepository;
    private final TicketPartRepository partRepository;

    public TicketService(TicketRepository ticketRepository,
                         CustomerRepository customerRepository,
                         EmployeeRepository employeeRepository,
                         TicketStatusChangeRepository statusChangeRepository,
                         TicketNoteRepository noteRepository,
                         TicketAssignmentChangeRepository assignmentChangeRepository,
                         TicketPartRepository partRepository) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.statusChangeRepository = statusChangeRepository;
        this.noteRepository = noteRepository;
        this.assignmentChangeRepository = assignmentChangeRepository;
        this.partRepository = partRepository;
    }

    public List<TicketDto> getAllTickets(Long customerId) {
        if (customerId != null) {
            return ticketRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
        }
        return ticketRepository.findAll().stream().map(this::toDto).toList();
    }

    public TicketDto getTicket(UUID publicId) {
        return toDto(findTicketOrThrow(publicId));
    }

    @Transactional
    public TicketDto createTicket(CreateTicketDto dto, Long createdByEmployeeId) {
        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + dto.customerId() + " not found"));
        Employee createdBy = employeeRepository.findById(createdByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + createdByEmployeeId + " not found"));

        Ticket ticket = new Ticket();
        ticket.setProblemDescription(dto.problemDescription());
        ticket.setDevice(toDeviceInfo(dto.device()));
        ticket.setCustomer(customer);
        ticket = ticketRepository.save(ticket);

        recordStatusChange(ticket, null, TicketStatus.PENDING, createdBy, null);

        return toDto(ticket);
    }

    @Transactional
    public TicketDto assignEmployee(UUID publicId, Long employeeId, Long assignedByEmployeeId) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee assignedBy = employeeRepository.findById(assignedByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + assignedByEmployeeId + " not found"));

        Employee previous = ticket.getAssignedEmployee();
        Employee next = null;
        if (employeeId != null) {
            next = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " not found"));
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

    @Transactional
    public TicketDto changeStatus(UUID publicId, TicketStatus newStatus, Long changedByEmployeeId, String note) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee changedBy = employeeRepository.findById(changedByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + changedByEmployeeId + " not found"));

        List<TicketStatus> allowedNext = ALLOWED_TRANSITIONS.get(ticket.getStatus());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStatusTransitionException(ticket.getStatus(), newStatus);
        }

        // The transition comes first. An unknown transition stays invalid
        // even when the note is also missing.
        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        if (cleanNote == null && NOTE_REQUIRED.contains(newStatus)) {
            throw new NoteRequiredException(newStatus);
        }

        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);

        recordStatusChange(ticket, previous, newStatus, changedBy, cleanNote);

        return toDto(ticket);
    }

    public void addNote(UUID publicId, Long authorEmployeeId, String text) {
        Ticket ticket = findTicketOrThrow(publicId);
        Employee author = employeeRepository.findById(authorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + authorEmployeeId + " not found"));

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
        partRepository.findByTicketOrderByAddedAtAsc(ticket).forEach(p -> {
            entries.add(new PartEntryDto(p.getAddedAt(), p.getAddedBy().getName(),
                    "PART_ADDED", p.getDescription(), p.getQuantity()));
            if (p.isRemoved()) {
                entries.add(new PartEntryDto(p.getRemovedAt(), p.getRemovedBy().getName(),
                        "PART_REMOVED", p.getDescription(), p.getQuantity()));
            }
        });

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
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + publicId + " not found"));
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
                List.copyOf(ALLOWED_TRANSITIONS.get(ticket.getStatus())),
                ticket.getStatus().isClosed()
        );
    }

    private DeviceInfo toDeviceInfo(DeviceDto dto) {
        return new DeviceInfo(dto.type(), dto.brand(), dto.model(), dto.identifier());
    }

    private DeviceDto toDeviceDto(DeviceInfo device) {
        return new DeviceDto(device.getType(), device.getBrand(), device.getModel(), device.getIdentifier());
    }
}
