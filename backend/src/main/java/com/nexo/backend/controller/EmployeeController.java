package com.nexo.backend.controller;

import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.EmployeeOptionDto;
import com.nexo.backend.dto.ResetPasswordDto;
import com.nexo.backend.dto.UpdateActiveDto;
import com.nexo.backend.dto.UpdateEmployeeProfileDto;
import com.nexo.backend.dto.UpdateRoleDto;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.security.EmployeePrincipal;
import com.nexo.backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/assignable")
    public List<EmployeeOptionDto> getAssignable() {
        return employeeService.getAssignable();
    }

    @PostMapping
    public EmployeeDto createEmployee(@Valid @RequestBody CreateEmployeeDto createEmployeeDto) {
        return employeeService.createEmployee(createEmployeeDto);
    }

    @PatchMapping("/{id}/password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDto dto) {
        employeeService.resetPassword(id, dto.newPassword());
    }

    @PatchMapping("/{id}/role")
    public EmployeeDto updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleDto dto,
                                  @AuthenticationPrincipal EmployeePrincipal principal) {
        return employeeService.updateRole(id, dto.role(), principal.employeeId());
    }

    @PatchMapping("/{id}/active")
    public EmployeeDto updateActive(@PathVariable Long id, @Valid @RequestBody UpdateActiveDto dto,
                                    @AuthenticationPrincipal EmployeePrincipal principal) {
        return employeeService.updateActive(id, dto.active(), principal.employeeId());
    }

    @PatchMapping("/{id}/profile")
    public EmployeeDto updateProfile(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeProfileDto dto) {
        return employeeService.updateProfile(id, dto);
    }
}
