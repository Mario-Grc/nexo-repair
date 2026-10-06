package com.nexo.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddPartDto(
        @NotBlank(message = "Description is required")
        @Size(max = 200, message = "Description must be at most 200 characters")
        String description,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 999, message = "Quantity must be at most 999")
        Integer quantity,
        // Optional charge price. Null means no price yet.
        @DecimalMin(value = "0.00", message = "Unit price must be zero or more")
        @Digits(integer = 8, fraction = 2, message = "Unit price must have at most 8 integer digits and 2 decimals")
        BigDecimal unitPrice) {
}
