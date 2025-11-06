package com.odontoapp.controlador;

import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.TratamientoRealizadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión de tratamientos realizados durante las citas.
 * Maneja el registro y eliminación de tratamientos.
 */
@Controller
@RequestMapping("/tratamientos")
public class TratamientoController {

    private final TratamientoRealizadoService tratamientoRealizadoService;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;
    private final CitaRepository citaRepository;
    private final ProcedimientoRepository procedimientoRepository;

    public TratamientoController(
            TratamientoRealizadoService tratamientoRealizadoService,
            TratamientoRealizadoRepository tratamientoRealizadoRepository,
            TratamientoPlanificadoRepository tratamientoPlanificadoRepository,
            CitaRepository citaRepository,
            ProcedimientoRepository procedimientoRepository) {
        this.tratamientoRealizadoService = tratamientoRealizadoService;
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.tratamientoPlanificadoRepository = tratamientoPlanificadoRepository;
        this.citaRepository = citaRepository;
        this.procedimientoRepository = procedimientoRepository;
    }

    /**
     * Registra un nuevo tratamiento realizado durante una cita.
     * Valida que el usuario que registra sea odontólogo y que la cita exista.
     *
     * @param dto DTO con datos del tratamiento
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario de citas
     */
    @PostMapping("/registrar")
    public String registrarTratamiento(@ModelAttribute TratamientoRealizadoDTO dto,
                                      RedirectAttributes attributes) {
        try {
            TratamientoRealizado tratamiento = tratamientoRealizadoService.registrarTratamiento(dto);
            attributes.addFlashAttribute("success",
                    "Tratamiento registrado con éxito en la cita #" + tratamiento.getCita().getId());
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error",
                    "Error en los datos del tratamiento: " + e.getMessage());
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error",
                    "Error al procesar el tratamiento: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al registrar el tratamiento: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Elimina un tratamiento realizado.
     * Esta operación es física (no soft delete).
     *
     * @param id ID del tratamiento a eliminar
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario de citas
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarTratamiento(@PathVariable Long id,
                                     RedirectAttributes attributes) {
        try {
            tratamientoRealizadoService.eliminarTratamiento(id);
            attributes.addFlashAttribute("success", "Tratamiento eliminado con éxito.");
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error",
                    "Error: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al eliminar el tratamiento: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Busca todos los tratamientos de una cita específica.
     * Útil para mostrar el historial de tratamientos en una cita.
     *
     * @param citaId ID de la cita
     * @return Lista de tratamientos en formato JSON
     */
    @GetMapping("/cita/{citaId}")
    @ResponseBody
    public java.util.List<TratamientoRealizado> getTratamientosPorCita(@PathVariable Long citaId) {
        return tratamientoRealizadoService.buscarPorCita(citaId);
    }

    /**
     * Registrar tratamiento realizado inmediatamente (Modal Avanzado)
     * Se ejecuta cuando el odontólogo hace el tratamiento en la cita actual
     */
    @PostMapping("/realizar-inmediato")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> realizarInmediato(@RequestBody Map<String, Object> datos) {
        try {
            // Extraer datos básicos
            Long citaId = Long.parseLong(datos.get("citaId").toString());
            Long procedimientoId = Long.parseLong(datos.get("procedimientoId").toString());
            String piezasDentales = (String) datos.get("piezasDentales");
            String descripcion = (String) datos.get("descripcion");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> camposDinamicos = (List<Map<String, String>>) datos.get("camposDinamicos");

            // Buscar entidades relacionadas
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                    .orElseThrow(() -> new RuntimeException("Procedimiento no encontrado"));

            // Construir descripción completa con campos dinámicos
            String descripcionCompleta = construirDescripcionCompleta(descripcion, camposDinamicos);

            // Crear tratamiento realizado
            TratamientoRealizado tratamiento = new TratamientoRealizado();
            tratamiento.setCita(cita);
            tratamiento.setProcedimiento(procedimiento);
            tratamiento.setOdontologo(cita.getOdontologo());
            tratamiento.setPiezaDental(piezasDentales); // Almacenar múltiples dientes separados por coma
            tratamiento.setDescripcionTrabajo(descripcionCompleta);
            tratamiento.setFechaRealizacion(LocalDateTime.now());

            // Guardar
            tratamientoRealizadoRepository.save(tratamiento);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Tratamiento registrado correctamente");
            response.put("tratamientoId", tratamiento.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", "Error al registrar tratamiento: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Planificar tratamiento para después (Modal Avanzado)
     * Se crea un registro de tratamiento pendiente asociado a la cita
     */
    @PostMapping("/planificar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> planificar(@RequestBody Map<String, Object> datos) {
        try {
            // Extraer datos básicos
            Long citaId = Long.parseLong(datos.get("citaId").toString());
            Long procedimientoId = Long.parseLong(datos.get("procedimientoId").toString());
            String piezasDentales = (String) datos.get("piezasDentales");
            String descripcion = (String) datos.get("descripcion");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> camposDinamicos = (List<Map<String, String>>) datos.get("camposDinamicos");

            // Buscar entidades relacionadas
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                    .orElseThrow(() -> new RuntimeException("Procedimiento no encontrado"));

            // Construir descripción completa con campos dinámicos
            String descripcionCompleta = construirDescripcionCompleta(descripcion, camposDinamicos);

            // Crear tratamiento planificado
            TratamientoPlanificado tratamiento = new TratamientoPlanificado();
            tratamiento.setPaciente(cita.getPaciente());
            tratamiento.setProcedimiento(procedimiento);
            tratamiento.setOdontologo(cita.getOdontologo());
            tratamiento.setPiezasDentales(piezasDentales);
            tratamiento.setDescripcion(descripcionCompleta);
            tratamiento.setEstado("PLANIFICADO");
            tratamiento.setNotas("Detectado en cita del " + cita.getFechaHoraInicio().toLocalDate());

            // Guardar
            tratamientoPlanificadoRepository.save(tratamiento);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Tratamiento planificado correctamente");
            response.put("tratamientoId", tratamiento.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", "Error al planificar tratamiento: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Construye una descripción completa combinando la descripción general
     * con los campos dinámicos del procedimiento
     */
    private String construirDescripcionCompleta(String descripcionBase, List<Map<String, String>> camposDinamicos) {
        StringBuilder descripcion = new StringBuilder();

        // Agregar descripción base si existe
        if (descripcionBase != null && !descripcionBase.trim().isEmpty()) {
            descripcion.append("OBSERVACIONES: ").append(descripcionBase).append("\n\n");
        }

        // Agregar campos dinámicos
        if (camposDinamicos != null && !camposDinamicos.isEmpty()) {
            descripcion.append("DETALLES DEL PROCEDIMIENTO:\n");
            for (Map<String, String> campo : camposDinamicos) {
                String nombre = campo.get("name");
                String valor = campo.get("value");
                if (valor != null && !valor.trim().isEmpty()) {
                    descripcion.append("- ").append(formatearNombreCampo(nombre))
                              .append(": ").append(valor).append("\n");
                }
            }
        }

        return descripcion.toString();
    }

    /**
     * Formatea el nombre del campo para hacerlo más legible
     * Ej: "numConductos" -> "Nº Conductos"
     */
    private String formatearNombreCampo(String nombre) {
        if (nombre == null) return "";

        // Diccionario de nombres comunes
        Map<String, String> nombres = new HashMap<>();
        nombres.put("motivo", "Motivo");
        nombres.put("diagnostico", "Diagnóstico");
        nombres.put("observaciones", "Observaciones");
        nombres.put("nivelDolor", "Nivel de Dolor");
        nombres.put("tratamientoInmediato", "Tratamiento Inmediato");
        nombres.put("numConductos", "Nº Conductos");
        nombres.put("tecnica", "Técnica");
        nombres.put("radiografia", "Radiografía");
        nombres.put("tipoBrackets", "Tipo de Brackets");
        nombres.put("arcada", "Arcada");
        nombres.put("planTratamiento", "Plan de Tratamiento");
        nombres.put("obturacion", "Obturación");
        nombres.put("actividades", "Actividades");
        nombres.put("avancePorcentaje", "Avance");
        nombres.put("proximoControl", "Próximo Control");
        nombres.put("nivelPlaca", "Nivel de Placa");
        nombres.put("fluor", "Aplicación de Flúor");
        nombres.put("cuadrantes", "Cuadrantes Tratados");
        nombres.put("anestesia", "Anestesia");
        nombres.put("profundidadBolsas", "Profundidad de Bolsas");
        nombres.put("marca", "Marca");
        nombres.put("diametro", "Diámetro");
        nombres.put("longitud", "Longitud");
        nombres.put("injertoOseo", "Injerto Óseo");
        nombres.put("torque", "Torque de Inserción");
        nombres.put("tipoCorona", "Tipo de Corona");
        nombres.put("color", "Color");
        nombres.put("fijacion", "Tipo de Fijación");
        nombres.put("tipoBlanqueamiento", "Tipo de Blanqueamiento");
        nombres.put("concentracion", "Concentración del Gel");
        nombres.put("sesiones", "Sesiones Programadas");
        nombres.put("material", "Material");
        nombres.put("tecnicaAdhesion", "Técnica de Adhesión");
        nombres.put("tipoExtraccion", "Tipo de Extracción");
        nombres.put("incision", "Incisión");
        nombres.put("osteotomia", "Osteotomía");
        nombres.put("odontoseccion", "Odontosección");
        nombres.put("sutura", "Sutura");
        nombres.put("puntosSutura", "Puntos de Sutura");

        return nombres.getOrDefault(nombre, capitalize(nombre));
    }

    /**
     * Capitaliza la primera letra de una cadena
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
