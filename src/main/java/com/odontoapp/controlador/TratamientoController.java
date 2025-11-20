package com.odontoapp.controlador;

import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.TratamientoRealizadoService;
import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final DetalleComprobanteRepository detalleComprobanteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final OdontogramaDienteService odontogramaService;

    public TratamientoController(
            TratamientoRealizadoService tratamientoRealizadoService,
            TratamientoRealizadoRepository tratamientoRealizadoRepository,
            TratamientoPlanificadoRepository tratamientoPlanificadoRepository,
            CitaRepository citaRepository,
            ProcedimientoRepository procedimientoRepository,
            ProcedimientoInsumoRepository procedimientoInsumoRepository,
            InsumoRepository insumoRepository,
            ComprobanteRepository comprobanteRepository,
            EstadoPagoRepository estadoPagoRepository,
            DetalleComprobanteRepository detalleComprobanteRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository,
            EstadoCitaRepository estadoCitaRepository,
            OdontogramaDienteService odontogramaService) {
        this.tratamientoRealizadoService = tratamientoRealizadoService;
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.tratamientoPlanificadoRepository = tratamientoPlanificadoRepository;
        this.citaRepository = citaRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.insumoRepository = insumoRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.detalleComprobanteRepository = detalleComprobanteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.odontogramaService = odontogramaService;
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
     * Obtiene la lista completa de insumos disponibles
     * Para usar en el modal de registro de tratamiento
     */
    @GetMapping("/insumos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getInsumosDisponibles() {
        List<Insumo> insumos = insumoRepository.findAllWithRelations();

        List<Map<String, Object>> insumosDTO = insumos.stream()
                .map(insumo -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", insumo.getId());
                    dto.put("codigo", insumo.getCodigo());
                    dto.put("nombre", insumo.getNombre());
                    dto.put("unidad", insumo.getUnidadMedida() != null ?
                            insumo.getUnidadMedida().getAbreviatura() : "Unid.");
                    dto.put("precioUnitario", insumo.getPrecioUnitario());
                    dto.put("stockActual", insumo.getStockActual());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(insumosDTO);
    }

    /**
     * Verifica si una cita ya tiene un tratamiento realizado
     * Retorna true si ya existe, false si se puede agregar
     */
    @GetMapping("/verificar-cita/{citaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarTratamientoCita(@PathVariable Long citaId) {
        List<TratamientoRealizado> tratamientos = tratamientoRealizadoRepository.findByCitaId(citaId);

        Map<String, Object> response = new HashMap<>();
        response.put("tieneTratamiento", !tratamientos.isEmpty());
        response.put("cantidad", tratamientos.size());

        if (!tratamientos.isEmpty()) {
            TratamientoRealizado tratamiento = tratamientos.get(0);
            response.put("procedimiento", tratamiento.getProcedimiento().getNombre());
            response.put("fecha", tratamiento.getFechaRealizacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Registrar tratamiento realizado inmediatamente (Modal Avanzado)
     * Se ejecuta cuando el odontólogo hace el tratamiento en la cita actual
     * Genera automáticamente un comprobante
     */
    @PostMapping("/realizar-inmediato")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> realizarInmediato(@RequestBody Map<String, Object> datos) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📥 ENDPOINT /realizar-inmediato INICIADO");
            System.out.println("=".repeat(80));
            System.out.println("📦 Datos RAW recibidos: " + datos);

            // Extraer datos básicos
            Long citaId = Long.parseLong(datos.get("citaId").toString());
            Long procedimientoId = Long.parseLong(datos.get("procedimientoId").toString());
            String piezasDentales = (String) datos.get("piezasDentales");
            String descripcion = (String) datos.get("descripcion");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> camposDinamicos = (List<Map<String, String>>) datos.get("camposDinamicos");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> insumosTotales = (List<Map<String, Object>>) datos.get("insumosTotales");

            // Extraer ID del tratamiento planificado (si viene de un flujo planificado->realizado)
            Long tratamientoPlanificadoId = datos.get("tratamientoPlanificadoId") != null
                ? Long.parseLong(datos.get("tratamientoPlanificadoId").toString())
                : null;

            // **BUSCAR TRATAMIENTO PLANIFICADO ASOCIADO**
            TratamientoPlanificado planificado = null;

            // Intento 1: Si viene el ID explícito del tratamiento planificado, usarlo
            if (tratamientoPlanificadoId != null) {
                planificado = tratamientoPlanificadoRepository.findById(tratamientoPlanificadoId)
                        .orElse(null);
                if (planificado != null) {
                    System.out.println("✓ Tratamiento planificado encontrado por ID: " + tratamientoPlanificadoId);
                }
            }

            // Intento 2: Si no se encuentra por ID, buscar por cita asociada
            if (planificado == null) {
                planificado = tratamientoPlanificadoRepository.findByCitaAsociadaId(citaId);
                if (planificado != null) {
                    System.out.println("✓ Tratamiento planificado encontrado por cita asociada: " + citaId);
                }
            }

            // Intento 3: Si no se encuentra por cita, buscar por paciente + procedimiento + estado
            if (planificado == null) {
                List<TratamientoPlanificado> tratamientosPendientes =
                    tratamientoPlanificadoRepository.findByPacienteAndProcedimientoAndEstado(
                        cita.getPaciente(),
                        procedimiento,
                        "PLANIFICADO"
                    );

                if (tratamientosPendientes.isEmpty()) {
                    tratamientosPendientes = tratamientoPlanificadoRepository.findByPacienteAndProcedimientoAndEstado(
                        cita.getPaciente(),
                        procedimiento,
                        "EN_CURSO"
                    );
                }

                if (!tratamientosPendientes.isEmpty()) {
                    planificado = tratamientosPendientes.get(0); // Tomar el más reciente
                    System.out.println("✓ Tratamiento planificado encontrado por paciente + procedimiento: " + planificado.getId());
                }
            }

            // **SI SE ENCONTRÓ UN TRATAMIENTO PLANIFICADO, USAR SUS INSUMOS**
            if (planificado != null && planificado.getInsumosJson() != null && !planificado.getInsumosJson().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> insumosGuardados = mapper.readValue(
                            planificado.getInsumosJson(),
                            List.class
                    );
                    insumosTotales = insumosGuardados;
                    tratamientoPlanificadoId = planificado.getId(); // Actualizar el ID para usarlo más adelante
                    System.out.println("✓ Cargados " + insumosTotales.size() + " insumos desde tratamiento planificado ID " + planificado.getId());
                } catch (Exception e) {
                    System.err.println("⚠️ Error al deserializar insumos del JSON: " + e.getMessage());
                    // Continuar con los insumos del request si falla la deserialización
                }
            } else {
                System.out.println("ℹ️ No se encontró tratamiento planificado con insumos guardados");
            }

            System.out.println("\n📊 DATOS PROCESADOS:");
            System.out.println("  ├─ Cita ID: " + citaId);
            System.out.println("  ├─ Procedimiento ID: " + procedimientoId);
            System.out.println("  ├─ Piezas Dentales: " + piezasDentales);
            System.out.println("  ├─ Tratamiento Planificado ID: " + tratamientoPlanificadoId);
            System.out.println("  └─ Insumos Totales: " + (insumosTotales != null ? insumosTotales.size() : 0) + " items");

            if (insumosTotales != null && !insumosTotales.isEmpty()) {
                System.out.println("\n📦 DETALLE DE INSUMOS RECIBIDOS:");
                for (int i = 0; i < insumosTotales.size(); i++) {
                    Map<String, Object> insumo = insumosTotales.get(i);
                    System.out.println("  [" + (i+1) + "] Insumo ID: " + insumo.get("insumoId") +
                                     ", Cantidad: " + insumo.get("cantidad"));
                }
            } else {
                System.out.println("\n⚠️ ADVERTENCIA: No se recibieron insumos o la lista está vacía");
            }
            System.out.println();

            // Buscar entidades relacionadas
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                    .orElseThrow(() -> new RuntimeException("Procedimiento no encontrado"));

            // Construir descripción completa con campos dinámicos
            String descripcionCompleta = construirDescripcionCompleta(descripcion, camposDinamicos);

            // Verificar si ya existe un TratamientoRealizado para esta cita (creado automáticamente al marcar asistencia)
            List<TratamientoRealizado> tratamientosExistentes = tratamientoRealizadoRepository.findByCitaId(citaId);
            TratamientoRealizado tratamiento;

            if (!tratamientosExistentes.isEmpty()) {
                // Ya existe un tratamiento mínimo → ACTUALIZARLO con los detalles del modal
                tratamiento = tratamientosExistentes.get(0);
                System.out.println("✓ Tratamiento existente encontrado (ID: " + tratamiento.getId() + ") - Actualizando con detalles del modal...");

                // Actualizar con los datos detallados del modal
                tratamiento.setProcedimiento(procedimiento); // Actualizar procedimiento por si cambió
                tratamiento.setPiezaDental(piezasDentales);
                tratamiento.setDescripcionTrabajo(descripcionCompleta);
                // Mantener la fecha original de cuando se marcó asistencia
            } else {
                // No existe tratamiento → Crear uno nuevo
                System.out.println("ℹ️ No existe tratamiento previo - Creando nuevo TratamientoRealizado...");
                tratamiento = new TratamientoRealizado();
                tratamiento.setCita(cita);
                tratamiento.setProcedimiento(procedimiento);
                tratamiento.setOdontologo(cita.getOdontologo());
                tratamiento.setPiezaDental(piezasDentales);
                tratamiento.setDescripcionTrabajo(descripcionCompleta);
                tratamiento.setFechaRealizacion(LocalDateTime.now());
            }

            // Guardar tratamiento (actualizado o nuevo)
            tratamientoRealizadoRepository.save(tratamiento);
            System.out.println("✅ TratamientoRealizado guardado con ID: " + tratamiento.getId());

            // **ACTUALIZAR ODONTOGRAMA AUTOMÁTICAMENTE**
            try {
                odontogramaService.actualizarDesdeTratamiento(tratamiento.getId());
                System.out.println("✓ Odontograma actualizado automáticamente para tratamiento ID: " + tratamiento.getId());
            } catch (Exception e) {
                System.err.println("⚠ Error al actualizar odontograma: " + e.getMessage());
                // No fallar el tratamiento si falla la actualización del odontograma
            }

            // **ACTUALIZAR TRATAMIENTO PLANIFICADO SI EXISTE (FLUJO PLANIFICADO->REALIZADO)**
            // Usar el ID explícito enviado desde el frontend (más preciso y confiable)
            if (tratamientoPlanificadoId != null) {
                TratamientoPlanificado planificado = tratamientoPlanificadoRepository.findById(tratamientoPlanificadoId)
                        .orElse(null);

                if (planificado != null) {
                    planificado.setEstado("COMPLETADO");
                    planificado.setTratamientoRealizadoId(tratamiento.getId());
                    tratamientoPlanificadoRepository.save(planificado);
                    System.out.println("✓ Tratamiento planificado ID " + planificado.getId() +
                            " marcado como COMPLETADO y vinculado a TratamientoRealizado ID " + tratamiento.getId());
                } else {
                    System.err.println("⚠️ No se encontró TratamientoPlanificado con ID " + tratamientoPlanificadoId);
                }
            } else {
                System.out.println("ℹ️ Tratamiento directo (sin planificación previa)");
            }

            // **CREAR CITA AUTOMÁTICA EN EL CALENDARIO**
            // Crear una cita automática que bloquee el tiempo en el que se realiza el tratamiento
            LocalDateTime inicioTratamiento = cita.getFechaHoraFin(); // Inicia después de la cita original
            LocalDateTime finTratamiento = inicioTratamiento.plusMinutes(procedimiento.getDuracionBaseMinutos());

            // Obtener estado "COMPLETADA" para la nueva cita
            EstadoCita estadoCompletada = estadoCitaRepository.findByNombre("COMPLETADA")
                    .orElseGet(() -> estadoCitaRepository.findByNombre("ASISTIO")
                            .orElseThrow(() -> new RuntimeException("No se encontró un estado válido para la cita")));

            // Crear nueva cita automática
            Cita citaTratamiento = new Cita();
            citaTratamiento.setPaciente(cita.getPaciente());
            citaTratamiento.setOdontologo(cita.getOdontologo());
            citaTratamiento.setProcedimiento(procedimiento);
            citaTratamiento.setFechaHoraInicio(inicioTratamiento);
            citaTratamiento.setFechaHoraFin(finTratamiento);
            citaTratamiento.setDuracionEstimadaMinutos(procedimiento.getDuracionBaseMinutos());
            citaTratamiento.setMotivoConsulta("Tratamiento realizado: " + procedimiento.getNombre());
            citaTratamiento.setEstadoCita(estadoCompletada);
            citaTratamiento.setNotas("Cita generada automáticamente al registrar tratamiento inmediato");
            citaTratamiento.setCitaGeneradaPorTratamiento(cita); // ✅ VINCULAR CON CITA ORIGEN (CADENA)

            // Guardar la nueva cita
            citaRepository.save(citaTratamiento);
            System.out.println("✓ Cita generada y vinculada: Cita #" + cita.getId() + " → Cita #" + citaTratamiento.getId());

            // **DESCONTAR INSUMOS USANDO LA LISTA UNIFICADA DEL FRONTEND**
            // La lista insumosTotales contiene todos los insumos con cantidades modificadas por el usuario
            if (insumosTotales != null && !insumosTotales.isEmpty()) {
                System.out.println("✓ Procesando " + insumosTotales.size() + " insumos del frontend");

                for (Map<String, Object> insumoData : insumosTotales) {
                    try {
                        Long insumoId = Long.parseLong(insumoData.get("insumoId").toString());
                        BigDecimal cantidad = new BigDecimal(insumoData.get("cantidad").toString());

                        // Buscar el insumo
                        Insumo insumo = insumoRepository.findById(insumoId)
                                .orElseThrow(() -> new RuntimeException("Insumo ID " + insumoId + " no encontrado"));

                        // Validar stock disponible
                        if (insumo.getStockActual().compareTo(cantidad) < 0) {
                            throw new RuntimeException(
                                    String.format("Stock insuficiente del insumo '%s'. " +
                                            "Disponible: %.2f %s, Requerido: %.2f %s",
                                            insumo.getNombre(),
                                            insumo.getStockActual(),
                                            insumo.getUnidadMedida().getAbreviatura(),
                                            cantidad,
                                            insumo.getUnidadMedida().getAbreviatura()));
                        }

                        // Descontar stock y registrar movimiento
                        String referencia = "Cita #" + cita.getId() + " - Tratamiento inmediato";
                        registrarUsoInsumo(insumo, cantidad, referencia);

                        System.out.println("  ✓ " + insumo.getNombre() +
                                " | Cantidad: " + cantidad + " " + insumo.getUnidadMedida().getAbreviatura() +
                                " | Nuevo stock: " + insumo.getStockActual());

                    } catch (Exception e) {
                        System.err.println("Error procesando insumo: " + e.getMessage());
                        throw new RuntimeException("Error al procesar insumo: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("ℹ️ No hay insumos para descontar");
            }

            // **OBTENER O GENERAR COMPROBANTE**
            System.out.println("\n🧾 PROCESAMIENTO DE COMPROBANTE:");
            // Verificar si ya existe un comprobante para esta cita
            Optional<Comprobante> comprobanteExistente = comprobanteRepository.findByCitaId(citaId);
            Comprobante comprobante;

            if (comprobanteExistente.isPresent()) {
                // Reutilizar el comprobante existente
                comprobante = comprobanteExistente.get();
                System.out.println("  ✓ Comprobante EXISTENTE encontrado: #" + comprobante.getId() +
                                 " (" + comprobante.getNumeroComprobante() + ")");

                // ✅ AGREGAR EL TRATAMIENTO REALIZADO AL COMPROBANTE CON SU PRECIO
                BigDecimal precioTratamiento = procedimiento.getPrecio() != null ? procedimiento.getPrecio() : BigDecimal.ZERO;

                DetalleComprobante detalleTratamiento = new DetalleComprobante();
                detalleTratamiento.setComprobante(comprobante);
                detalleTratamiento.setTipoItem("TRATAMIENTO");
                detalleTratamiento.setItemId(tratamiento.getId());
                detalleTratamiento.setDescripcionItem(procedimiento.getCodigo() + " - " + procedimiento.getNombre());
                detalleTratamiento.setCantidad(BigDecimal.ONE);
                detalleTratamiento.setPrecioUnitario(precioTratamiento);
                detalleTratamiento.setSubtotal(precioTratamiento);
                detalleComprobanteRepository.save(detalleTratamiento);

                System.out.println("  ✅ Tratamiento agregado: " + procedimiento.getNombre() +
                                 " | Precio: S/ " + precioTratamiento);

                // ✅ ACTUALIZAR MONTO TOTAL DEL COMPROBANTE
                BigDecimal nuevoMontoTotal = comprobante.getMontoTotal().add(precioTratamiento);
                BigDecimal nuevoMontoPendiente = comprobante.getMontoPendiente().add(precioTratamiento);
                comprobante.setMontoTotal(nuevoMontoTotal);
                comprobante.setMontoPendiente(nuevoMontoPendiente);
                comprobanteRepository.save(comprobante);

                System.out.println("  ✅ Monto total actualizado: S/ " + nuevoMontoTotal);

                // AGREGAR: Crear detalles de insumos para este nuevo tratamiento
                if (insumosTotales != null && !insumosTotales.isEmpty()) {
                    System.out.println("  └─ Agregando " + insumosTotales.size() + " insumos al comprobante existente");

                    for (Map<String, Object> insumoData : insumosTotales) {
                        try {
                            Long insumoId = Long.parseLong(insumoData.get("insumoId").toString());
                            BigDecimal cantidad = new BigDecimal(insumoData.get("cantidad").toString());
                            Insumo insumo = insumoRepository.findById(insumoId).orElse(null);

                            if (insumo != null) {
                                // Crear detalle INFORMATIVO (los insumos ya fueron descontados arriba)
                                DetalleComprobante detalleInsumo = new DetalleComprobante();
                                detalleInsumo.setComprobante(comprobante);
                                detalleInsumo.setTipoItem("INSUMO");
                                detalleInsumo.setItemId(insumo.getId());
                                detalleInsumo.setDescripcionItem(insumo.getCodigo() + " - " + insumo.getNombre() + " (Incluido)");
                                detalleInsumo.setCantidad(cantidad);
                                detalleInsumo.setPrecioUnitario(BigDecimal.ZERO);
                                detalleInsumo.setSubtotal(BigDecimal.ZERO);
                                detalleComprobanteRepository.save(detalleInsumo);

                                System.out.println("     ✓ Detalle agregado: " + insumo.getNombre() + " x " + cantidad);
                            }
                        } catch (Exception e) {
                            System.err.println("     ❌ Error agregando detalle: " + e.getMessage());
                        }
                    }
                }
            } else {
                System.out.println("  ➕ Generando NUEVO comprobante...");
                // Generar nuevo comprobante con los insumos totales
                comprobante = generarComprobante(cita, procedimiento, insumosTotales);
                System.out.println("  ✓ Comprobante NUEVO creado: #" + comprobante.getId() +
                                 " (" + comprobante.getNumeroComprobante() + ")");
            }

            System.out.println("\n✅ TRATAMIENTO COMPLETADO EXITOSAMENTE");
            System.out.println("  ├─ Tratamiento ID: " + tratamiento.getId());
            System.out.println("  ├─ Comprobante ID: " + comprobante.getId());
            System.out.println("  └─ Número Comprobante: " + comprobante.getNumeroComprobante());
            System.out.println("=".repeat(80) + "\n");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Tratamiento registrado correctamente y comprobante generado");
            response.put("tratamientoId", tratamiento.getId());
            response.put("comprobanteId", comprobante.getId());
            response.put("numeroComprobante", comprobante.getNumeroComprobante());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
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
            System.out.println("📥 ENDPOINT /planificar - Datos recibidos:");
            System.out.println("  Datos completos: " + datos);

            // Extraer datos básicos
            Long citaId = Long.parseLong(datos.get("citaId").toString());
            Long procedimientoId = Long.parseLong(datos.get("procedimientoId").toString());
            String piezasDentales = (String) datos.get("piezasDentales");
            String descripcion = (String) datos.get("descripcion");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> camposDinamicos = (List<Map<String, String>>) datos.get("camposDinamicos");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> insumosTotales = (List<Map<String, Object>>) datos.get("insumosTotales");

            System.out.println("  CitaId: " + citaId);
            System.out.println("  ProcedimientoId: " + procedimientoId);
            System.out.println("  Insumos recibidos: " + (insumosTotales != null ? insumosTotales.size() : 0));
            if (insumosTotales != null) {
                for (Map<String, Object> insumo : insumosTotales) {
                    System.out.println("    - Insumo ID: " + insumo.get("insumoId") + ", Cantidad: " + insumo.get("cantidad"));
                }
            }

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
            tratamiento.setEstado("EN_CURSO"); // Cambiar a EN_CURSO porque está asociado a una cita activa
            tratamiento.setCitaAsociada(cita); // ← CRÍTICO: Asociar la cita
            tratamiento.setNotas("Detectado en cita del " + cita.getFechaHoraInicio().toLocalDate());

            // **GUARDAR LOS INSUMOS MODIFICADOS EN FORMATO JSON**
            if (insumosTotales != null && !insumosTotales.isEmpty()) {
                try {
                    // Usar ObjectMapper de Jackson para serializar a JSON
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String insumosJson = mapper.writeValueAsString(insumosTotales);
                    tratamiento.setInsumosJson(insumosJson);
                    System.out.println("✓ Insumos guardados en JSON: " + insumosJson);
                } catch (Exception e) {
                    System.err.println("⚠ Error al serializar insumos a JSON: " + e.getMessage());
                    // No fallar el guardado por esto
                }
            } else {
                System.out.println("ℹ️ No hay insumos modificados para guardar");
            }

            // Guardar
            tratamientoPlanificadoRepository.save(tratamiento);
            System.out.println("✓ Tratamiento planificado guardado con ID: " + tratamiento.getId());
            System.out.println("ℹ️ El comprobante se generará automáticamente cuando se marque 'ASISTIÓ' en la cita asociada");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Tratamiento planificado correctamente con " +
                        (insumosTotales != null ? insumosTotales.size() : 0) + " insumos guardados");
            response.put("tratamientoId", tratamiento.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ ERROR en /planificar: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * Genera automáticamente un comprobante para el tratamiento realizado
     * Incluye el procedimiento principal y los insumos utilizados
     *
     * IMPORTANTE: Este método NO descuenta insumos del inventario.
     * El descuento ya se hizo en realizarInmediato(), aquí solo se crean los detalles informativos.
     */
    private Comprobante generarComprobante(Cita cita, Procedimiento procedimiento,
                                          List<Map<String, Object>> insumosTotales) {
        // Crear comprobante
        Comprobante comprobante = new Comprobante();
        comprobante.setCita(cita);
        comprobante.setPaciente(cita.getPaciente());
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setTipoComprobante("CITA");
        comprobante.setDescripcion("Comprobante por tratamiento: " + procedimiento.getNombre());

        // Generar número de comprobante único
        String numeroComprobante = generarNumeroComprobante();
        comprobante.setNumeroComprobante(numeroComprobante);

        // Calcular monto total - SOLO incluye el precio del procedimiento
        // Los insumos NO se cobran por separado (están incluidos en el precio del procedimiento)
        BigDecimal montoTotal = procedimiento.getPrecio() != null ? procedimiento.getPrecio() : BigDecimal.ZERO;

        comprobante.setMontoTotal(montoTotal);
        comprobante.setMontoPagado(BigDecimal.ZERO);
        comprobante.setMontoPendiente(montoTotal);

        // Obtener estado "PENDIENTE"
        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre("PENDIENTE")
                .orElseGet(() -> {
                    // Si no existe, buscar el primero disponible
                    List<EstadoPago> estados = estadoPagoRepository.findAll();
                    return estados.isEmpty() ? null : estados.get(0);
                });

        if (estadoPendiente == null) {
            throw new RuntimeException("No se encontró estado de pago disponible");
        }

        comprobante.setEstadoPago(estadoPendiente);

        // Guardar comprobante
        comprobante = comprobanteRepository.save(comprobante);

        // Crear detalle del procedimiento
        DetalleComprobante detalleProcedimiento = new DetalleComprobante();
        detalleProcedimiento.setComprobante(comprobante);
        detalleProcedimiento.setTipoItem("PROCEDIMIENTO");
        detalleProcedimiento.setItemId(procedimiento.getId());
        detalleProcedimiento.setDescripcionItem(procedimiento.getCodigo() + " - " + procedimiento.getNombre());
        detalleProcedimiento.setCantidad(BigDecimal.ONE);
        detalleProcedimiento.setPrecioUnitario(procedimiento.getPrecio() != null ? procedimiento.getPrecio() : BigDecimal.ZERO);
        detalleProcedimiento.setSubtotal(procedimiento.getPrecio() != null ? procedimiento.getPrecio() : BigDecimal.ZERO);
        detalleComprobanteRepository.save(detalleProcedimiento);

        // Crear detalles de insumos (INFORMATIVOS - Sin cargo)
        // IMPORTANTE: Los insumos YA fueron descontados en realizarInmediato()
        // Aquí solo creamos los detalles para que aparezcan en el comprobante
        if (insumosTotales != null && !insumosTotales.isEmpty()) {
            for (Map<String, Object> insumo : insumosTotales) {
                try {
                    Long insumoId = Long.parseLong(insumo.get("insumoId").toString());
                    BigDecimal cantidad = new BigDecimal(insumo.get("cantidad").toString());
                    Insumo insumoEntity = insumoRepository.findById(insumoId).orElse(null);

                    if (insumoEntity != null) {
                        // Crear detalle INFORMATIVO con precio S/ 0.00 (sin cargo)
                        DetalleComprobante detalleInsumo = new DetalleComprobante();
                        detalleInsumo.setComprobante(comprobante);
                        detalleInsumo.setTipoItem("INSUMO");
                        detalleInsumo.setItemId(insumoEntity.getId());
                        detalleInsumo.setDescripcionItem(insumoEntity.getCodigo() + " - " + insumoEntity.getNombre() + " (Incluido)");
                        detalleInsumo.setCantidad(cantidad);
                        detalleInsumo.setPrecioUnitario(BigDecimal.ZERO); // ← SIN CARGO
                        detalleInsumo.setSubtotal(BigDecimal.ZERO); // ← SIN CARGO
                        detalleComprobanteRepository.save(detalleInsumo);

                        System.out.println("✓ Insumo agregado al comprobante (sin cargo): " +
                                          insumoEntity.getNombre() + " x " + cantidad);
                    }
                } catch (Exception e) {
                    System.err.println("Error guardando detalle de insumo: " + e.getMessage());
                }
            }
        }

        return comprobante;
    }

    /**
     * Genera un número de comprobante único en formato: COMP-YYYYMMDD-XXXX
     * Donde XXXX es un correlativo del día
     */
    private String generarNumeroComprobante() {
        LocalDateTime ahora = LocalDateTime.now();
        String fecha = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Contar comprobantes del día
        String prefijo = "COMP-" + fecha + "-";
        List<Comprobante> comprobantesHoy = comprobanteRepository.findAll().stream()
                .filter(c -> c.getNumeroComprobante().startsWith(prefijo))
                .collect(Collectors.toList());

        int correlativo = comprobantesHoy.size() + 1;
        return prefijo + String.format("%04d", correlativo);
    }

    /**
     * Registra el uso de insumos en el inventario
     * Crea un movimiento de tipo SALIDA y actualiza el stock del insumo
     *
     * @param insumo Insumo utilizado
     * @param cantidad Cantidad usada
     * @param referencia Referencia del movimiento (ej: "Cita #123")
     */
    private void registrarUsoInsumo(Insumo insumo, BigDecimal cantidad, String referencia) {
        try {
            // Obtener tipo de movimiento SALIDA
            TipoMovimiento tipoSalida = tipoMovimientoRepository.findByCodigo("SALIDA")
                    .orElseThrow(() -> new RuntimeException("Tipo de movimiento SALIDA no encontrado"));

            // ✅ CORRECCIÓN: Obtener motivo de movimiento "Uso en procedimiento"
            MotivoMovimiento motivoUsoProcedimiento = motivoMovimientoRepository.findByNombre("Uso en procedimiento")
                    .orElseGet(() -> {
                        // Si no existe, buscar alternativas comunes
                        Optional<MotivoMovimiento> alternativo = motivoMovimientoRepository.findByNombre("Uso en tratamiento");
                        if (alternativo.isPresent()) {
                            return alternativo.get();
                        }
                        // Si tampoco existe, intentar crear uno automáticamente
                        System.err.println("⚠️ ADVERTENCIA: No se encontró motivo 'Uso en procedimiento'. Buscando primer motivo de tipo SALIDA...");
                        // Buscar el primer motivo asociado al tipo SALIDA
                        return motivoMovimientoRepository.findAll().stream()
                                .filter(m -> m.getTipoMovimiento() != null &&
                                           m.getTipoMovimiento().getId().equals(tipoSalida.getId()))
                                .findFirst()
                                .orElse(null);
                    });

            if (motivoUsoProcedimiento == null) {
                System.err.println("❌ ERROR CRÍTICO: No se pudo encontrar ningún motivo válido para movimientos de SALIDA");
                throw new RuntimeException("No existe motivo de movimiento para uso en procedimientos. " +
                                         "Configure los motivos de movimiento en la base de datos.");
            }

            // Guardar stock anterior
            BigDecimal stockAnterior = insumo.getStockActual();
            BigDecimal stockNuevo = stockAnterior.subtract(cantidad);

            // Validar que no quede stock negativo
            if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
                System.err.println("⚠️ Advertencia: Stock insuficiente para insumo " + insumo.getNombre() +
                        ". Stock actual: " + stockAnterior + ", Cantidad solicitada: " + cantidad);
                // Permitir el movimiento pero registrar como stock 0
                stockNuevo = BigDecimal.ZERO;
            }

            // Crear movimiento de inventario
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setInsumo(insumo);
            movimiento.setTipoMovimiento(tipoSalida);
            movimiento.setMotivoMovimiento(motivoUsoProcedimiento); // ✅ ASIGNAR MOTIVO
            movimiento.setCantidad(cantidad);
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockNuevo(stockNuevo);
            movimiento.setReferencia(referencia);
            movimiento.setNotas("Uso en tratamiento odontológico");

            System.out.println("  💾 Guardando movimiento:");
            System.out.println("     ├─ Insumo: " + insumo.getNombre());
            System.out.println("     ├─ Tipo: " + tipoSalida.getNombre());
            System.out.println("     ├─ Motivo: " + motivoUsoProcedimiento.getNombre());
            System.out.println("     ├─ Cantidad: " + cantidad);
            System.out.println("     └─ Referencia: " + referencia);

            // Guardar movimiento
            movimientoInventarioRepository.save(movimiento);

            // Actualizar stock del insumo
            insumo.setStockActual(stockNuevo);
            insumoRepository.save(insumo);

        } catch (Exception e) {
            System.err.println("❌ Error al registrar movimiento de inventario: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al registrar uso de insumo: " + e.getMessage());
        }
    }
}
