package com.nexo.backend.security;

public record EmployeePrincipal(Long employeeId, String email, String role) {}
