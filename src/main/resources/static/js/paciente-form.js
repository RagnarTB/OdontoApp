// Archivo: C:\proyectos\nuevo\odontoapp\src\main\resources\static\js\paciente-form.js

document.addEventListener('DOMContentLoaded', function () {

    // Identifica el botón de búsqueda. Puede ser btn-buscar-dni (admin) o btn-buscar-reniec-registro (self-service)
    const btnBuscarDni = document.getElementById('btn-buscar-dni') || document.getElementById('btn-buscar-reniec-registro');
    const numeroDocumentoInput = document.getElementById('numeroDocumento');
    const nombreCompletoInput = document.getElementById('nombreCompleto');

    // Si se cambia o borra el DNI, limpiar el nombre automáticamente
    if (numeroDocumentoInput && nombreCompletoInput) {
        numeroDocumentoInput.addEventListener('input', function() {
            const pacienteId = document.querySelector('input[name="id"]')?.value;
            if (!pacienteId) {
                // Solo en modo creación
                const dniActual = this.value.trim();
                const errorSpan = document.getElementById('doc-error');

                // Si se borra o cambia el DNI, borrar el nombre
                if (dniActual.length !== 8 || !/^\d{8}$/.test(dniActual)) {
                    nombreCompletoInput.value = '';
                    nombreCompletoInput.readOnly = true;
                    if (errorSpan) errorSpan.textContent = '';
                }
            }
        });
    }

    if (btnBuscarDni) {
        btnBuscarDni.addEventListener('click', function () {
            const numeroDocumentoInput = document.getElementById('numeroDocumento');
            const tipoDocumentoSelect = document.getElementById('tipoDocumentoId');
            const nombreCompletoInput = document.getElementById('nombreCompleto');
            const errorSpan = document.getElementById('doc-error');

            const numDoc = numeroDocumentoInput.value.trim();
            const tipoDocId = tipoDocumentoSelect.value;
            const tipoDocNombre = tipoDocumentoSelect.options[tipoDocumentoSelect.selectedIndex].text.trim().toUpperCase();

            errorSpan.textContent = '';
            nombreCompletoInput.value = '';

            // 🔹 Validación inicial
            if (!numDoc || !tipoDocId) {
                errorSpan.textContent = 'Seleccione el tipo y escriba el número de documento.';
                return;
            }

            // 🔹 Validación: Solo DNI
            if (tipoDocNombre !== 'DOCUMENTO NACIONAL DE IDENTIDAD' && tipoDocNombre !== 'DNI') {
                errorSpan.textContent = 'La búsqueda automática solo está disponible para DNI.';
                nombreCompletoInput.readOnly = false;
                return;
            }

            // 🔹 Validación: Formato DNI
            if (!/^\d{8}$/.test(numDoc)) {
                errorSpan.textContent = 'El DNI debe contener exactamente 8 dígitos numéricos.';
                return;
            }

            // 🔸 Iniciar búsqueda
            const self = this;
            const originalText = self.textContent;
            self.textContent = 'Buscando...';
            self.disabled = true;

            // Deshabilitar edición
            numeroDocumentoInput.readOnly = true;
            nombreCompletoInput.readOnly = true;

            errorSpan.classList.remove('text-success');
            errorSpan.classList.add('text-danger');
            errorSpan.textContent = 'Buscando en RENIEC...';

            // 🔹 Llamada a la API (fetch nativo)
            fetch(`/api/reniec?numDoc=${encodeURIComponent(numDoc)}&tipoDocId=${encodeURIComponent(tipoDocId)}`)
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            // 🔥 DETECCIÓN DE RESTABLECIMIENTO (409)
                            if (response.status === 409 && errorData.restaurar) {
                                throw {
                                    isRestorable: true,
                                    message: errorData.error,
                                    pacienteId: errorData.pacienteId
                                };
                            }
                            throw new Error(errorData.error || `Error ${response.status}: ${response.statusText}`);
                        }).catch(e => {
                            throw (e.isRestorable ? e : new Error(`Error ${response.status}: ${response.statusText}`));
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    let nombreEncontrado = data && data.nombreCompleto ? data.nombreCompleto.trim() : null;

                    if (nombreEncontrado) {
                        nombreCompletoInput.value = nombreEncontrado;
                        nombreCompletoInput.readOnly = true;
                        errorSpan.textContent = 'Datos cargados con éxito.';
                        errorSpan.classList.remove('text-danger');
                        errorSpan.classList.add('text-success');
                    } else {
                        errorSpan.textContent = 'DNI encontrado, pero no se recuperaron datos de nombre.';
                        nombreCompletoInput.readOnly = false;
                    }
                })
                .catch(error => {
                    if (error.isRestorable) {
                        // Lógica para el ADMIN/RECEPCIÓN: Solicitar restablecimiento
                        const confirmar = window.confirm(`El paciente está eliminado. ¿Desea restablecerlo (ID: ${error.pacienteId}) y continuar con la edición?`);
                        if (confirmar) {
                            window.location.href = `/pacientes/restablecer/${error.pacienteId}`;
                            return;
                        }
                    }

                    errorSpan.textContent = error.message || 'Error desconocido al consultar Reniec.';
                    errorSpan.classList.remove('text-success');
                    errorSpan.classList.add('text-danger');
                    nombreCompletoInput.readOnly = false;
                })
                .finally(() => {
                    self.textContent = originalText;
                    const idInput = document.querySelector('input[name="id"]');
                    if (!idInput || !idInput.value) {
                        self.disabled = false;
                        numeroDocumentoInput.readOnly = false;
                    }
                });
        });
    }
});

// Nota: Eliminar el <script> inline de modulos/pacientes/formulario.html y registro-formulario.html