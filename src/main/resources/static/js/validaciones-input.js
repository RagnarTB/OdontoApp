/**
 * validaciones-input.js
 * Archivo centralizado para validaciones de inputs en toda la aplicación OdontoApp
 *
 * Funciones:
 * - Validación de DNI (8 dígitos, solo números)
 * - Validación de teléfono (9 dígitos, solo números)
 * - Validación de nombres (solo letras y espacios)
 * - Validación de números (bloqueo de letras)
 * - Validación de fechas (no futuras, no pasadas)
 * - Validación de edad >= 18 años
 */

(function() {
    'use strict';

    // ===== VALIDACIÓN DE DNI (8 DÍGITOS, SOLO NÚMEROS) =====
    window.validarInputDNI = function(input) {
        input.addEventListener('input', function(e) {
            // Eliminar cualquier caracter que no sea número
            this.value = this.value.replace(/[^0-9]/g, '');

            // Limitar a 8 dígitos
            if (this.value.length > 8) {
                this.value = this.value.slice(0, 8);
            }
        });

        input.addEventListener('keypress', function(e) {
            // Solo permite números
            if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Delete' && e.key !== 'Tab') {
                e.preventDefault();
            }
        });

        // Validación al perder foco
        input.addEventListener('blur', function() {
            if (this.value && this.value.length !== 8) {
                this.setCustomValidity('El DNI debe tener exactamente 8 dígitos');
                this.classList.add('is-invalid');
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
            }
        });
    };

    // ===== VALIDACIÓN DE TELÉFONO (9 DÍGITOS, SOLO NÚMEROS) =====
    window.validarInputTelefono = function(input) {
        input.addEventListener('input', function(e) {
            // Eliminar cualquier caracter que no sea número
            this.value = this.value.replace(/[^0-9]/g, '');

            // Limitar a 9 dígitos
            if (this.value.length > 9) {
                this.value = this.value.slice(0, 9);
            }
        });

        input.addEventListener('keypress', function(e) {
            // Solo permite números
            if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Delete' && e.key !== 'Tab') {
                e.preventDefault();
            }
        });

        // Validación al perder foco
        input.addEventListener('blur', function() {
            if (this.value && this.value.length !== 9) {
                this.setCustomValidity('El teléfono debe tener exactamente 9 dígitos');
                this.classList.add('is-invalid');
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
            }
        });
    };

    // ===== VALIDACIÓN DE NOMBRES (SOLO LETRAS, ESPACIOS Y ACENTOS) =====
    window.validarInputNombre = function(input) {
        input.addEventListener('input', function(e) {
            // Permite solo letras (incluyendo acentos), espacios y apóstrofes
            this.value = this.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s']/g, '');
        });

        input.addEventListener('keypress', function(e) {
            // Bloquea números y caracteres especiales (excepto espacio y apóstrofe)
            if (!/[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s']/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Delete' && e.key !== 'Tab') {
                e.preventDefault();
            }
        });
    };

    // ===== VALIDACIÓN DE SOLO NÚMEROS =====
    window.validarInputSoloNumeros = function(input, maxLength) {
        input.addEventListener('input', function(e) {
            // Eliminar cualquier caracter que no sea número
            this.value = this.value.replace(/[^0-9]/g, '');

            // Limitar longitud si se especifica
            if (maxLength && this.value.length > maxLength) {
                this.value = this.value.slice(0, maxLength);
            }
        });

        input.addEventListener('keypress', function(e) {
            // Solo permite números
            if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Delete' && e.key !== 'Tab') {
                e.preventDefault();
            }
        });
    };

    // ===== VALIDACIÓN DE DECIMALES =====
    window.validarInputDecimal = function(input, maxDecimals = 2) {
        input.addEventListener('input', function(e) {
            // Permite números y un solo punto decimal
            this.value = this.value.replace(/[^0-9.]/g, '');

            // Evitar múltiples puntos
            const parts = this.value.split('.');
            if (parts.length > 2) {
                this.value = parts[0] + '.' + parts.slice(1).join('');
            }

            // Limitar decimales
            if (parts.length === 2 && parts[1].length > maxDecimals) {
                this.value = parts[0] + '.' + parts[1].slice(0, maxDecimals);
            }
        });

        input.addEventListener('keypress', function(e) {
            // Solo permite números y punto decimal
            if (!/[0-9.]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Delete' && e.key !== 'Tab') {
                e.preventDefault();
            }
        });
    };

    // ===== VALIDACIÓN DE FECHA NO FUTURA =====
    window.validarFechaNoFutura = function(input) {
        input.addEventListener('change', function() {
            const fechaSeleccionada = new Date(this.value);
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fechaSeleccionada > hoy) {
                this.setCustomValidity('La fecha no puede ser futura');
                this.classList.add('is-invalid');
                mostrarAdvertencia('La fecha no puede ser futura');
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
            }
        });

        // Establecer fecha máxima como hoy
        const hoy = new Date().toISOString().split('T')[0];
        input.setAttribute('max', hoy);
    };

    // ===== VALIDACIÓN DE FECHA NO PASADA =====
    window.validarFechaNoPasada = function(input) {
        input.addEventListener('change', function() {
            const fechaSeleccionada = new Date(this.value);
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fechaSeleccionada < hoy) {
                this.setCustomValidity('La fecha no puede ser pasada');
                this.classList.add('is-invalid');
                mostrarAdvertencia('La fecha no puede ser pasada');
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
            }
        });

        // Establecer fecha mínima como hoy
        const hoy = new Date().toISOString().split('T')[0];
        input.setAttribute('min', hoy);
    };

    // ===== VALIDACIÓN EDAD MÍNIMA (18 AÑOS) =====
    window.validarEdadMinima = function(input, edadMinima = 18) {
        input.addEventListener('change', function() {
            if (!this.value) return;

            const fechaNacimiento = new Date(this.value);
            const hoy = new Date();

            // Calcular edad
            let edad = hoy.getFullYear() - fechaNacimiento.getFullYear();
            const mes = hoy.getMonth() - fechaNacimiento.getMonth();

            if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNacimiento.getDate())) {
                edad--;
            }

            if (edad < edadMinima) {
                this.setCustomValidity(`Debe ser mayor de ${edadMinima} años`);
                this.classList.add('is-invalid');
                mostrarAdvertencia(`El trabajador debe ser mayor de ${edadMinima} años`);
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
            }
        });

        // Establecer fecha máxima (hace 18 años)
        const hace18Anos = new Date();
        hace18Anos.setFullYear(hace18Anos.getFullYear() - edadMinima);
        const fechaMaxima = hace18Anos.toISOString().split('T')[0];
        input.setAttribute('max', fechaMaxima);
    };

    // ===== VALIDACIÓN DE DATETIME-LOCAL CON 2 HORAS DE ANTICIPACIÓN =====
    window.validarFechaHoraCitaConAnticipacion = function(input) {
        input.addEventListener('change', function() {
            if (!this.value) return;

            const fechaHoraSeleccionada = new Date(this.value);
            const ahora = new Date();

            // Agregar 2 horas de anticipación
            const dosHorasAdelante = new Date(ahora.getTime() + (2 * 60 * 60 * 1000));

            if (fechaHoraSeleccionada < dosHorasAdelante) {
                this.setCustomValidity('La cita debe agendarse con al menos 2 horas de anticipación');
                this.classList.add('is-invalid');
                mostrarAdvertencia('La cita debe agendarse con al menos 2 horas de anticipación');
                return false;
            } else {
                this.setCustomValidity('');
                this.classList.remove('is-invalid');
                return true;
            }
        });

        // Establecer fecha/hora mínima (2 horas desde ahora)
        const dosHorasAdelante = new Date(Date.now() + (2 * 60 * 60 * 1000));
        const minDateTime = dosHorasAdelante.toISOString().slice(0, 16);
        input.setAttribute('min', minDateTime);
    };

    // ===== HACER CAMPO NO EDITABLE PERO VISIBLE =====
    window.hacerCampoNoEditable = function(input) {
        input.setAttribute('readonly', true);
        input.style.backgroundColor = '#e9ecef';
        input.style.cursor = 'not-allowed';
    };

    // ===== INICIALIZACIÓN AUTOMÁTICA AL CARGAR EL DOM =====
    document.addEventListener('DOMContentLoaded', function() {
        // Auto-aplicar validaciones según atributos data

        // DNI
        document.querySelectorAll('input[data-validar="dni"]').forEach(function(input) {
            validarInputDNI(input);
        });

        // Teléfono
        document.querySelectorAll('input[data-validar="telefono"]').forEach(function(input) {
            validarInputTelefono(input);
        });

        // Nombres
        document.querySelectorAll('input[data-validar="nombre"]').forEach(function(input) {
            validarInputNombre(input);
        });

        // Solo números
        document.querySelectorAll('input[data-validar="numeros"]').forEach(function(input) {
            const maxLength = input.getAttribute('data-max-length');
            validarInputSoloNumeros(input, maxLength);
        });

        // Decimales
        document.querySelectorAll('input[data-validar="decimal"]').forEach(function(input) {
            const maxDecimals = input.getAttribute('data-max-decimals') || 2;
            validarInputDecimal(input, maxDecimals);
        });

        // Fecha no futura
        document.querySelectorAll('input[data-validar="fecha-no-futura"]').forEach(function(input) {
            validarFechaNoFutura(input);
        });

        // Fecha no pasada
        document.querySelectorAll('input[data-validar="fecha-no-pasada"]').forEach(function(input) {
            validarFechaNoPasada(input);
        });

        // Edad mínima
        document.querySelectorAll('input[data-validar="edad-minima"]').forEach(function(input) {
            const edadMinima = input.getAttribute('data-edad-minima') || 18;
            validarEdadMinima(input, edadMinima);
        });

        // Fecha/hora cita con anticipación
        document.querySelectorAll('input[data-validar="cita-anticipacion"]').forEach(function(input) {
            validarFechaHoraCitaConAnticipacion(input);
        });

        // Campos no editables
        document.querySelectorAll('input[data-no-editable="true"]').forEach(function(input) {
            hacerCampoNoEditable(input);
        });
    });

    // Exportar funciones globalmente
    window.ValidacionesInput = {
        validarInputDNI: validarInputDNI,
        validarInputTelefono: validarInputTelefono,
        validarInputNombre: validarInputNombre,
        validarInputSoloNumeros: validarInputSoloNumeros,
        validarInputDecimal: validarInputDecimal,
        validarFechaNoFutura: validarFechaNoFutura,
        validarFechaNoPasada: validarFechaNoPasada,
        validarEdadMinima: validarEdadMinima,
        validarFechaHoraCitaConAnticipacion: validarFechaHoraCitaConAnticipacion,
        hacerCampoNoEditable: hacerCampoNoEditable
    };

})();
