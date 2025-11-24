/**
 * Sistema de Validación de Permisos para OdontoApp
 *
 * Este módulo proporciona funciones para validar permisos del usuario actual
 * y mostrar alertas cuando no tenga los permisos necesarios.
 *
 * Uso:
 * 1. Incluir este archivo en las vistas: <script th:src="@{/js/permisos-validator.js}"></script>
 * 2. Agregar atributos data-permiso a los botones: data-permiso="EDITAR_USUARIOS"
 * 3. Llamar a PermisosValidator.init() al cargar la página
 */

const PermisosValidator = (function() {
    'use strict';

    // Cache de permisos del usuario (se carga una sola vez)
    let permisosUsuario = null;
    let cargando = false;

    /**
     * Carga los permisos del usuario actual desde el servidor
     * @returns {Promise<Set<string>>} Set con los permisos del usuario
     */
    async function cargarPermisos() {
        if (permisosUsuario !== null) {
            return permisosUsuario;
        }

        if (cargando) {
            // Si ya está cargando, esperar a que termine
            return new Promise((resolve) => {
                const intervalo = setInterval(() => {
                    if (!cargando) {
                        clearInterval(intervalo);
                        resolve(permisosUsuario);
                    }
                }, 100);
            });
        }

        cargando = true;

        try {
            const response = await fetch('/api/permisos/mis-permisos');
            if (!response.ok) {
                throw new Error('Error al cargar permisos');
            }

            const data = await response.json();
            permisosUsuario = new Set(data.permisos || []);
            return permisosUsuario;
        } catch (error) {
            console.error('Error al cargar permisos:', error);
            permisosUsuario = new Set(); // Vacío en caso de error
            return permisosUsuario;
        } finally {
            cargando = false;
        }
    }

    /**
     * Verifica si el usuario tiene un permiso específico
     * @param {string} permiso - Nombre del permiso (ej: "CREAR_USUARIOS")
     * @returns {Promise<boolean>}
     */
    async function tienePermiso(permiso) {
        const permisos = await cargarPermisos();
        return permisos.has(permiso);
    }

    /**
     * Verifica si el usuario tiene al menos uno de varios permisos
     * @param {string[]} permisos - Array de nombres de permisos
     * @returns {Promise<boolean>}
     */
    async function tieneAlgunoDePermisos(permisos) {
        const permisosUsuario = await cargarPermisos();
        return permisos.some(p => permisosUsuario.has(p));
    }

    /**
     * Verifica si el usuario tiene todos los permisos especificados
     * @param {string[]} permisos - Array de nombres de permisos
     * @returns {Promise<boolean>}
     */
    async function tieneTodosLosPermisos(permisos) {
        const permisosUsuario = await cargarPermisos();
        return permisos.every(p => permisosUsuario.has(p));
    }

    /**
     * Muestra un alert de SweetAlert2 cuando no tiene permisos
     * @param {string} accion - Descripción de la acción (ej: "crear usuarios", "editar pacientes")
     */
    function mostrarAlertaNoPermiso(accion) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'warning',
                title: 'Acceso Denegado',
                text: `No tienes permiso para ${accion}.`,
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#6c757d'
            });
        } else {
            alert(`No tienes permiso para ${accion}.`);
        }
    }

    /**
     * Valida un permiso y ejecuta una acción si tiene permiso, o muestra alert si no
     * @param {string} permiso - Nombre del permiso
     * @param {Function} callback - Función a ejecutar si tiene permiso
     * @param {string} accionDescripcion - Descripción de la acción para el mensaje de error
     */
    async function validarYEjecutar(permiso, callback, accionDescripcion) {
        const tiene = await tienePermiso(permiso);

        if (tiene) {
            callback();
        } else {
            mostrarAlertaNoPermiso(accionDescripcion);
        }
    }

    /**
     * Intercepta clicks en elementos con data-permiso
     * Si el usuario no tiene el permiso, muestra un alert y previene la acción
     */
    function interceptarClicks() {
        $(document).on('click', '[data-permiso]', async function(e) {
            const $elemento = $(this);
            const permiso = $elemento.data('permiso');
            const accion = $elemento.data('accion-descripcion') || 'realizar esta acción';

            // Si no hay permiso definido, dejar pasar
            if (!permiso) {
                return true;
            }

            // Verificar permiso
            const tiene = await tienePermiso(permiso);

            if (!tiene) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                mostrarAlertaNoPermiso(accion);
                return false;
            }

            // Si tiene permiso, dejar que el evento continúe
            return true;
        });
    }

    /**
     * Intercepta envíos de formularios con data-permiso
     */
    function interceptarFormularios() {
        $(document).on('submit', 'form[data-permiso]', async function(e) {
            const $form = $(this);
            const permiso = $form.data('permiso');
            const accion = $form.data('accion-descripcion') || 'enviar este formulario';

            if (!permiso) {
                return true;
            }

            const tiene = await tienePermiso(permiso);

            if (!tiene) {
                e.preventDefault();
                e.stopPropagation();
                mostrarAlertaNoPermiso(accion);
                return false;
            }

            return true;
        });
    }

    /**
     * Oculta o deshabilita elementos según permisos
     * Elementos con data-permiso-ocultar serán ocultados si no tiene permiso
     * Elementos con data-permiso-deshabilitar serán deshabilitados si no tiene permiso
     */
    async function aplicarVisibilidadYEstado() {
        // Ocultar elementos sin permiso
        $('[data-permiso-ocultar]').each(async function() {
            const $elemento = $(this);
            const permiso = $elemento.data('permiso-ocultar');
            const tiene = await tienePermiso(permiso);

            if (!tiene) {
                $elemento.hide();
            }
        });

        // Deshabilitar elementos sin permiso
        $('[data-permiso-deshabilitar]').each(async function() {
            const $elemento = $(this);
            const permiso = $elemento.data('permiso-deshabilitar');
            const tiene = await tienePermiso(permiso);

            if (!tiene) {
                $elemento.prop('disabled', true);
                $elemento.addClass('disabled');
                $elemento.attr('title', 'No tienes permiso para esta acción');
            }
        });
    }

    /**
     * Inicializa el sistema de validación de permisos
     * Debe llamarse cuando el DOM esté listo
     */
    function init() {
        // Precargar permisos
        cargarPermisos();

        // Interceptar clicks y formularios
        interceptarClicks();
        interceptarFormularios();

        // Aplicar visibilidad y estado
        aplicarVisibilidadYEstado();

        console.log('✅ Sistema de validación de permisos inicializado');
    }

    /**
     * Limpia el cache de permisos (útil después de cambiar de rol)
     */
    function limpiarCache() {
        permisosUsuario = null;
        cargando = false;
    }

    // API Pública
    return {
        init: init,
        tienePermiso: tienePermiso,
        tieneAlgunoDePermisos: tieneAlgunoDePermisos,
        tieneTodosLosPermisos: tieneTodosLosPermisos,
        validarYEjecutar: validarYEjecutar,
        mostrarAlertaNoPermiso: mostrarAlertaNoPermiso,
        limpiarCache: limpiarCache
    };
})();

// Auto-inicializar cuando jQuery esté listo
$(document).ready(function() {
    PermisosValidator.init();
});
