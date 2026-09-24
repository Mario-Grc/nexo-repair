package com.nexo.backend.controller;

import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeControler {
    private final EmployeeService employeeService;

    public EmployeeControler(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeDto> getEmployees(@RequestParam(required = false) EmployeeRole role,
                                          @RequestParam(required = false) Boolean active) {
        return employeeService.getEmployees(role, active);
    }

    @PostMapping
    public EmployeeDto createEmployee(@RequestBody CreateEmployeeDto createEmployeeDto) {
        return employeeService.createEmployee(createEmployeeDto);
    }
}
