package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;

public record ChangeStatusDto(TicketStatus newStatus) {
}
