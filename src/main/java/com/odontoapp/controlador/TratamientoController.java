package com.odontoapp.controlador;

import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.TratamientoRealizado;
import com.odontoapp.servicio.TratamientoRealizadoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para la gestión de tratamientos realizados durante las citas.
 * Maneja el registro y eliminación de tratamientos.
 */
@Controller
@RequestMapping("/tratamientos")
public class TratamientoController {

    private final TratamientoRealizadoService tratamientoRealizadoService;

    public TratamientoController(TratamientoRealizadoService tratamientoRealizadoService) {
        this.tratamientoRealizadoService = tratamientoRealizadoService;
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
}
