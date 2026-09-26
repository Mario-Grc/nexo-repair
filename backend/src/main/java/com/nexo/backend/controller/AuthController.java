package com.nexo.backend.controller;

import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.LoginDto;
import com.nexo.backend.exception.InvalidCredentialsException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.EmployeePrincipal;
import com.nexo.backend.security.JwtService;
import com.nexo.backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(EmployeeRepository employeeRepository, EmployeeService employeeService,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public EmployeeDto login(@Valid @RequestBody LoginDto dto, HttpServletResponse response) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(dto.email().trim())
                .filter(Employee::isActive)
                .filter(e -> passwordEncoder.matches(dto.password(), e.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(employee.getId(), employee.getEmail(), employee.getRole().name());
        response.addHeader("Set-Cookie", sessionCookie(token, jwtService.getExpirationHours() * 3600).toString());

        return employeeService.toDto(employee);
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", sessionCookie("", 0).toString());
    }

    @GetMapping("/me")
    public EmployeeDto me(@AuthenticationPrincipal EmployeePrincipal principal) {
        Employee employee = employeeRepository.findById(principal.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return employeeService.toDto(employee);
    }

    private static ResponseCookie sessionCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(JwtService.TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(false) // Set to true once deployed with HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
