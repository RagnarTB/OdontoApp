// Validación dinámica de tipo de documento
document.addEventListener('DOMContentLoaded', function () {
    const tipoDocumentoSelect = document.getElementById('tipoDocumentoId');
    const numeroDocumentoInput = document.getElementById('numeroDocumento');

    // Buscar el elemento de ayuda (puede estar en diferentes lugares según el formulario)
    let helpText = numeroDocumentoInput?.closest('.form-group')?.querySelector('.form-text.text-muted');

    if (!tipoDocumentoSelect || !numeroDocumentoInput) {
        return; // No hacer nada si los elementos no existen
    }

    // Si no existe el texto de ayuda, crearlo
    if (!helpText) {
        helpText = document.createElement('small');
        helpText.className = 'form-text text-muted';
        helpText.id = 'help-numero-documento';
        numeroDocumentoInput.closest('.form-group').appendChild(helpText);
    }

    function actualizarValidacionDocumento() {
        const selectedOption = tipoDocumentoSelect.options[tipoDocumentoSelect.selectedIndex];
        const tipoDocTexto = selectedOption ? selectedOption.text.toUpperCase() : '';

        // Determinar el tipo de documento y actualizar validaciones
        if (tipoDocTexto.includes('DNI')) {
            numeroDocumentoInput.setAttribute('maxlength', '8');
            numeroDocumentoInput.setAttribute('pattern', '[0-9]{8}');
            numeroDocumentoInput.setAttribute('placeholder', '12345678');
            helpText.textContent = '8 dígitos - Solo números';
        } else if (tipoDocTexto.includes('RUC')) {
            numeroDocumentoInput.setAttribute('maxlength', '11');
            numeroDocumentoInput.setAttribute('pattern', '[0-9]{11}');
            numeroDocumentoInput.setAttribute('placeholder', '20123456789');
            helpText.textContent = '11 dígitos - Solo números';
        } else if (tipoDocTexto.includes('CARNET') || tipoDocTexto.includes('EXTRANJERÍA') || tipoDocTexto.includes('EXTRANJERIA')) {
            numeroDocumentoInput.setAttribute('maxlength', '12');
            numeroDocumentoInput.removeAttribute('pattern'); // Permitir alfanuméricos
            numeroDocumentoInput.setAttribute('placeholder', 'ABC123456789');
            helpText.textContent = 'Hasta 12 caracteres alfanuméricos';
        } else if (tipoDocTexto.includes('PASAPORTE')) {
            numeroDocumentoInput.setAttribute('maxlength', '12');
            numeroDocumentoInput.removeAttribute('pattern'); // Permitir alfanuméricos
            numeroDocumentoInput.setAttribute('placeholder', 'A12345678');
            helpText.textContent = 'Hasta 12 caracteres alfanuméricos';
        } else {
            // Tipo de documento genérico
            numeroDocumentoInput.setAttribute('maxlength', '20');
            numeroDocumentoInput.removeAttribute('pattern');
            numeroDocumentoInput.setAttribute('placeholder', 'Número de documento');
            helpText.textContent = 'Ingrese el número de documento';
        }
    }

    // Ejecutar al cargar la página
    actualizarValidacionDocumento();

    // Ejecutar al cambiar el tipo de documento
    tipoDocumentoSelect.addEventListener('change', actualizarValidacionDocumento);
});
