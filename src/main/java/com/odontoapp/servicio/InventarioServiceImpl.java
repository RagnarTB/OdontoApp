package com.odontoapp.servicio;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.MovimientoInventario;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MovimientoInventarioRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;

    public InventarioServiceImpl(InsumoRepository insumoRepository,
            MovimientoInventarioRepository movimientoRepository,
            TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository,
            ProcedimientoRepository procedimientoRepository,
            ProcedimientoInsumoRepository procedimientoInsumoRepository) {
        this.insumoRepository = insumoRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
    }

    @Override
    @Transactional
    public void registrarMovimiento(MovimientoDTO dto) {
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new IllegalStateException("Insumo no encontrado con ID: " + dto.getInsumoId()));

        // Validar que el insumo no esté eliminado
        if (insumo.isEliminado()) {
            throw new IllegalStateException("No se puede registrar un movimiento para un insumo eliminado.");
        }

        // ✅ VALIDACIÓN COMPLETA: Verificar coherencia de cantidad según unidad de
        // medida
        BigDecimal cantidad = dto.getCantidad();
        if (insumo.getUnidadMedida() != null) {
            String unidad = insumo.getUnidadMedida().getNombre().toLowerCase();
            String abreviatura = insumo.getUnidadMedida().getAbreviatura().toLowerCase();

            // Determinar si la unidad requiere valores enteros
            boolean requiereEntero = esUnidadEntera(unidad, abreviatura);

            if (requiereEntero) {
                // Verificar si tiene decimales
                if (cantidad.stripTrailingZeros().scale() > 0) {
                    throw new IllegalStateException(
                            String.format("No se pueden registrar cantidades decimales para '%s'. " +
                                    "La cantidad debe ser un número entero.",
                                    insumo.getUnidadMedida().getNombre()));
                }
            }
        }

        TipoMovimiento tipo = tipoMovimientoRepository.findById(dto.getTipoMovimientoId())
                .orElseThrow(() -> new IllegalStateException("Tipo de movimiento no encontrado."));

        MotivoMovimiento motivo = motivoMovimientoRepository.findById(dto.getMotivoMovimientoId())
                .orElseThrow(() -> new IllegalStateException("Motivo de movimiento no encontrado."));

        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockNuevo;

        if (tipo.getAfectaStock() == TipoMovimiento.AfectaStock.SUMA) {
            stockNuevo = stockAnterior.add(cantidad);
        } else if (tipo.getAfectaStock() == TipoMovimiento.AfectaStock.RESTA) {
            stockNuevo = stockAnterior.subtract(cantidad);
            if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "No hay stock suficiente para registrar la salida. Stock actual: " + stockAnterior);
            }
        } else {
            stockNuevo = stockAnterior; // Para movimientos de tipo AJUSTE que no afectan stock
        }

        // 1. Crear y guardar el registro del movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setInsumo(insumo);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setNotas(dto.getNotas());
        movimiento.setReferencia(dto.getReferencia());
        movimientoRepository.save(movimiento);

        // 2. Actualizar el stock del insumo
        insumo.setStockActual(stockNuevo);
        insumoRepository.save(insumo);
    }

    @Override
    public Page<MovimientoInventario> listarMovimientosPorInsumo(Long insumoId, Pageable pageable) {
        return movimientoRepository.findByInsumoIdOrderByFechaCreacionDesc(insumoId, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void descontarStockPorProcedimientoRealizado(Long procedimientoId, BigDecimal cantidadAjustada,
            Long insumoAjustadoId, String referenciaCita) {
        System.out.println("🔍 [INVENTARIO] Iniciando descuento de stock...");
        System.out.println("   - Procedimiento ID: " + procedimientoId);
        System.out.println("   - Referencia: " + referenciaCita);

        // Validar que el procedimiento existe
        Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Procedimiento no encontrado con ID: " + procedimientoId));

        System.out.println("   - Procedimiento encontrado: " + procedimiento.getNombre());

        // Buscar el tipo de movimiento "SALIDA" por CÓDIGO (más confiable)
        TipoMovimiento tipoSalida = tipoMovimientoRepository.findByCodigo("SALIDA")
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de movimiento 'SALIDA' no encontrado en el sistema"));

        // Buscar motivo con el nombre correcto según DataInitializer
        MotivoMovimiento motivoProcedimiento = motivoMovimientoRepository.findByNombre("Uso en procedimiento")
                .orElseThrow(() -> new IllegalStateException(
                        "Motivo 'Uso en procedimiento' no encontrado en el sistema"));

        System.out.println("   - Tipo y motivo encontrados correctamente");

        // Caso 1: Se especificó un insumo ajustado (se usa solo ese insumo con cantidad
        // ajustada)
        if (insumoAjustadoId != null && cantidadAjustada != null) {
            Insumo insumo = insumoRepository.findById(insumoAjustadoId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Insumo no encontrado con ID: " + insumoAjustadoId));

            descontarInsumo(insumo, cantidadAjustada, tipoSalida, motivoProcedimiento, referenciaCita);
        }
        // Caso 2: No se especificó insumo ajustado, usar insumos por defecto del
        // procedimiento
        else {
            List<ProcedimientoInsumo> insumosDelProcedimiento = procedimientoInsumoRepository
                    .findByProcedimientoId(procedimientoId);

            System.out.println("   - Insumos encontrados para el procedimiento: " + insumosDelProcedimiento.size());

            if (insumosDelProcedimiento.isEmpty()) {
                System.out.println("   ⚠️ No hay insumos configurados para este procedimiento - no se descuenta nada");
                // No hay insumos configurados para este procedimiento, no se descuenta nada
                return;
            }

            // Descontar cada insumo según la cantidad por defecto configurada
            for (ProcedimientoInsumo pi : insumosDelProcedimiento) {
                System.out.println("   📦 Procesando insumo: " + pi.getInsumo().getNombre() +
                        " (Cantidad: " + pi.getCantidadDefecto() + ")");
                descontarInsumo(pi.getInsumo(), pi.getCantidadDefecto(), tipoSalida, motivoProcedimiento,
                        referenciaCita);
            }
        }

        System.out.println("✅ [INVENTARIO] Descuento de stock completado exitosamente");
    }

    /**
     * Método auxiliar para descontar stock de un insumo y registrar el movimiento.
     */
    private void descontarInsumo(Insumo insumo, BigDecimal cantidad, TipoMovimiento tipoMovimiento,
            MotivoMovimiento motivo, String referencia) {
        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockNuevo = stockAnterior.subtract(cantidad);

        // Validar que hay stock suficiente
        if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    "Stock insuficiente para el insumo '" + insumo.getNombre() +
                            "'. Stock actual: " + stockAnterior + ", se requiere: " + cantidad);
        }

        // Crear el movimiento de inventario
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setInsumo(insumo);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setReferencia(referencia);
        movimiento.setNotas("Descuento automático por realización de procedimiento");
        movimientoRepository.save(movimiento);

        // Actualizar el stock del insumo
        insumo.setStockActual(stockNuevo);
        insumoRepository.save(insumo);
    }

    /**
     * Determina si una unidad de medida requiere valores enteros (no decimales).
     * 
     * @param unidad      Nombre de la unidad en minúsculas
     * @param abreviatura Abreviatura de la unidad en minúsculas
     * @return true si requiere valores enteros, false si permite decimales
     */
    private boolean esUnidadEntera(String unidad, String abreviatura) {
        // Lista de unidades que SOLO permiten valores enteros
        String[] unidadesEnteras = {
                // Unidades básicas
                "unidad", "und", "u", "pieza", "pza", "pz", "articulo", "art",
                // Contenedores
                "caja", "cj", "cja", "paquete", "paq", "frasco", "fco", "fras",
                "carpule", "carp", "cartucho", "cart",
                // Recipientes médicos
                "ampolla", "amp", "vial", "tubo", "tub", "sobre", "sob",
                // Formatos
                "rollo", "hoja", "pliego", "lamina", "lam",
                // Otros
                "blister", "blist", "kit", "set", "par"
        };

        // Verificar si la unidad o abreviatura está en la lista de enteros
        for (String unidadEntera : unidadesEnteras) {
            if (unidad.contains(unidadEntera) || abreviatura.equals(unidadEntera)) {
                return true;
            }
        }

        // Si no está en la lista de enteros, permite decimales
        return false;
    }
}
