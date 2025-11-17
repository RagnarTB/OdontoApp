/**
 * Script de validación dinámica de documentos
 * Valida DNI (8 dígitos), RUC (11 dígitos) y Carnet de Extranjería (12 alfanuméricos)
 * según el tipo seleccionado
 */

(function() {
    'use strict';

    // Configuración de validaciones por tipo de documento
    const VALIDACIONES_DOCUMENTO = {
        'DNI': {
            maxLength: 8,
            pattern: /^\d{8}$/,
            mensaje: 'El DNI debe tener exactamente 8 dígitos numéricos',
            placeholder: '12345678',
            soloNumeros: true
        },
        'RUC': {
            maxLength: 11,
            pattern: /^\d{11}$/,
            mensaje: 'El RUC debe tener exactamente 11 dígitos numéricos',
            placeholder: '20123456789',
            soloNumeros: true
        },
        'CARNET DE EXTRANJERIA': {
            maxLength: 12,
            pattern: /^[A-Z0-9]{9,12}$/,
            mensaje: 'El Carnet debe tener entre 9 y 12 caracteres alfanuméricos',
            placeholder: 'ABC123456789',
            soloNumeros: false
        },
        'CARNET': {  // Alias
            maxLength: 12,
            pattern: /^[A-Z0-9]{9,12}$/,
            mensaje: 'El Carnet debe tener entre 9 y 12 caracteres alfanuméricos',
            placeholder: 'ABC123456789',
            soloNumeros: false
        },
        'PASAPORTE': {
            maxLength: 12,
            pattern: /^[A-Z0-9]{6,12}$/,
            mensaje: 'El Pasaporte debe tener entre 6 y 12 caracteres alfanuméricos',
            placeholder: 'ABC123456',
            soloNumeros: false
        }
    };

    /**
     * Inicializa la validación dinámica de documentos
     */
    function inicializarValidacionDocumentos() {
        const tipoDocSelect = document.getElementById('tipoDocumentoId');
        const numeroDocInput = document.getElementById('numeroDocumento');

        if (!tipoDocSelect || !numeroDocInput) {
            console.warn('No se encontraron los campos de tipo o número de documento');
            return;
        }

        // Aplicar validación al cambiar el tipo de documento
        tipoDocSelect.addEventListener('change', function() {
            aplicarValidacion(tipoDocSelect, numeroDocInput);
        });

        // Validar en tiempo real al escribir
        numeroDocInput.addEventListener('input', function(e) {
            const tipoSeleccionado = obtenerTipoDocumento(tipoDocSelect);
            const config = VALIDACIONES_DOCUMENTO[tipoSeleccionado];

            if (config) {
                // Filtrar solo números si es necesario
                if (config.soloNumeros) {
                    this.value = this.value.replace(/\D/g, '');
                } else {
                    // Para alfanuméricos, convertir a mayúsculas
                    this.value = this.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
                }

                // Limitar longitud
                if (this.value.length > config.maxLength) {
                    this.value = this.value.substring(0, config.maxLength);
                }

                // Validar patrón
                validarPatron(numeroDocInput, config);
            }
        });

        // Aplicar validación inicial si ya hay un tipo seleccionado
        if (tipoDocSelect.value) {
            aplicarValidacion(tipoDocSelect, numeroDocInput);
        }
    }

    /**
     * Obtiene el nombre del tipo de documento seleccionado
     */
    function obtenerTipoDocumento(selectElement) {
        const selectedOption = selectElement.options[selectElement.selectedIndex];
        if (!selectedOption || !selectedOption.text) {
            return null;
        }

        const tipoTexto = selectedOption.text.toUpperCase().trim();

        // Buscar coincidencia en las configuraciones
        for (let tipo in VALIDACIONES_DOCUMENTO) {
            if (tipoTexto.includes(tipo)) {
                return tipo;
            }
        }

        return null;
    }

    /**
     * Aplica la validación según el tipo de documento seleccionado
     */
    function aplicarValidacion(tipoSelect, numeroInput) {
        const tipoDocumento = obtenerTipoDocumento(tipoSelect);
        const config = VALIDACIONES_DOCUMENTO[tipoDocumento];

        if (config) {
            // Actualizar atributos del input
            numeroInput.maxLength = config.maxLength;
            numeroInput.setAttribute('pattern', config.pattern.source);
            numeroInput.setAttribute('title', config.mensaje);
            numeroInput.placeholder = config.placeholder;

            // Limpiar el valor si no coincide con el nuevo tipo
            if (numeroInput.value && !config.pattern.test(numeroInput.value)) {
                numeroInput.value = '';
            }

            // Actualizar mensaje de ayuda si existe
            const helpText = numeroInput.parentElement.querySelector('.form-text');
            if (helpText && !helpText.classList.contains('text-success')) {
                helpText.textContent = `${config.maxLength} dígitos - ${config.soloNumeros ? 'Solo números' : 'Alfanumérico'}`;
            }
        }
    }

    /**
     * Valida el patrón del documento y muestra feedback visual
     */
    function validarPatron(input, config) {
        const valor = input.value.trim();
        const errorElement = document.getElementById('doc-error');

        if (!valor) {
            input.classList.remove('is-invalid', 'is-valid');
            if (errorElement) errorElement.textContent = '';
            return true;
        }

        if (config.pattern.test(valor)) {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
            if (errorElement) {
                errorElement.textContent = '';
                errorElement.classList.remove('text-danger');
                errorElement.classList.add('text-success');
            }
            return true;
        } else {
            input.classList.remove('is-valid');
            input.classList.add('is-invalid');
            if (errorElement) {
                errorElement.textContent = config.mensaje;
                errorElement.classList.remove('text-success');
                errorElement.classList.add('text-danger');
            }
            return false;
        }
    }

    // Inicializar cuando el DOM esté listo
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', inicializarValidacionDocumentos);
    } else {
        inicializarValidacionDocumentos();
    }

    // Exportar funciones para uso externo si es necesario
    window.ValidacionDocumentos = {
        inicializar: inicializarValidacionDocumentos,
        validar: function(numeroDocumento, tipoDocumento) {
            const config = VALIDACIONES_DOCUMENTO[tipoDocumento];
            return config ? config.pattern.test(numeroDocumento) : false;
        }
    };

})();
