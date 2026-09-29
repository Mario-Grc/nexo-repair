package com.nexo.backend.controller;

import com.nexo.backend.TestData;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.AuthService;
import com.nexo.backend.service.CustomerService;
import com.nexo.backend.service.EmployeeService;
import com.nexo.backend.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Real HTTP through the container. MockMvc skips error dispatch,
// so only these tests prove that 4xx responses survive /error as themselves.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class HttpRealSecurityTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JwtService jwt;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private EmployeeService employeeService;
    @MockitoBean
    private CustomerService customerService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private EmployeeRepository employeeRepository;

    private HttpHeaders cookieHeaders(long id, EmployeeRole role) {
        when(employeeRepository.findById(id)).thenReturn(Optional.of(TestData.employee(id, role)));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "nexo_token=" + jwt.generateToken(id, "t@nexo.com", role.name()));
        return headers;
    }

    @Test
    void listEmployees_asTechnician_returns200() {
        ResponseEntity<String> response = rest.exchange("/api/employees", HttpMethod.GET,
                new HttpEntity<>(cookieHeaders(7L, EmployeeRole.TECHNICIAN)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createEmployee_asTechnician_returns403() {
        HttpHeaders headers = cookieHeaders(7L, EmployeeRole.TECHNICIAN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"name":"Beto","email":"beto@nexo.com",
                 "password":"secret123","role":"TECHNICIAN"}""";

        ResponseEntity<String> response = rest.exchange("/api/employees", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void unknownRoute_withCookie_returns404() {
        ResponseEntity<String> response = rest.exchange("/api/does-not-exist", HttpMethod.GET,
                new HttpEntity<>(cookieHeaders(7L, EmployeeRole.TECHNICIAN)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createTicket_malformedJson_returns400() {
        HttpHeaders headers = cookieHeaders(7L, EmployeeRole.TECHNICIAN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = rest.exchange("/api/tickets", HttpMethod.POST,
                new HttpEntity<>("{broken", headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void unknownRoute_withoutCookie_returns401() {
        ResponseEntity<String> response = rest.getForEntity("/api/does-not-exist", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
