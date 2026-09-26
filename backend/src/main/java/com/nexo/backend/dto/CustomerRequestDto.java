package com.nexo.backend.dto;

import com.nexo.backend.validation.ValidContact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ValidContact
public record CustomerRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 255, message = "Email must be at most 255 characters")
        @Email(message = "Email must be valid")
        String email,

        @Size(max = 50, message = "Phone must be at most 50 characters")
        String phone,

        String notes) {
}
