package com.nexo.backend.dto;

public record CreateTicketDto(
        String problemDescription,
        DeviceDto device,
        Long customerId
) {}
