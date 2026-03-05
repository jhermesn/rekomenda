package com.rekomenda.api.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoControlCharsValidator implements ConstraintValidator<NoControlChars, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.chars().noneMatch(Character::isISOControl);
    }
}
