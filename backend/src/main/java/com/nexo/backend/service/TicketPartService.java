package com.nexo.backend.service;

import com.nexo.backend.dto.AddPartDto;
import com.nexo.backend.dto.PartsDto;
import com.nexo.backend.dto.TicketPartDto;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.TicketClosedException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketPart;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.repository.TicketPartRepository;
import com.nexo.backend.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Parts live in their own service. TicketService already has enough collaborators.
@Service
public class TicketPartService {

    private final TicketRepository ticketRepository;
    private final TicketPartRepository partRepository;
    private final EmployeeRepository employeeRepository;

    public TicketPartService(TicketRepository ticketRepository,
                             TicketPartRepository partRepository,
                             EmployeeRepository employeeRepository) {
        this.ticketRepository = ticketRepository;
        this.partRepository = partRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public TicketPartDto add(UUID publicId, AddPartDto dto, Long employeeId) {
        Ticket ticket = findTicket(publicId);
        if (ticket.getStatus().isClosed()) {
            throw new TicketClosedException();
        }
        Employee author = findEmployee(employeeId);
        TicketPart part = new TicketPart(ticket, dto.description().trim(), dto.quantity(), dto.unitPrice(), author);
        return toDto(partRepository.save(part));
    }

    @Transactional
    public void remove(UUID publicId, Long partId, Long employeeId) {
        Ticket ticket = findTicket(publicId);
        if (ticket.getStatus().isClosed()) {
            throw new TicketClosedException();
        }
        TicketPart part = partRepository.findById(partId)
                // The part must belong to this ticket. Without this check anyone
                // knowing a numeric part id could remove it through another ticket.
                .filter(p -> p.getTicket().getId().equals(ticket.getId()))
                .filter(p -> !p.isRemoved())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket part " + partId + " not found"));
        // Managed entity, so the change is saved when the transaction closes.
        part.markRemoved(findEmployee(employeeId), Instant.now());
    }

    @Transactional(readOnly = true)
    public PartsDto list(UUID publicId) {
        Ticket ticket = findTicket(publicId);
        List<TicketPartDto> items = partRepository.findByTicketAndRemovedAtIsNullOrderByAddedAtAsc(ticket)
                .stream()
                .map(this::toDto)
                .toList();
        BigDecimal total = null;
        BigDecimal sum = BigDecimal.ZERO;
        boolean hasPricedLine = false;
        for (TicketPartDto item : items) {
            if (item.lineTotal() != null) {
                sum = sum.add(item.lineTotal());
                hasPricedLine = true;
            }
        }
        if (hasPricedLine) {
            total = sum;
        }
        return new PartsDto(items, total);
    }

    private TicketPartDto toDto(TicketPart part) {
        return new TicketPartDto(
                part.getId(),
                part.getDescription(),
                part.getQuantity(),
                part.getUnitPrice(),
                lineTotal(part.getUnitPrice(), part.getQuantity()),
                part.getAddedBy().getName(),
                part.getAddedAt());
    }

    private BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private Ticket findTicket(UUID publicId) {
        return ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + publicId + " not found"));
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " not found"));
    }
}
