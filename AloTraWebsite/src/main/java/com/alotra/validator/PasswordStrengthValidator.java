package com.alotra.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordStrengthValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        if (password.length() < 8) {
            addConstraintViolation(context, "Mật khẩu phải có ít nhất 8 ký tự");
            return false;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecialChar) {
            StringBuilder errors = new StringBuilder("Mật khẩu phải chứa: ");
            if (!hasUpperCase) errors.append("chữ hoa, ");
            if (!hasLowerCase) errors.append("chữ thường, ");
            if (!hasDigit) errors.append("chữ số, ");
            if (!hasSpecialChar) errors.append("ký tự đặc biệt");

            String message = errors.toString().replaceAll(", $", "");
            addConstraintViolation(context, message);
            return false;
        }

        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}