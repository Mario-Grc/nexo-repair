package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.PartsDto;
import com.nexo.backend.dto.TicketPartDto;
import com.nexo.backend.exception.NoteRequiredException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.TicketClosedException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// MockMvc tests for the parts endpoints and the new error mappings.
// Written before the implementation.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketPartsControllerTest {

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

    @Test
    void addPart_quantityZero_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets/{id}/parts", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake pads\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPart_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets/{id}/parts", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"   \",\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPart_negativePrice_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets/{id}/parts", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake pads\",\"quantity\":1,\"unitPrice\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPart_closedTicket_returns409() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(partService.add(eq(publicId), any(), eq(7L))).thenThrow(new TicketClosedException());

        mockMvc.perform(post("/api/tickets/{id}/parts", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake pads\",\"quantity\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void addPart_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/tickets/{id}/parts", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake pads\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addPart_usesEmployeeIdFromToken() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(partService.add(eq(publicId), any(), eq(7L)))
                .thenReturn(new TicketPartDto(1L, "Brake pads", 1, null, null, "Ana", Instant.now()));

        mockMvc.perform(post("/api/tickets/{id}/parts", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake pads\",\"quantity\":1}"))
                .andExpect(status().isOk());

        verify(partService).add(eq(publicId), any(), eq(7L));
    }

    @Test
    void removePart_partFromOtherTicket_returns404() throws Exception {
        UUID otherId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Ticket part 5 not found"))
                .when(partService).remove(eq(otherId), eq(5L), eq(7L));

        mockMvc.perform(delete("/api/tickets/{id}/parts/{partId}", otherId, 5L)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removePart_closedTicket_returns409() throws Exception {
        UUID publicId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new TicketClosedException())
                .when(partService).remove(eq(publicId), eq(5L), eq(7L));

        mockMvc.perform(delete("/api/tickets/{id}/parts/{partId}", publicId, 5L)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isConflict());
    }

    @Test
    void removePart_withoutCookie_returns401() throws Exception {
        mockMvc.perform(delete("/api/tickets/{id}/parts/{partId}", UUID.randomUUID(), 5L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removePart_usesEmployeeIdFromToken() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(delete("/api/tickets/{id}/parts/{partId}", publicId, 5L)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isOk());

        verify(partService).remove(eq(publicId), eq(5L), eq(7L));
    }

    @Test
    void getParts_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}/parts", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getParts_valid_returnsItemsAndTotal() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(partService.list(publicId)).thenReturn(new PartsDto(
                List.of(new TicketPartDto(1L, "Brake pads", 2, new BigDecimal("10.50"),
                        new BigDecimal("21.00"), "Ana", Instant.now())),
                new BigDecimal("21.00")));

        mockMvc.perform(get("/api/tickets/{id}/parts", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Brake pads"))
                .andExpect(jsonPath("$.total").value(21.00));
    }

    @Test
    void changeStatus_completedWithoutNote_returns400() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.changeStatus(eq(publicId), eq(TicketStatus.COMPLETED), eq(7L), any()))
                .thenThrow(new NoteRequiredException(TicketStatus.COMPLETED));

        mockMvc.perform(patch("/api/tickets/{id}/status", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
    }
}
