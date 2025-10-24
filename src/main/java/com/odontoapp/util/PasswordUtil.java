package com.odontoapp.util;

import java.security.SecureRandom;

public class PasswordUtil {

    private static final String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMEROS = "0123456789";
    private static final String ESPECIALES = "!@#$%&*";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Genera una contraseña aleatoria segura de 12 caracteres
     * Garantiza al menos: 1 mayúscula, 1 minúscula, 1 número y 1 especial
     */
    public static String generarPasswordAleatoria() {
        StringBuilder password = new StringBuilder(12);

        // Garantizar al menos un carácter de cada tipo
        password.append(MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length())));
        password.append(MINUSCULAS.charAt(random.nextInt(MINUSCULAS.length())));
        password.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        password.append(ESPECIALES.charAt(random.nextInt(ESPECIALES.length())));

        // Completar hasta 12 caracteres
        for (int i = 4; i < 12; i++) {
            password.append(TODOS.charAt(random.nextInt(TODOS.length())));
        }

        // Mezclar los caracteres
        return mezclarCaracteres(password.toString());
    }

    private static String mezclarCaracteres(String input) {
        char[] caracteres = input.toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temp;
        }
        return new String(caracteres);
    }

    /**
     * Valida que la contraseña cumpla con los requisitos de seguridad
     * 
     * @return null si es válida, o un mensaje de error si no cumple
     */
    public static String validarPasswordRobusta(String password) {
        if (password == null || password.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres.";
        }

        boolean tieneMayuscula = password.chars().anyMatch(Character::isUpperCase);
        boolean tieneMinuscula = password.chars().anyMatch(Character::isLowerCase);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);
        boolean tieneEspecial = password.chars().anyMatch(ch -> ESPECIALES.indexOf(ch) >= 0);

        if (!tieneMayuscula) {
            return "La contraseña debe contener al menos una letra mayúscula.";
        }
        if (!tieneMinuscula) {
            return "La contraseña debe contener al menos una letra minúscula.";
        }
        if (!tieneNumero) {
            return "La contraseña debe contener al menos un número.";
        }
        if (!tieneEspecial) {
            return "La contraseña debe contener al menos un carácter especial (!@#$%&*).";
        }

        return null; // Contraseña válida
    }
}