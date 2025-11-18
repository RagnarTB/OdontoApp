package com.odontoapp.servicio;

import com.odontoapp.dto.CitaDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.TratamientoPlanificado;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar el dashboard del portal de pacientes.
 * Proporciona estadísticas y datos específicos del paciente autenticado.
 */
public interface PacienteDashboardService {

    /**
     * Obtiene todas las estadísticas del dashboard para el paciente especificado
     * @param usuarioId ID del usuario paciente
     * @return Mapa con todas las estadísticas
     */
    Map<String, Object> obtenerEstadisticasPaciente(Long usuarioId);

    /**
     * Obtiene las próximas 3 citas del paciente (CONFIRMADA o PENDIENTE)
     * @param usuarioId ID del usuario paciente
     * @return Lista de hasta 3 próximas citas
     */
    List<CitaDTO> obtenerProximasCitas(Long usuarioId);

    /**
     * Obtiene la última cita realizada por el paciente (estado ASISTIO)
     * @param usuarioId ID del usuario paciente
     * @return Última cita realizada o null si no hay
     */
    Cita obtenerUltimaCitaRealizada(Long usuarioId);

    /**
     * Obtiene los tratamientos en curso del paciente (PLANIFICADO o EN_CURSO)
     * @param usuarioId ID del usuario paciente
     * @return Lista de tratamientos pendientes
     */
    List<TratamientoPlanificado> obtenerTratamientosEnCurso(Long usuarioId);

    /**
     * Obtiene el saldo pendiente total del paciente sumando todos los comprobantes pendientes
     * @param usuarioId ID del usuario paciente
     * @return Saldo pendiente total
     */
    BigDecimal obtenerSaldoPendiente(Long usuarioId);

    /**
     * Obtiene el número de citas canceladas del paciente (últimos 6 meses)
     * @param usuarioId ID del usuario paciente
     * @return Cantidad de citas canceladas
     */
    Long obtenerCitasCanceladas(Long usuarioId);
}
