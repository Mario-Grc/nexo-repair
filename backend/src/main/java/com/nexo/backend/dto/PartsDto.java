package com.nexo.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record PartsDto(
        List<TicketPartDto> items,
        // Sum of the lines that have a price, or null when none has a price.
        BigDecimal total) {
}
