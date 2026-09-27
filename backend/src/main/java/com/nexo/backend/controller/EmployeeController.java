package com.nexo.backend.controller;

import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.ResetPasswordDto;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeDto> getEmployees(@RequestParam(required = false) EmployeeRole role,
                                          @RequestParam(required = false) Boolean active) {
        return employeeService.getEmployees(role, active);
    }

    @PostMapping
    public EmployeeDto createEmployee(@Valid @RequestBody CreateEmployeeDto createEmployeeDto) {
        return employeeService.createEmployee(createEmployeeDto);
    }

    @PatchMapping("/{id}/password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDto dto) {
        employeeService.resetPassword(id, dto.newPassword());
    }
}
