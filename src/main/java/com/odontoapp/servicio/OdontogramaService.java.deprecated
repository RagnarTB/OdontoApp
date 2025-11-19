package com.odontoapp.servicio;

/**
 * Servicio para la gestión del odontograma (estado dental) de los pacientes.
 * Maneja el almacenamiento y recuperación del estado de las piezas dentales en formato JSON.
 */
public interface OdontogramaService {

    /**
     * Actualiza el estado del odontograma de un paciente.
     * El estado se almacena como JSON en el campo estadoOdontograma de la entidad Paciente.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @param estadoJson Estado del odontograma en formato JSON
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el paciente
     * @throws IllegalArgumentException si el JSON es inválido
     */
    void actualizarEstadoOdontograma(Long pacienteUsuarioId, String estadoJson);

    /**
     * Obtiene el estado actual del odontograma de un paciente.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return El estado del odontograma en formato JSON, o null si no tiene
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el paciente
     */
    String obtenerEstadoOdontograma(Long pacienteUsuarioId);

    /**
     * Valida que un JSON de odontograma tenga el formato correcto.
     *
     * @param estadoJson JSON a validar
     * @return true si el formato es válido, false en caso contrario
     */
    boolean validarFormatoOdontograma(String estadoJson);

    /**
     * Inicializa un odontograma vacío para un paciente nuevo.
     * Crea la estructura JSON base con todas las piezas dentales sin marcar.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return El JSON del odontograma inicializado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el paciente
     */
    String inicializarOdontograma(Long pacienteUsuarioId);
}
