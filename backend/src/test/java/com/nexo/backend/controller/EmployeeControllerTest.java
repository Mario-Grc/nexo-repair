package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.EmployeeOptionDto;
import com.nexo.backend.exception.DuplicateEmailException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.SelfModificationException;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.EmployeeService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

    private static final String CREATE_BODY = """
            {"name":"Beto","email":"beto@nexo.com",
             "password":"secret123","role":"TECHNICIAN"}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwt;

    @MockitoBean
    private EmployeeService employeeService;
    @MockitoBean
    private EmployeeRepository employeeRepository;

    private Cookie as(long id, EmployeeRole role) {
        when(employeeRepository.findById(id)).thenReturn(Optional.of(TestData.employee(id, role)));
        return new Cookie("nexo_token", jwt.generateToken(id, "t@nexo.com", role.name()));
    }

    private static EmployeeDto employeeDto() {
        return new EmployeeDto(2L, "Beto", "beto@nexo.com", EmployeeRole.TECHNICIAN, true);
    }

    @Test
    void create_asTechnician_returns403() throws Exception {
        mockMvc.perform(post("/api/employees").cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_asTechnician_returns403() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/role", 5L).cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateActive_asTechnician_returns403() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/active", 5L).cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetPassword_asTechnician_returns403() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/password", 5L).cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"newsecret123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProfile_asTechnician_returns403() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/profile", 5L).cookie(as(7L, EmployeeRole.TECHNICIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Beto\",\"email\":\"beto@nexo.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns200() throws Exception {
        when(employeeService.createEmployee(any())).thenReturn(employeeDto());

        mockMvc.perform(post("/api/employees").cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/employees").cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"beto@nexo.com",
                                 "password":"secret123","role":"TECHNICIAN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("name")));
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/employees").cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Beto","email":"not-an-email",
                                 "password":"secret123","role":"TECHNICIAN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("email")));
    }

    @Test
    void create_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/employees").cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Beto","email":"beto@nexo.com",
                                 "password":"short","role":"TECHNICIAN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("password")));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(employeeService.createEmployee(any())).thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/api/employees").cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateRole_selfModification_returns400() throws Exception {
        when(employeeService.updateRole(eq(5L), eq(EmployeeRole.ADMIN), eq(5L)))
                .thenThrow(new SelfModificationException("You cannot change your own role"));

        mockMvc.perform(patch("/api/employees/{id}/role", 5L).cookie(as(5L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_missingEmployee_returns404() throws Exception {
        when(employeeService.updateRole(eq(99L), eq(EmployeeRole.ADMIN), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Employee 99 not found"));

        mockMvc.perform(patch("/api/employees/{id}/role", 99L).cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_asTechnician_returns200() throws Exception {
        when(employeeService.getEmployees(null, null)).thenReturn(List.of(employeeDto()));

        mockMvc.perform(get("/api/employees").cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isOk());
    }

    @Test
    void list_asAdmin_returns200() throws Exception {
        when(employeeService.getEmployees(null, null)).thenReturn(List.of(employeeDto()));

        mockMvc.perform(get("/api/employees").cookie(as(1L, EmployeeRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void list_asReception_returns200() throws Exception {
        when(employeeService.getEmployees(null, null)).thenReturn(List.of(employeeDto()));

        mockMvc.perform(get("/api/employees").cookie(as(8L, EmployeeRole.RECEPTION)))
                .andExpect(status().isOk());
    }

    @Test
    void assignable_asTechnician_returnsOnlyIdAndName() throws Exception {
        when(employeeService.getAssignable()).thenReturn(List.of(new EmployeeOptionDto(2L, "Beto")));

        mockMvc.perform(get("/api/employees/assignable").cookie(as(7L, EmployeeRole.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Beto"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].role").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void assignable_asReception_returns200() throws Exception {
        when(employeeService.getAssignable()).thenReturn(List.of(new EmployeeOptionDto(2L, "Beto")));

        mockMvc.perform(get("/api/employees/assignable").cookie(as(7L, EmployeeRole.RECEPTION)))
                .andExpect(status().isOk());
    }

    @Test
    void updateRole_usesActorIdFromToken() throws Exception {
        when(employeeService.updateRole(eq(5L), eq(EmployeeRole.ADMIN), eq(1L))).thenReturn(employeeDto());

        mockMvc.perform(patch("/api/employees/{id}/role", 5L).cookie(as(1L, EmployeeRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());

        verify(employeeService).updateRole(eq(5L), eq(EmployeeRole.ADMIN), eq(1L));
    }
}
