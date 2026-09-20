package com.nexo.backend.service;

import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.model.Employee;
import com.nexo.backend.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private EmployeeDto toDto(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive()
        );
    }

    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::toDto).toList();
    }

    public EmployeeDto createEmployee(CreateEmployeeDto dto) {
        Employee employee = new Employee(
                dto.name(),
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.role()
        );
        return toDto(employeeRepository.save(employee));
    }
}
