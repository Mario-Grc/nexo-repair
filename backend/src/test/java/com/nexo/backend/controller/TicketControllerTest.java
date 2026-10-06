package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.AssignmentChangeEntryDto;
import com.nexo.backend.dto.DeviceDto;
import com.nexo.backend.dto.NoteEntryDto;
import com.nexo.backend.dto.StatusChangeEntryDto;
import com.nexo.backend.dto.TicketDto;
import com.nexo.backend.dto.CreateTicketDto;
import com.nexo.backend.exception.InvalidStatusTransitionException;
import com.nexo.backend.model.DeviceType;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.TicketStatus;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.TicketService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    private static final String CREATE_BODY = """
            {"problemDescription":"Screen stays black",
             "device":{"type":"LAPTOP","brand":"Lenovo","model":"T14","identifier":"SN1"},
             "customerId":1}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwt;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Value("${nexo.jwt.secret}")
    private String jwtSecret;

    private Cookie as(long id, EmployeeRole role) {
        when(employeeRepository.findById(id)).thenReturn(Optional.of(TestData.employee(id, role)));
        return new Cookie("nexo_token", jwt.generateToken(id, "t@nexo.com", role.name()));
    }

    private Cookie expiredAs(long id, EmployeeRole role) {
        String token = new JwtService(jwtSecret, -1).generateToken(id, "t@nexo.com", role.name());
        return new Cookie("nexo_token", token);
    }

    private static TicketDto ticketDto(UUID publicId) {
        return new TicketDto(publicId, "Screen stays black",
                new DeviceDto(DeviceType.LAPTOP, "Lenovo", "T14", "SN1"),
                TicketStatus.PENDING, 1L, "Juan", null, null, Instant.now(),
                List.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED), false);
    }

    @Test
    void list_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void list_withTamperedCookie_returns401() throws Exception {
        Cookie tampered = new Cookie("nexo_token",
                jwt.generateToken(7L, "t@nexo.com", "TECHNICIAN") + "tampered");

        mockMvc.perform(get("/api/tickets").cookie(tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void list_withExpiredCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets").cookie(expiredAs(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_missingDevice_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets").cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"Screen stays black\",\"customerId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingCustomerId_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets").cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemDescription":"Screen stays black",
                                 "device":{"type":"LAPTOP"}}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets").cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemDescription":"",
                                 "device":{"type":"LAPTOP"},"customerId":1}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_nullNewStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/{id}/status", UUID.randomUUID())
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"oops\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_invalidTransition_returns400() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.changeStatus(eq(publicId), eq(TicketStatus.DELIVERED), eq(7L), any()))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.PENDING, TicketStatus.DELIVERED));

        mockMvc.perform(patch("/api/tickets/{id}/status", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timeline_valid_returnsTypedEntries() throws Exception {
        UUID publicId = UUID.randomUUID();
        Instant now = Instant.now();
        when(ticketService.getTimeline(publicId)).thenReturn(List.of(
                new StatusChangeEntryDto(now, "Ana", TicketStatus.PENDING, TicketStatus.IN_PROGRESS, null),
                new NoteEntryDto(now, "Ana", "Waiting on parts"),
                new AssignmentChangeEntryDto(now, "Ana", null, "Beto")));

        mockMvc.perform(get("/api/tickets/{id}/timeline", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$[1].type").value("NOTE"))
                .andExpect(jsonPath("$[2].type").value("ASSIGNMENT_CHANGE"));
    }

    @Test
    void create_usesEmployeeIdFromToken() throws Exception {
        when(ticketService.createTicket(any(CreateTicketDto.class), eq(7L)))
                .thenReturn(ticketDto(UUID.randomUUID()));

        mockMvc.perform(post("/api/tickets").cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk());

        verify(ticketService).createTicket(any(CreateTicketDto.class), eq(7L));
    }

    @Test
    void changeStatus_usesEmployeeIdFromToken() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.changeStatus(eq(publicId), eq(TicketStatus.IN_PROGRESS), eq(7L), eq("Started")))
                .thenReturn(ticketDto(publicId));

        mockMvc.perform(patch("/api/tickets/{id}/status", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\",\"note\":\"Started\"}"))
                .andExpect(status().isOk());

        verify(ticketService).changeStatus(eq(publicId), eq(TicketStatus.IN_PROGRESS), eq(7L), eq("Started"));
    }

    @Test
    void addNote_usesEmployeeIdFromToken() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(post("/api/tickets/{id}/notes", publicId)
                        .cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hello\"}"))
                .andExpect(status().isOk());

        verify(ticketService).addNote(eq(publicId), eq(7L), eq("hello"));
    }

    @Test
    void assign_usesEmployeeIdFromToken() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(ticketService.assignEmployee(eq(publicId), eq(3L), eq(7L))).thenReturn(ticketDto(publicId));

        mockMvc.perform(patch("/api/tickets/{id}/assign", publicId)
                        .cookie(as(7L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":3}"))
                .andExpect(status().isOk());

        verify(ticketService).assignEmployee(eq(publicId), eq(3L), eq(7L));
    }
}
