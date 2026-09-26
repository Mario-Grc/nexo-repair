package com.nexo.backend.validation;

import com.nexo.backend.dto.CustomerRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidContactValidator implements ConstraintValidator<ValidContact, CustomerRequestDto> {

    @Override
    public boolean isValid(CustomerRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        boolean hasEmail = dto.email() != null && !dto.email().isBlank();
        boolean hasPhone = dto.phone() != null && !dto.phone().isBlank();
        return hasEmail || hasPhone;
    }
}
