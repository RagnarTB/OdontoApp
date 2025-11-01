package com.odontoapp.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Servicio para obtener estadísticas del dashboard
 */
public interface DashboardService {

    /**
     * Obtiene las estadísticas generales del dashboard
     * @return Mapa con las estadísticas
     */
    Map<String, Object> obtenerEstadisticasGenerales();

    /**
     * Obtiene el número de citas para hoy
     * @return Número de citas
     */
    Long obtenerCitasDelDia();

    /**
     * Obtiene el número de pacientes nuevos del mes actual
     * @return Número de pacientes nuevos
     */
    Long obtenerPacientesNuevosDelMes();

    /**
     * Obtiene los ingresos pendientes de cobro
     * @return Total de ingresos pendientes
     */
    BigDecimal obtenerIngresosPendientes();

    /**
     * Obtiene los ingresos del mes actual
     * @return Total de ingresos del mes
     */
    BigDecimal obtenerIngresosDelMes();

    /**
     * Obtiene el total de pacientes activos
     * @return Total de pacientes
     */
    Long obtenerTotalPacientes();

    /**
     * Obtiene las próximas citas (hoy y mañana)
     * @return Lista de citas próximas
     */
    java.util.List<com.odontoapp.dto.CitaDTO> obtenerProximasCitas();

    /**
     * Obtiene los insumos con stock bajo
     * @return Lista de insumos con stock bajo
     */
    java.util.List<com.odontoapp.dto.InsumoDTO> obtenerInsumosStockBajo();
}
