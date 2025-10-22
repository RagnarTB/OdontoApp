document.addEventListener('DOMContentLoaded', function () {
    const btnBuscarDni = document.getElementById('btn-buscar-dni');

    if (btnBuscarDni) { // Asegurarse de que el botón existe en la página
        btnBuscarDni.addEventListener('click', function () {
            const dniInput = document.getElementById('dni');
            const dni = dniInput.value;
            const nombreCompletoInput = document.getElementById('nombreCompleto');
            const dniError = document.getElementById('dni-error');
            const self = this; // Guardar referencia al botón

            dniError.textContent = ''; // Limpiar errores

            // Validación básica de formato DNI
            if (!/^\d{8}$/.test(dni)) {
                dniError.textContent = 'El DNI debe contener exactamente 8 dígitos numéricos.';
                return;
            }

            // Deshabilitar botón y campo DNI durante la búsqueda
            self.textContent = 'Buscando...';
            self.disabled = true;
            dniInput.readOnly = true;

            fetch('/api/reniec/' + dni)
                .then(response => {
                    // ... (manejo de errores de respuesta como antes) ...
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            throw new Error(errorData.error || `Error ${response.status}: ${response.statusText}`);
                        }).catch(() => {
                            throw new Error(`Error ${response.status}: ${response.statusText}`);
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    // *** LÓGICA MEJORADA ***
                    let nombreEncontrado = null;

                    // 1. Prioridad: Usar nombreCompleto si existe y no está vacío
                    if (data && data.nombreCompleto && data.nombreCompleto.trim() !== '') {
                        nombreEncontrado = data.nombreCompleto.trim();
                    }
                    // 2. Fallback: Construir desde partes si nombreCompleto falta pero las partes existen
                    else if (data && data.nombres && data.apellidoPaterno) {
                        let partes = [
                            data.apellidoPaterno.trim(),
                            data.apellidoMaterno ? data.apellidoMaterno.trim() : null, // Manejar apellido materno opcional
                            data.nombres.trim()
                        ];
                        // Filtrar partes nulas/vacías y unir con espacio
                        nombreEncontrado = partes.filter(p => p && p !== '').join(' ').replace(/\s+/g, ' ');
                    }

                    // Asignar si se encontró/construyó un nombre
                    if (nombreEncontrado) {
                        nombreCompletoInput.value = nombreEncontrado;
                    } else {
                        // Si ni nombreCompleto ni las partes necesarias existen
                        dniError.textContent = 'DNI encontrado, pero no se recuperaron datos de nombre.';
                        console.warn("Respuesta API Reniec incompleta:", data);
                    }
                    // *** FIN LÓGICA MEJORADA ***
                })
                .catch(error => {
                    // ... (manejo de errores de fetch como antes) ...
                    dniError.textContent = error.message;
                    console.error("Error en fetch API Reniec:", error);
                })
                .finally(() => {
                    // ... (re-habilitar botón/campo como antes) ...
                    self.textContent = 'Buscar en Reniec';
                    const idInput = document.querySelector('input[name="id"]');
                    if (!idInput || !idInput.value) {
                        self.disabled = false;
                        dniInput.readOnly = false;
                    }
                });
        });
    }

    // Deshabilitar botón de búsqueda si ya hay un ID (estamos editando)
    const idInput = document.querySelector('input[name="id"]');
    if (idInput && idInput.value && btnBuscarDni) {
        btnBuscarDni.disabled = true;
        const dniInput = document.getElementById('dni');
        if (dniInput) dniInput.readOnly = true;
    }

});