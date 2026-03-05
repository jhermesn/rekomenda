package com.rekomenda.api.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankWhenPresentValidator implements ConstraintValidator<NotBlankWhenPresent, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) return true;
        return !value.toString().isBlank();
    }
}
