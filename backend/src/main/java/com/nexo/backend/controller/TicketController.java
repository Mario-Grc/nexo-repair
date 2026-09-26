package com.nexo.backend.controller;

import com.nexo.backend.dto.*;
import com.nexo.backend.security.EmployeePrincipal;
import com.nexo.backend.service.TicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketDto> getAllTickets(@RequestParam(required = false) Long customerId) {
        return ticketService.getAllTickets(customerId);
    }

    @GetMapping("/{publicId}")
    public TicketDto getTicket(@PathVariable UUID publicId) {
        return ticketService.getTicket(publicId);
    }

    @PostMapping
    public TicketDto createTicket(@RequestBody CreateTicketDto dto, @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.createTicket(dto, principal.employeeId());
    }

    @PatchMapping("/{publicId}/assign")
    public TicketDto assignEmployee(@PathVariable UUID publicId, @RequestBody AssignEmployeeDto dto,
                                    @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.assignEmployee(publicId, dto.employeeId(), principal.employeeId());
    }

    @PatchMapping("/{publicId}/status")
    public TicketDto changeStatus(@PathVariable UUID publicId, @RequestBody ChangeStatusDto dto,
                                  @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.changeStatus(publicId, dto.newStatus(), principal.employeeId(), dto.note());
    }

    @PostMapping("/{publicId}/notes")
    public void addNote(@PathVariable UUID publicId, @RequestBody AddNoteDto dto,
                        @AuthenticationPrincipal EmployeePrincipal principal) {
        ticketService.addNote(publicId, principal.employeeId(), dto.text());
    }

    @GetMapping("/{publicId}/timeline")
    public List<TimelineEntryDto> getTimeline(@PathVariable UUID publicId) {
        return ticketService.getTimeline(publicId);
    }
}
