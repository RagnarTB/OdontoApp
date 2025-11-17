package com.odontoapp.controlador;

import com.odontoapp.dto.ComprobanteDTO;
import com.odontoapp.dto.PagoDTO;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MetodoPagoRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.servicio.FacturacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para la gestión de facturación y pagos.
 * Maneja la generación de comprobantes, registro de pagos y anulaciones.
 */
@Controller
@RequestMapping("/facturacion")
public class FacturacionController {

    private final FacturacionService facturacionService;
    private final PacienteRepository pacienteRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final InsumoRepository insumoRepository;
    private final MetodoPagoRepository metodoPagoRepository;

    public FacturacionController(FacturacionService facturacionService,
                                PacienteRepository pacienteRepository,
                                ProcedimientoRepository procedimientoRepository,
                                InsumoRepository insumoRepository,
                                MetodoPagoRepository metodoPagoRepository) {
        this.facturacionService = facturacionService;
        this.pacienteRepository = pacienteRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
    }

    /**
     * Muestra la lista de comprobantes con paginación.
     *
     * @param model Modelo de Spring MVC
     * @param page Número de página (inicia en 0)
     * @return Vista de lista de facturación
     */
    @GetMapping
    public String verFacturacion(Model model,
                                @RequestParam(defaultValue = "0") int page) {
        // Crear paginación (10 comprobantes por página)
        Pageable pageable = PageRequest.of(page, 10);

        // Buscar comprobantes pendientes
        Page<Comprobante> comprobantes = facturacionService.buscarComprobantesPendientes(pageable);

        // Añadir al modelo
        model.addAttribute("comprobantes", comprobantes);
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", comprobantes.getTotalPages());

        return "modulos/facturacion/lista";
    }

    /**
     * Muestra la vista de Punto de Venta (POS) para ventas directas.
     *
     * @param model Modelo de Spring MVC
     * @return Vista del POS
     */
    @GetMapping("/pos")
    public String verPOS(Model model) {
        // Buscar todos los pacientes
        var listaPacientes = pacienteRepository.findAll();

        // Buscar todos los procedimientos
        var listaProcedimientos = procedimientoRepository.findAll();

        // Buscar todos los insumos
        var listaInsumos = insumoRepository.findAll();

        // Buscar métodos de pago
        var listaMetodosPago = metodoPagoRepository.findAll();

        // Añadir al modelo
        model.addAttribute("comprobanteDTO", new ComprobanteDTO());
        model.addAttribute("listaPacientes", listaPacientes);
        model.addAttribute("listaProcedimientos", listaProcedimientos);
        model.addAttribute("listaInsumos", listaInsumos);
        model.addAttribute("listaMetodosPago", listaMetodosPago);

        return "modulos/facturacion/pos";
    }

    /**
     * Genera un comprobante de venta directa (sin cita).
     * Opcionalmente puede generar y registrar el pago en la misma operación.
     *
     * @param dto DTO con datos del comprobante
     * @param generarPago Si se debe registrar el pago simultáneamente
     * @param metodoPagoId ID del método de pago (si generarPago = true)
     * @param montoPago Monto del pago (si generarPago = true)
     * @param montoEfectivo Monto en efectivo (para pago mixto)
     * @param montoYape Monto Yape (para pago mixto)
     * @param referenciaYape Referencia de Yape/Transferencia
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la lista de facturación
     */
    @PostMapping("/generar-venta-directa")
    public String generarVentaDirecta(@ModelAttribute ComprobanteDTO dto,
                                     @RequestParam(required = false) Boolean generarPago,
                                     @RequestParam(required = false) Long metodoPagoId,
                                     @RequestParam(required = false) java.math.BigDecimal montoPago,
                                     @RequestParam(required = false) java.math.BigDecimal montoEfectivo,
                                     @RequestParam(required = false) java.math.BigDecimal montoYape,
                                     @RequestParam(required = false) String referenciaYape,
                                     RedirectAttributes attributes) {
        try {
            // 1. Generar comprobante
            Comprobante comprobante = facturacionService.generarComprobanteVentaDirecta(dto);

            // 2. Si se solicitó generar pago, registrarlo
            if (generarPago != null && generarPago && metodoPagoId != null && montoPago != null) {
                try {
                    PagoDTO pagoDTO = new PagoDTO();
                    pagoDTO.setComprobanteId(comprobante.getId());
                    pagoDTO.setMetodoPagoId(metodoPagoId);
                    pagoDTO.setMonto(montoPago);
                    pagoDTO.setFechaPago(java.time.LocalDateTime.now());
                    pagoDTO.setMontoEfectivo(montoEfectivo);
                    pagoDTO.setMontoYape(montoYape);
                    pagoDTO.setReferenciaYape(referenciaYape);
                    pagoDTO.setNotas("Pago registrado desde POS");

                    facturacionService.registrarPago(pagoDTO);

                    attributes.addFlashAttribute("success",
                            "Comprobante " + comprobante.getNumeroComprobante() + " generado y pagado con éxito. Estado: PAGADO.");
                } catch (Exception e) {
                    attributes.addFlashAttribute("warning",
                            "Comprobante " + comprobante.getNumeroComprobante() + " generado, pero hubo un error al registrar el pago: " + e.getMessage());
                }
            } else {
                attributes.addFlashAttribute("success",
                        "Comprobante " + comprobante.getNumeroComprobante() + " generado con éxito.");
            }

        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error",
                    "Error en los datos del comprobante: " + e.getMessage());
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error",
                    "Error al procesar la venta: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al generar el comprobante: " + e.getMessage());
        }

        return "redirect:/facturacion";
    }

    /**
     * Registra un pago sobre un comprobante existente.
     *
     * @param dto DTO con datos del pago
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la lista de facturación
     */
    @PostMapping("/registrar-pago")
    public String registrarPago(@ModelAttribute PagoDTO dto,
                               RedirectAttributes attributes) {
        try {
            facturacionService.registrarPago(dto);
            attributes.addFlashAttribute("success", "Pago registrado con éxito.");
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error",
                    "Error en los datos del pago: " + e.getMessage());
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error",
                    "Error al procesar el pago: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al registrar el pago: " + e.getMessage());
        }

        return "redirect:/facturacion";
    }

    /**
     * Anula un comprobante existente.
     * Solo permite anular comprobantes sin pagos registrados.
     *
     * @param id ID del comprobante a anular
     * @param motivo Motivo de la anulación
     * @param regresarInventario Si se debe regresar el inventario (true/false)
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la lista de facturación
     */
    @GetMapping("/anular/{id}")
    public String anularComprobante(@PathVariable Long id,
                                   @RequestParam(defaultValue = "Anulado por usuario") String motivo,
                                   @RequestParam(required = false) Boolean regresarInventario,
                                   RedirectAttributes attributes) {
        try {
            Comprobante comprobante = facturacionService.anularComprobante(
                    id,
                    motivo,
                    regresarInventario != null && regresarInventario
            );

            String mensaje = "Comprobante " + comprobante.getNumeroComprobante() + " anulado con éxito.";
            if (regresarInventario != null && regresarInventario) {
                mensaje += " Los insumos han sido regresados al inventario.";
            }

            attributes.addFlashAttribute("success", mensaje);
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error",
                    "Error al anular el comprobante: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al anular el comprobante: " + e.getMessage());
        }

        return "redirect:/facturacion";
    }

    /**
     * Muestra el detalle de un comprobante específico.
     *
     * @param id ID del comprobante
     * @param model Modelo de Spring MVC
     * @param attributes Atributos para mensajes flash
     * @return Vista de detalle del comprobante
     */
    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id,
                            Model model,
                            RedirectAttributes attributes) {
        try {
            var comprobanteOpt = facturacionService.buscarComprobantePorId(id);

            if (comprobanteOpt.isEmpty()) {
                attributes.addFlashAttribute("error", "Comprobante no encontrado.");
                return "redirect:/facturacion";
            }

            Comprobante comprobante = comprobanteOpt.get();
            var pagos = facturacionService.buscarPagosPorComprobante(id);
            var listaMetodosPago = metodoPagoRepository.findAll();

            model.addAttribute("comprobante", comprobante);
            model.addAttribute("pagos", pagos);
            model.addAttribute("pagoDTO", new PagoDTO());
            model.addAttribute("listaMetodosPago", listaMetodosPago);

            return "modulos/facturacion/detalle";

        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error al cargar el detalle: " + e.getMessage());
            return "redirect:/facturacion";
        }
    }

    /**
     * Busca comprobantes de un paciente específico.
     *
     * @param pacienteId ID del paciente
     * @param model Modelo de Spring MVC
     * @param page Número de página
     * @return Vista de lista de facturación filtrada
     */
    @GetMapping("/paciente/{pacienteId}")
    public String verComprobantesPorPaciente(@PathVariable Long pacienteId,
                                            Model model,
                                            @RequestParam(defaultValue = "0") int page) {
        // Crear paginación
        Pageable pageable = PageRequest.of(page, 10);

        // Buscar comprobantes del paciente
        Page<Comprobante> comprobantes =
                facturacionService.buscarComprobantesPorPaciente(pacienteId, pageable);

        // Añadir al modelo
        model.addAttribute("comprobantes", comprobantes);
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", comprobantes.getTotalPages());
        model.addAttribute("pacienteId", pacienteId);

        return "modulos/facturacion/lista";
    }

    /**
     * Genera una vista de impresión del comprobante.
     *
     * @param id ID del comprobante
     * @param model Modelo de Spring MVC
     * @param attributes Atributos de redirección
     * @return Vista de impresión del comprobante
     */
    @GetMapping("/imprimir/{id}")
    public String imprimirComprobante(@PathVariable Long id,
                                     Model model,
                                     RedirectAttributes attributes) {
        try {
            // Obtener el comprobante con todos sus detalles
            Comprobante comprobante = facturacionService.buscarComprobantePorId(id)
                    .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

            // Añadir al modelo
            model.addAttribute("comprobante", comprobante);

            return "modulos/facturacion/imprimir";

        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error al cargar el comprobante para impresión: " + e.getMessage());
            return "redirect:/facturacion";
        }
    }
}
