package com.odontoapp.validacion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Anotación para validar que una fecha de vencimiento sea válida:
 * - Debe ser al menos 1 mes en el futuro desde la fecha actual
 * - No puede ser una fecha pasada
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FechaVencimientoValidator.class)
public @interface FechaVencimientoValida {
    String message() default "La fecha de vencimiento debe ser al menos 1 mes en el futuro";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
