package com.nexo.backend.service;

import com.nexo.backend.dto.*;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.*;
import com.nexo.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.PENDING, Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.WAITING_FOR_PARTS, TicketStatus.COMPLETED),
            TicketStatus.WAITING_FOR_PARTS, Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.COMPLETED, Set.of(TicketStatus.DELIVERED),
            TicketStatus.DELIVERED, Set.of()
    );

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public TicketService(TicketRepository ticketRepository,
                         CustomerRepository customerRepository,
                         EmployeeRepository employeeRepository) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<TicketDto> getAllTickets() {
        return ticketRepository.findAll().stream().map(this::toDto).toList();
    }

    public TicketDto createTicket(CreateTicketDto dto) {
        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + dto.customerId() + " no encontrado"));

        Ticket ticket = new Ticket();
        ticket.setDescription(dto.description());
        ticket.setDeviceInfo(dto.deviceInfo());
        ticket.setCustomer(customer);

        return toDto(ticketRepository.save(ticket));
    }

    public TicketDto assignEmployee(Long ticketId, Long employeeId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " no encontrado"));

        ticket.setEmployee(employee);
        return toDto(ticketRepository.save(ticket));
    }

    public TicketDto changeStatus(Long ticketId, TicketStatus newStatus) {
        Ticket ticket = findTicketOrThrow(ticketId);

        Set<TicketStatus> allowedNext = ALLOWED_TRANSITIONS.get(ticket.getStatus());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStatusTransitionException(ticket.getStatus(), newStatus);
        }

        ticket.setStatus(newStatus);
        return toDto(ticketRepository.save(ticket));
    }

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + id + " no encontrado"));
    }

    private TicketDto toDto(Ticket ticket) {
        return new TicketDto(
                ticket.getId(),
                ticket.getDescription(),
                ticket.getDeviceInfo(),
                ticket.getStatus(),
                ticket.getCustomer().getId(),
                ticket.getEmployee() != null ? ticket.getEmployee().getId() : null
        );
    }
}
