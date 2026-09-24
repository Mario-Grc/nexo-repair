package com.nexo.backend.controller;

import com.nexo.backend.dto.*;
import com.nexo.backend.service.TicketService;
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
    public List<TicketDto> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{publicId}")
    public TicketDto getTicket(@PathVariable UUID publicId) {
        return ticketService.getTicket(publicId);
    }

    @PostMapping
    public TicketDto createTicket(@RequestBody CreateTicketDto dto) {
        return ticketService.createTicket(dto);
    }

    @PatchMapping("/{publicId}/assign")
    public TicketDto assignEmployee(@PathVariable UUID publicId, @RequestBody AssignEmployeeDto dto) {
        return ticketService.assignEmployee(publicId, dto.employeeId(), dto.assignedByEmployeeId());
    }

    @PatchMapping("/{publicId}/status")
    public TicketDto changeStatus(@PathVariable UUID publicId, @RequestBody ChangeStatusDto dto) {
        return ticketService.changeStatus(publicId, dto.newStatus(), dto.changedByEmployeeId(), dto.note());
    }

    @PostMapping("/{publicId}/notes")
    public void addNote(@PathVariable UUID publicId, @RequestBody AddNoteDto dto) {
        ticketService.addNote(publicId, dto.authorEmployeeId(), dto.text());
    }

    @GetMapping("/{publicId}/timeline")
    public List<TimelineEntryDto> getTimeline(@PathVariable UUID publicId) {
        return ticketService.getTimeline(publicId);
    }
}
