package com.odontoapp.validacion;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador personalizado para fechas de vencimiento de insumos.
 * Verifica que la fecha:
 * - No sea nula (si es obligatoria, usar @NotNull adicional)
 * - Sea al menos 1 mes en el futuro desde hoy
 * - No sea una fecha pasada
 */
public class FechaVencimientoValidator implements ConstraintValidator<FechaVencimientoValida, LocalDate> {

    private static final int MESES_MINIMOS = 1;

    @Override
    public void initialize(FechaVencimientoValida constraintAnnotation) {
        // No se necesita inicialización especial
    }

    @Override
    public boolean isValid(LocalDate fechaVencimiento, ConstraintValidatorContext context) {
        // Si la fecha es nula, permitir (la validación de @NotNull debe manejarlo por separado)
        if (fechaVencimiento == null) {
            return true;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaMinimaPermitida = hoy.plusMonths(MESES_MINIMOS);

        // Validar que no sea una fecha pasada
        if (fechaVencimiento.isBefore(hoy)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "La fecha de vencimiento no puede ser una fecha pasada")
                .addConstraintViolation();
            return false;
        }

        // Validar que sea al menos 1 mes en el futuro
        if (fechaVencimiento.isBefore(fechaMinimaPermitida)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "La fecha de vencimiento debe ser al menos " + MESES_MINIMOS + " mes(es) en el futuro")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
