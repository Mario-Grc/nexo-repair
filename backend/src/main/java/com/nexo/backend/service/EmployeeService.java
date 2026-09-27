package com.nexo.backend.service;

import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.exception.DuplicateEmailException;
import com.nexo.backend.exception.InvalidCredentialsException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.SelfModificationException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public EmployeeDto toDto(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive()
        );
    }

    public List<EmployeeDto> getEmployees(EmployeeRole role, Boolean active) {
        return employeeRepository.findAll().stream()
                .filter(e -> role == null || e.getRole() == role)
                .filter(e -> active == null || e.isActive() == active)
                .map(this::toDto).toList();
    }

    public EmployeeDto createEmployee(CreateEmployeeDto dto) {
        String email = dto.email().trim().toLowerCase();
        employeeRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new DuplicateEmailException();
        });
        Employee employee = new Employee(
                dto.name().trim(),
                email,
                passwordEncoder.encode(dto.password()),
                dto.role()
        );
        return toDto(employeeRepository.save(employee));
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

    public void resetPassword(Long id, String newPassword) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
    }

    public EmployeeDto updateRole(Long id, EmployeeRole role, Long actingEmployeeId) {
        if (Objects.equals(id, actingEmployeeId)) {
            throw new SelfModificationException("You cannot change your own role");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
        employee.setRole(role);
        return toDto(employeeRepository.save(employee));
    }

    public EmployeeDto updateActive(Long id, Boolean active, Long actingEmployeeId) {
        if (Objects.equals(id, actingEmployeeId)) {
            throw new SelfModificationException("You cannot deactivate yourself");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
        employee.setActive(active);
        return toDto(employeeRepository.save(employee));
    }
}
