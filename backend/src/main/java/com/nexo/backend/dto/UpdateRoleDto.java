package com.nexo.backend.dto;

import com.nexo.backend.model.EmployeeRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleDto(
        @NotNull(message = "Role is required")
        EmployeeRole role) {
}
