package com.nexo.backend.dto;

import com.nexo.backend.model.EmployeeRole;

public record CreateEmployeeDto(String name, String email, String password, EmployeeRole role) {}
