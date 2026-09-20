package com.nexo.backend.dto;

import com.nexo.backend.model.DeviceType;

public record DeviceDto(DeviceType type, String brand, String model, String identifier) {}