package com.nexo.backend.dto;

import com.nexo.backend.model.EmployeeRole;

public record EmployeeDto(Long id, String name, String email, EmployeeRole role, boolean active) {}
