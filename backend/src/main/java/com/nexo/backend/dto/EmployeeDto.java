package com.nexo.backend.dto;

import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;

public record EmployeeDto(Long id, String name, String email, EmployeeRole role, boolean active) {

    // Single mapping shared by EmployeeService and AuthService.
    public static EmployeeDto from(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive()
        );
    }
}
