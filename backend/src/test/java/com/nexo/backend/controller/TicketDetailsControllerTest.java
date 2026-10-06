package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.DeviceDto;
import com.nexo.backend.dto.TicketDto;
import com.nexo.backend.exception.TicketClosedException;
import com.nexo.backend.model.DeviceType;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.TicketPartService;
import com.nexo.backend.service.TicketService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// MockMvc tests for the details endpoint. Written before the implementation.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwt;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private TicketPartService partService;
    @MockitoBean
    private EmployeeRepository employeeRepository;

    private Cookie as(long id, EmployeeRole role) {
        when(employeeRepository.findById(id)).thenReturn(Optional.of(TestData.employee(id, role)));
        return new Cookie("nexo_token", jwt.generateToken(id, "t@nexo.com", role.name()));
    }

    private static TicketDto ticketDto(UUID publicId, String problemDescription) {
        return new TicketDto(publicId, problemDescription,
                new DeviceDto(DeviceType.LAPTOP, "Lenovo", "T14", "SN1"),
                TicketStatus.PENDING, 1L, "Juan", null, null, Instant.now(),
                List.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED), false);
    }

    @Test
    void updateDetails_withoutCookie_returns401() throws Exception {
        mockMvc.perform(patch("/api/tickets/{id}/details", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"New description\"}"))
                .andExpect(status().isUnauthorized());
        verify(ticketService, never()).updateDetails(any(), any());
    }

    @Test
    void updateDetails_blankDescription_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/{id}/details", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verify(ticketService, never()).updateDetails(any(), any());
    }

    @Test
    void updateDetails_missingField_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/{id}/details", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(ticketService, never()).updateDetails(any(), any());
    }

    @Test
    void updateDetails_tooLongDescription_returns400() throws Exception {
        String tooLong = "a".repeat(501);

        mockMvc.perform(patch("/api/tickets/{id}/details", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
        verify(ticketService, never()).updateDetails(any(), any());
    }

    @Test
    void updateDetails_closedTicket_returns409() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.updateDetails(eq(publicId), any())).thenThrow(new TicketClosedException());

        mockMvc.perform(patch("/api/tickets/{id}/details", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"New description\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateDetails_valid_returnsUpdatedTicket() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.updateDetails(eq(publicId), eq("New description")))
                .thenReturn(ticketDto(publicId, "New description"));

        mockMvc.perform(patch("/api/tickets/{id}/details", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"New description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemDescription").value("New description"));

        verify(ticketService).updateDetails(eq(publicId), eq("New description"));
    }

    @Test
    void updateDetails_receptionRole_returnsOk() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.updateDetails(eq(publicId), eq("New description")))
                .thenReturn(ticketDto(publicId, "New description"));

        mockMvc.perform(patch("/api/tickets/{id}/details", publicId)
                        .cookie(as(7L, EmployeeRole.RECEPTION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"New description\"}"))
                .andExpect(status().isOk());

        verify(ticketService).updateDetails(eq(publicId), eq("New description"));
    }
}
