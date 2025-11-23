package com.odontoapp.util;

/**
 * Clase de constantes para los permisos del sistema.
 * Centraliza todos los permisos para facilitar el mantenimiento y evitar errores de tipeo.
 */
public final class Permisos {

    private Permisos() {
        // Evitar instanciación
    }

    // ============== MÓDULO: USUARIOS ==============
    public static final String VER_LISTA_USUARIOS = "VER_LISTA_USUARIOS";
    public static final String VER_DETALLE_USUARIOS = "VER_DETALLE_USUARIOS";
    public static final String CREAR_USUARIOS = "CREAR_USUARIOS";
    public static final String EDITAR_USUARIOS = "EDITAR_USUARIOS";
    public static final String ELIMINAR_USUARIOS = "ELIMINAR_USUARIOS";
    public static final String RESTAURAR_USUARIOS = "RESTAURAR_USUARIOS";

    // ============== MÓDULO: ROLES ==============
    public static final String VER_LISTA_ROLES = "VER_LISTA_ROLES";
    public static final String VER_DETALLE_ROLES = "VER_DETALLE_ROLES";
    public static final String CREAR_ROLES = "CREAR_ROLES";
    public static final String EDITAR_ROLES = "EDITAR_ROLES";
    public static final String ELIMINAR_ROLES = "ELIMINAR_ROLES";
    public static final String RESTAURAR_ROLES = "RESTAURAR_ROLES";

    // ============== MÓDULO: PACIENTES ==============
    public static final String VER_LISTA_PACIENTES = "VER_LISTA_PACIENTES";
    public static final String VER_DETALLE_PACIENTES = "VER_DETALLE_PACIENTES";
    public static final String CREAR_PACIENTES = "CREAR_PACIENTES";
    public static final String EDITAR_PACIENTES = "EDITAR_PACIENTES";
    public static final String ELIMINAR_PACIENTES = "ELIMINAR_PACIENTES";
    public static final String RESTAURAR_PACIENTES = "RESTAURAR_PACIENTES";

    // ============== MÓDULO: CITAS ==============
    public static final String VER_LISTA_CITAS = "VER_LISTA_CITAS";
    public static final String VER_DETALLE_CITAS = "VER_DETALLE_CITAS";
    public static final String CREAR_CITAS = "CREAR_CITAS";
    public static final String EDITAR_CITAS = "EDITAR_CITAS"; // Incluye confirmar, cancelar, reprogramar
    public static final String ELIMINAR_CITAS = "ELIMINAR_CITAS";

    // ============== MÓDULO: SERVICIOS ==============
    public static final String VER_LISTA_SERVICIOS = "VER_LISTA_SERVICIOS";
    public static final String VER_DETALLE_SERVICIOS = "VER_DETALLE_SERVICIOS";
    public static final String CREAR_SERVICIOS = "CREAR_SERVICIOS";
    public static final String EDITAR_SERVICIOS = "EDITAR_SERVICIOS";
    public static final String ELIMINAR_SERVICIOS = "ELIMINAR_SERVICIOS";
    public static final String RESTAURAR_SERVICIOS = "RESTAURAR_SERVICIOS";

    // ============== MÓDULO: FACTURACIÓN ==============
    public static final String VER_LISTA_FACTURACION = "VER_LISTA_FACTURACION";
    public static final String VER_DETALLE_FACTURACION = "VER_DETALLE_FACTURACION";
    public static final String CREAR_FACTURACION = "CREAR_FACTURACION"; // Incluye POS y ventas directas
    public static final String EDITAR_FACTURACION = "EDITAR_FACTURACION"; // Incluye registrar pagos
    public static final String ELIMINAR_FACTURACION = "ELIMINAR_FACTURACION"; // Incluye anular comprobantes

    // ============== MÓDULO: INVENTARIO ==============
    public static final String VER_LISTA_INVENTARIO = "VER_LISTA_INVENTARIO";
    public static final String VER_DETALLE_INVENTARIO = "VER_DETALLE_INVENTARIO";
    public static final String CREAR_INVENTARIO = "CREAR_INVENTARIO";
    public static final String EDITAR_INVENTARIO = "EDITAR_INVENTARIO"; // Incluye registrar movimientos
    public static final String ELIMINAR_INVENTARIO = "ELIMINAR_INVENTARIO";
    public static final String RESTAURAR_INVENTARIO = "RESTAURAR_INVENTARIO";

    // ============== MÓDULO: TRATAMIENTOS ==============
    public static final String VER_LISTA_TRATAMIENTOS = "VER_LISTA_TRATAMIENTOS";
    public static final String VER_DETALLE_TRATAMIENTOS = "VER_DETALLE_TRATAMIENTOS";
    public static final String CREAR_TRATAMIENTOS = "CREAR_TRATAMIENTOS";
    public static final String EDITAR_TRATAMIENTOS = "EDITAR_TRATAMIENTOS";
    public static final String ELIMINAR_TRATAMIENTOS = "ELIMINAR_TRATAMIENTOS";

    // ============== MÓDULO: ODONTOGRAMA ==============
    public static final String VER_LISTA_ODONTOGRAMA = "VER_LISTA_ODONTOGRAMA";
    public static final String VER_DETALLE_ODONTOGRAMA = "VER_DETALLE_ODONTOGRAMA";
    public static final String CREAR_ODONTOGRAMA = "CREAR_ODONTOGRAMA";
    public static final String EDITAR_ODONTOGRAMA = "EDITAR_ODONTOGRAMA";
    public static final String ELIMINAR_ODONTOGRAMA = "ELIMINAR_ODONTOGRAMA";

    // ============== MÓDULO: ADMINISTRACIÓN ==============
    public static final String VER_REGISTROS_ELIMINADOS = "VER_REGISTROS_ELIMINADOS";
}
