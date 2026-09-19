package com.nexo.backend.controller;

import com.nexo.backend.dto.*;
import com.nexo.backend.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public TicketDto createTicket(@RequestBody CreateTicketDto dto) {
        return ticketService.createTicket(dto);
    }

    @PatchMapping("/{id}/assign")
    public TicketDto assignEmployee(@PathVariable Long id, @RequestBody AssignEmployeeDto dto) {
        return ticketService.assignEmployee(id, dto.employeeId());
    }

    @PatchMapping("/{id}/status")
    public TicketDto changeStatus(@PathVariable Long id, @RequestBody ChangeStatusDto dto) {
        return ticketService.changeStatus(id, dto.newStatus());
    }
}
