package com.nexo.backend.controller;

import com.nexo.backend.dto.*;
import com.nexo.backend.security.EmployeePrincipal;
import com.nexo.backend.service.TicketPartService;
import com.nexo.backend.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketPartService partService;

    public TicketController(TicketService ticketService, TicketPartService partService) {
        this.ticketService = ticketService;
        this.partService = partService;
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
    public TicketDto createTicket(@Valid @RequestBody CreateTicketDto dto, @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.createTicket(dto, principal.employeeId());
    }

    @PatchMapping("/{publicId}/assign")
    public TicketDto assignEmployee(@PathVariable UUID publicId, @Valid @RequestBody AssignEmployeeDto dto,
                                    @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.assignEmployee(publicId, dto.employeeId(), principal.employeeId());
    }

    @PatchMapping("/{publicId}/status")
    public TicketDto changeStatus(@PathVariable UUID publicId, @Valid @RequestBody ChangeStatusDto dto,
                                  @AuthenticationPrincipal EmployeePrincipal principal) {
        return ticketService.changeStatus(publicId, dto.newStatus(), principal.employeeId(), dto.note());
    }

    @PostMapping("/{publicId}/notes")
    public void addNote(@PathVariable UUID publicId, @Valid @RequestBody AddNoteDto dto,
                        @AuthenticationPrincipal EmployeePrincipal principal) {
        ticketService.addNote(publicId, principal.employeeId(), dto.text());
    }

    // No principal needed. Description edits carry no audit trail by design.
    // Auth is still enforced by SecurityConfig anyRequest().authenticated().
    @PatchMapping("/{publicId}/details")
    public TicketDto updateDetails(@PathVariable UUID publicId,
                                   @Valid @RequestBody UpdateTicketDetailsDto dto) {
        return ticketService.updateDetails(publicId, dto.problemDescription());
    }

    @GetMapping("/{publicId}/timeline")
    public List<TimelineEntryDto> getTimeline(@PathVariable UUID publicId) {
        return ticketService.getTimeline(publicId);
    }

    @GetMapping("/{publicId}/parts")
    public PartsDto getParts(@PathVariable UUID publicId) {
        return partService.list(publicId);
    }

    @PostMapping("/{publicId}/parts")
    public TicketPartDto addPart(@PathVariable UUID publicId, @Valid @RequestBody AddPartDto dto,
                                 @AuthenticationPrincipal EmployeePrincipal principal) {
        return partService.add(publicId, dto, principal.employeeId());
    }

    @DeleteMapping("/{publicId}/parts/{partId}")
    public void removePart(@PathVariable UUID publicId, @PathVariable Long partId,
                           @AuthenticationPrincipal EmployeePrincipal principal) {
        partService.remove(publicId, partId, principal.employeeId());
    }
}
