package com.odontoapp.controlador;

import com.odontoapp.dto.ComprobanteDTO;
import com.odontoapp.dto.PagoDTO;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.entidad.Pago;
import com.odontoapp.repositorio.ComprobanteRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MetodoPagoRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.servicio.FacturacionService;
import com.odontoapp.util.Permisos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

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
    private final ComprobanteRepository comprobanteRepository;

    public FacturacionController(FacturacionService facturacionService,
                                PacienteRepository pacienteRepository,
                                ProcedimientoRepository procedimientoRepository,
                                InsumoRepository insumoRepository,
                                MetodoPagoRepository metodoPagoRepository,
                                ComprobanteRepository comprobanteRepository) {
        this.facturacionService = facturacionService;
        this.pacienteRepository = pacienteRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.comprobanteRepository = comprobanteRepository;
    }

    /**
     * Muestra la lista de comprobantes con paginación.
     *
     * @param model Modelo de Spring MVC
     * @param page Número de página (inicia en 0)
     * @return Vista de lista de facturación
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_FACTURACION)")
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
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_FACTURACION)")
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
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_FACTURACION)")
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
     * Registra un pago sobre un comprobante existente (versión AJAX).
     * Devuelve JSON para ser procesado por el frontend.
     *
     * @param dto DTO con datos del pago
     * @return ResponseEntity con resultado en JSON
     */
    @PostMapping("/registrar-pago")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_FACTURACION)")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarPago(@RequestBody PagoDTO dto) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar que el ID del comprobante esté presente
            if (dto.getComprobanteId() == null) {
                response.put("success", false);
                response.put("mensaje", "El ID del comprobante es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            // Registrar el pago
            Pago pago = facturacionService.registrarPago(dto);

            // Obtener el comprobante actualizado con su nuevo estado
            Comprobante comprobante = pago.getComprobante();

            response.put("success", true);
            response.put("mensaje", "Pago registrado con éxito");
            response.put("pagoId", pago.getId());
            response.put("comprobanteId", comprobante.getId());
            response.put("estadoPago", comprobante.getEstadoPago().getNombre()); // Para detectar si está PAGADO_TOTAL

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("mensaje", "Error en los datos del pago: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("mensaje", "Error al procesar el pago: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("mensaje", "Error inesperado al registrar el pago: " + e.getMessage());
            System.err.println("Error en registrarPago: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
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
    @PostMapping("/anular/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_FACTURACION)")
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
     * Anula un comprobante con devolución selectiva de insumos (AJAX).
     * Permite seleccionar qué insumos y qué cantidades devolver al inventario.
     *
     * @param id ID del comprobante a anular
     * @param request Objeto con lista de insumos a devolver
     * @return ResponseEntity con resultado en JSON
     */
    @PostMapping("/anular-con-devolucion/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_FACTURACION)")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> anularComprobanteConDevolucion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extraer lista de insumos del request
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> insumosRequest =
                (java.util.List<Map<String, Object>>) request.get("insumos");

            if (insumosRequest == null || insumosRequest.isEmpty()) {
                response.put("success", false);
                response.put("mensaje", "Debe seleccionar al menos un insumo para devolver");
                return ResponseEntity.badRequest().body(response);
            }

            // Convertir a mapa insumoId → cantidad
            Map<Long, java.math.BigDecimal> insumosADevolver = new HashMap<>();

            for (Map<String, Object> insumoData : insumosRequest) {
                try {
                    Long insumoId = Long.parseLong(insumoData.get("insumoId").toString());
                    java.math.BigDecimal cantidad = new java.math.BigDecimal(insumoData.get("cantidad").toString());

                    if (cantidad.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        response.put("success", false);
                        response.put("mensaje", "Las cantidades deben ser mayores a 0");
                        return ResponseEntity.badRequest().body(response);
                    }

                    insumosADevolver.put(insumoId, cantidad);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("mensaje", "Error al procesar los datos de insumos: " + e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Llamar al servicio para anular con devolución selectiva
            Comprobante comprobante = facturacionService.anularComprobanteConDevolucionSelectiva(
                id,
                "Anulado por usuario con devolución selectiva de insumos",
                insumosADevolver
            );

            response.put("success", true);
            response.put("mensaje", "Comprobante " + comprobante.getNumeroComprobante() +
                                   " anulado con éxito. Se devolvieron " + insumosADevolver.size() +
                                   " insumo(s) al inventario.");
            response.put("comprobanteId", comprobante.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("mensaje", "Error al anular el comprobante: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("mensaje", "Error inesperado: " + e.getMessage());
            System.err.println("Error en anularComprobanteConDevolucion: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
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
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_FACTURACION)")
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
     * Obtiene los detalles del comprobante en formato JSON para el modal de anulación.
     *
     * @param id ID del comprobante
     * @return Map con los detalles del comprobante en formato JSON
     */
    @GetMapping("/detalle/{id}/json")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_FACTURACION)")
    @ResponseBody
    public Map<String, Object> obtenerDetallesJSON(@PathVariable Long id) {
        try {
            var comprobanteOpt = facturacionService.buscarComprobantePorId(id);

            if (comprobanteOpt.isEmpty()) {
                return Map.of("error", "Comprobante no encontrado");
            }

            Comprobante comprobante = comprobanteOpt.get();

            // Crear lista de detalles simplificada para el JSON
            var detalles = comprobante.getDetalles().stream()
                    .map(detalle -> Map.of(
                            "tipoItem", detalle.getTipoItem(),
                            "descripcionItem", detalle.getDescripcionItem(),
                            "cantidad", detalle.getCantidad(),
                            "unidad", "ud" // Podríamos obtener la unidad real si está disponible
                    ))
                    .toList();

            return Map.of(
                    "id", comprobante.getId(),
                    "numeroComprobante", comprobante.getNumeroComprobante(),
                    "detalles", detalles
            );

        } catch (Exception e) {
            return Map.of("error", "Error al obtener detalles: " + e.getMessage());
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
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_FACTURACION)")
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
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_FACTURACION)")
    public String imprimirComprobante(@PathVariable Long id,
                                     Model model,
                                     RedirectAttributes attributes) {
        try {
            // Obtener el comprobante con todas las relaciones cargadas (EAGER)
            // para evitar LazyInitializationException en la vista
            Comprobante comprobante = comprobanteRepository.findByIdWithAllRelations(id)
                    .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

            // Añadir al modelo
            model.addAttribute("comprobante", comprobante);

            return "modulos/facturacion/imprimir";

        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error al cargar el comprobante para impresión: " + e.getMessage());
            e.printStackTrace(); // Para debug en consola
            return "redirect:/facturacion";
        }
    }
}
