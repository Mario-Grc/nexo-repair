package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.exception.InvalidCredentialsException;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwt;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private EmployeeRepository employeeRepository;

    private Cookie as(long id, EmployeeRole role) {
        when(employeeRepository.findById(id)).thenReturn(Optional.of(TestData.employee(id, role)));
        return new Cookie("nexo_token", jwt.generateToken(id, "t@nexo.com", role.name()));
    }

    @Test
    void login_valid_setsSessionCookie() throws Exception {
        EmployeeDto employee = new EmployeeDto(1L, "Ana", "ana@nexo.com", EmployeeRole.ADMIN, true);
        String token = jwt.generateToken(1L, "ana@nexo.com", "ADMIN");
        when(authService.login(eq("ana@nexo.com"), eq("secret123")))
                .thenReturn(new AuthService.LoginResult(employee, token));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@nexo.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(header().string("Set-Cookie", containsString("nexo_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=3600")));
    }

    @Test
    void login_invalid_returns401WithoutCookie() throws Exception {
        when(authService.login(eq("ana@nexo.com"), eq("wrong")))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@nexo.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void logout_authenticated_clearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(as(1L, EmployeeRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("nexo_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void me_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
