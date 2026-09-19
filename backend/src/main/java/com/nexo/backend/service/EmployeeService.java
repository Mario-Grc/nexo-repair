package com.nexo.backend.service;

import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.model.Employee;
import com.nexo.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeDto toDto(Employee employee) {
        return new EmployeeDto(employee.getId(), employee.getName());
    }

    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::toDto).toList();
    }

    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        Employee employee = new Employee(employeeDto.name());
        Employee savedEmployee = employeeRepository.save(employee);
        return toDto(savedEmployee);
    }
}
