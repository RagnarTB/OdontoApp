/**
 * Utilidades para mostrar notificaciones con SweetAlert2
 * OdontoApp - Sistema de Gestión de Clínicas Dentales
 */

// Configuración global de SweetAlert2
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3500,
    timerProgressBar: true,
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
    }
});

/**
 * Muestra una notificación de éxito (toast pequeño en esquina)
 * @param {string} mensaje - Mensaje a mostrar
 */
function mostrarExito(mensaje) {
    Toast.fire({
        icon: 'success',
        title: mensaje
    });
}

/**
 * Muestra una notificación de error (toast pequeño en esquina)
 * @param {string} mensaje - Mensaje a mostrar
 */
function mostrarError(mensaje) {
    Toast.fire({
        icon: 'error',
        title: mensaje
    });
}

/**
 * Muestra una notificación de advertencia (toast pequeño en esquina)
 * @param {string} mensaje - Mensaje a mostrar
 */
function mostrarAdvertencia(mensaje) {
    Toast.fire({
        icon: 'warning',
        title: mensaje
    });
}

/**
 * Muestra una notificación informativa (toast pequeño en esquina)
 * @param {string} mensaje - Mensaje a mostrar
 */
function mostrarInfo(mensaje) {
    Toast.fire({
        icon: 'info',
        title: mensaje
    });
}

/**
 * Muestra un diálogo de confirmación
 * @param {string} titulo - Título del diálogo
 * @param {string} mensaje - Mensaje del diálogo
 * @param {string} textoBoton - Texto del botón de confirmar (por defecto: "Sí, confirmar")
 * @returns {Promise} - Promesa que resuelve con {isConfirmed: boolean}
 */
function confirmarAccion(titulo, mensaje, textoBoton = "Sí, confirmar") {
    return Swal.fire({
        title: titulo,
        text: mensaje,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: textoBoton,
        cancelButtonText: 'Cancelar'
    });
}

/**
 * Muestra un diálogo de confirmación para eliminar
 * @param {string} itemNombre - Nombre del item a eliminar
 * @returns {Promise} - Promesa que resuelve con {isConfirmed: boolean}
 */
function confirmarEliminacion(itemNombre) {
    return Swal.fire({
        title: '¿Está seguro?',
        html: `¿Desea eliminar <b>${itemNombre}</b>?<br><small class="text-muted">Esta acción no se puede deshacer.</small>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });
}

/**
 * Muestra un diálogo modal con información detallada
 * @param {string} titulo - Título del modal
 * @param {string} contenidoHtml - Contenido HTML del modal
 * @param {string} icono - Icono (success, error, warning, info, question)
 */
function mostrarModal(titulo, contenidoHtml, icono = 'info') {
    Swal.fire({
        title: titulo,
        html: contenidoHtml,
        icon: icono,
        confirmButtonText: 'Entendido'
    });
}

/**
 * Muestra un loading mientras se procesa una acción
 * @param {string} mensaje - Mensaje a mostrar (por defecto: "Procesando...")
 */
function mostrarCargando(mensaje = "Procesando...") {
    Swal.fire({
        title: mensaje,
        html: '<div class="spinner-border text-primary" role="status"><span class="sr-only">Cargando...</span></div>',
        showConfirmButton: false,
        allowOutsideClick: false,
        allowEscapeKey: false
    });
}

/**
 * Cierra el diálogo de loading
 */
function cerrarCargando() {
    Swal.close();
}

/**
 * Muestra una notificación de éxito grande (modal centrado)
 * @param {string} titulo - Título
 * @param {string} mensaje - Mensaje
 */
function mostrarExitoGrande(titulo, mensaje) {
    Swal.fire({
        icon: 'success',
        title: titulo,
        text: mensaje,
        confirmButtonText: 'Aceptar'
    });
}

/**
 * Muestra una notificación de error grande (modal centrado)
 * @param {string} titulo - Título
 * @param {string} mensaje - Mensaje
 */
function mostrarErrorGrande(titulo, mensaje) {
    Swal.fire({
        icon: 'error',
        title: titulo,
        text: mensaje,
        confirmButtonText: 'Aceptar'
    });
}

/**
 * Reemplaza los confirm() nativos de JavaScript
 * Uso: onclick="return confirmarNativo('¿Está seguro?')"
 */
function confirmarNativo(mensaje) {
    event.preventDefault();
    Swal.fire({
        title: '¿Está seguro?',
        text: mensaje,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Sí',
        cancelButtonText: 'No'
    }).then((result) => {
        if (result.isConfirmed) {
            // Continuar con la acción original
            event.target.closest('form')?.submit() || (window.location.href = event.target.href);
        }
    });
    return false;
}
