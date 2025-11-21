package com.odontoapp.validacion;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador personalizado para fechas de nacimiento.
 * Verifica que la fecha:
 * - No sea nula (si es obligatoria, usar @NotNull adicional)
 * - No sea una fecha futura
 * - No sea de hace más de 120 años (edad máxima razonable)
 */
public class FechaNacimientoValidator implements ConstraintValidator<FechaNacimientoValida, LocalDate> {

    private static final int EDAD_MAXIMA_ANOS = 120;

    @Override
    public void initialize(FechaNacimientoValida constraintAnnotation) {
        // No se necesita inicialización especial
    }

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        // Si la fecha es nula, permitir (la validación de @NotNull debe manejarlo por separado)
        if (fechaNacimiento == null) {
            return true;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate hace120Anos = hoy.minusYears(EDAD_MAXIMA_ANOS);

        // Validar que no sea una fecha futura
        if (fechaNacimiento.isAfter(hoy)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "La fecha de nacimiento no puede ser una fecha futura")
                .addConstraintViolation();
            return false;
        }

        // Validar que no sea de hace más de 120 años
        if (fechaNacimiento.isBefore(hace120Anos)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "La fecha de nacimiento no puede ser de hace más de " + EDAD_MAXIMA_ANOS + " años")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
