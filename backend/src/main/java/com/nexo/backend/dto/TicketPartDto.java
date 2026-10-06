package com.nexo.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketPartDto(
        Long id,
        String description,
        int quantity,
        BigDecimal unitPrice,
        // unitPrice times quantity, or null when the line has no price.
        BigDecimal lineTotal,
        String addedByName,
        Instant addedAt) {
}
