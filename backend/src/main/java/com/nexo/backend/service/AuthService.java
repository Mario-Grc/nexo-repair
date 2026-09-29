package com.nexo.backend.service;

import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.exception.InvalidCredentialsException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Login, session and password logic. The controller only handles HTTP.
@Service
public class AuthService {

    public record LoginResult(EmployeeDto employee, String token) {}

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(String email, String password) {
        // Same error for unknown email, inactive employee and wrong password
        // so the API does not reveal which emails exist.
        Employee employee = employeeRepository.findByEmailIgnoreCase(email.trim())
                .filter(Employee::isActive)
                .filter(e -> passwordEncoder.matches(password, e.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.generateToken(employee.getId(), employee.getEmail(), employee.getRole().name());
        return new LoginResult(EmployeeDto.from(employee), token);
    }

    public EmployeeDto me(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return EmployeeDto.from(employee);
    }

    public void changePassword(Long employeeId, String currentPassword, String newPassword) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " not found"));
        if (!passwordEncoder.matches(currentPassword, employee.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
    }
}
