package com.odontoapp.validacion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Anotación para validar que una fecha de nacimiento sea válida:
 * - No puede ser una fecha futura
 * - No puede ser de hace más de 120 años
 * - Debe ser una fecha razonable
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FechaNacimientoValidator.class)
public @interface FechaNacimientoValida {
    String message() default "La fecha de nacimiento no es válida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
