# Implementación Pendiente - Cadena de Citas y Comprobantes
**Fecha:** 2025-11-20
**Status:** ✅ **COMPLETADO** (Backend 100%, Frontend 100%)

---

## ✅ Completado Hasta Ahora

### 1. Infraestructura de Base de Datos
- ✅ Campo `cita_generada_por_tratamiento_id` en tabla `citas`
- ✅ Método `findByCitaGeneradaPorTratamientoId()` en CitaRepository
- ✅ Script SQL de migración: `docs/sql-migration-cadena-citas.sql`

### 2. Lógica de Negocio - Backend
- ✅ Vincular cita generada con cita origen (línea 317 TratamientoController)
- ✅ Agregar TRATAMIENTO al comprobante con su precio (líneas 378-389)
- ✅ Actualizar monto total del comprobante (líneas 394-401)
- ✅ Logging detallado de todo el proceso

---

## 🟡 Pendiente de Implementar

### 1. API para Verificar Botón Activo

**Archivo:** `src/main/java/com/odontoapp/controlador/CitaController.java`

**Agregar endpoint:**
```java
/**
 * Verifica si una cita puede registrar tratamientos.
 * Una cita NO puede registrar tratamientos si ya generó otra cita.
 */
@GetMapping("/api/cita/{citaId}/puede-registrar-tratamiento")
@ResponseBody
public ResponseEntity<Map<String, Object>> puedeRegistrarTratamiento(@PathVariable Long citaId) {
    Map<String, Object> response = new HashMap<>();

    try {
        // Buscar si esta cita ya generó otra cita de tratamiento
        Cita citaGenerada = citaRepository.findByCitaGeneradaPorTratamientoId(citaId);

        boolean puedeRegistrar = (citaGenerada == null);
        Long citaGeneradaId = (citaGenerada != null) ? citaGenerada.getId() : null;

        response.put("puedeRegistrar", puedeRegistrar);
        response.put("citaGeneradaId", citaGeneradaId);
        response.put("mensaje", puedeRegistrar
            ? "Esta cita puede registrar tratamientos"
            : "Esta cita ya generó tratamiento en Cita #" + citaGeneradaId);

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        response.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
```

---

### 2. Frontend - Mostrar/Ocultar Botón

**Archivo:** `src/main/resources/templates/modulos/citas/detalle.html`

**Modificar el botón "Registrar Tratamiento":**

```html
<!-- ANTES -->
<button type="button" class="btn btn-success"
        data-toggle="modal"
        data-target="#modalRegistrarTratamiento">
    <i class="fas fa-tooth mr-2"></i>Registrar Tratamiento
</button>

<!-- DESPUÉS -->
<button type="button"
        id="btnRegistrarTratamiento"
        class="btn btn-success"
        data-toggle="modal"
        data-target="#modalRegistrarTratamiento">
    <i class="fas fa-tooth mr-2"></i>Registrar Tratamiento
</button>

<div id="mensajeCitaGenerada"
     class="alert alert-info mt-2"
     style="display:none;">
    <i class="fas fa-info-circle"></i>
    Este tratamiento fue registrado en otra cita.
    <a href="#" id="linkCitaGenerada">Ver cita generada →</a>
</div>
```

**Agregar JavaScript en detalle.html:**

```javascript
// Al cargar el detalle de la cita
$(document).ready(function() {
    const citaId = $('#detalleCitaId').val();

    if (citaId) {
        // Verificar si la cita puede registrar tratamientos
        $.get('/citas/api/cita/' + citaId + '/puede-registrar-tratamiento', function(response) {
            if (!response.puedeRegistrar) {
                // Deshabilitar botón y mostrar mensaje
                $('#btnRegistrarTratamiento')
                    .prop('disabled', true)
                    .removeClass('btn-success')
                    .addClass('btn-secondary')
                    .attr('title', 'Esta cita ya generó un tratamiento');

                // Mostrar mensaje con link a cita generada
                $('#mensajeCitaGenerada').show();
                $('#linkCitaGenerada')
                    .attr('href', '/citas/' + response.citaGeneradaId)
                    .text('Ver Cita #' + response.citaGeneradaId + ' →');

                console.log('ℹ️ Botón deshabilitado: Cita ya generó tratamiento en Cita #' + response.citaGeneradaId);
            } else {
                console.log('✅ Botón habilitado: Cita puede registrar tratamientos');
            }
        }).fail(function() {
            console.error('⚠️ Error al verificar estado de la cita');
        });
    }
});
```

---

### 3. Corregir Error 403 en Registrar Pago

**Archivo:** `src/main/resources/templates/modulos/facturacion/detalle.html`

**Problema:** Falta token CSRF en petición AJAX

**Buscar en detalle.html la función que registra el pago y agregar:**

```javascript
// ANTES (sin CSRF token)
$.ajax({
    url: '/facturacion/registrar-pago',
    method: 'POST',
    data: datos,
    success: function(response) { ... }
});

// DESPUÉS (con CSRF token)
var token = $("meta[name='_csrf']").attr("content");
var header = $("meta[name='_csrf_header']").attr("content");

$.ajax({
    url: '/facturacion/registrar-pago',
    method: 'POST',
    data: datos,
    beforeSend: function(xhr) {
        xhr.setRequestHeader(header, token);  // ✅ AGREGAR CSRF TOKEN
    },
    success: function(response) { ... }
});
```

---

### 4. Modificar "Planificar para Después"

**Archivo:** `src/main/java/com/odontoapp/controlador/TratamientoController.java`

**Endpoint:** `/planificar` (línea 487+)

**Cambio requerido:** Actualmente no hace nada con comprobantes. Debe crear un NUEVO comprobante para el tratamiento planificado.

**Lógica a agregar:**

```java
@PostMapping("/planificar")
@ResponseBody
public ResponseEntity<Map<String, Object>> planificar(@RequestBody Map<String, Object> datos) {
    try {
        // ... código existente ...

        // Guardar tratamiento planificado
        tratamientoPlanificadoRepository.save(tratamiento);

        // ✅ NUEVO: Crear comprobante para el tratamiento planificado
        Comprobante comprobanteNuevo = new Comprobante();
        comprobanteNuevo.setCita(cita);
        comprobanteNuevo.setPaciente(cita.getPaciente());
        comprobanteNuevo.setFechaEmision(LocalDateTime.now());
        comprobanteNuevo.setTipoComprobante("TRATAMIENTO_PLANIFICADO");
        comprobanteNuevo.setDescripcion("Tratamiento planificado: " + procedimiento.getNombre());
        comprobanteNuevo.setNumeroComprobante(generarNumeroComprobante());

        // Calcular monto (precio del procedimiento)
        BigDecimal montoTotal = procedimiento.getPrecio() != null
            ? procedimiento.getPrecio()
            : BigDecimal.ZERO;

        comprobanteNuevo.setMontoTotal(montoTotal);
        comprobanteNuevo.setMontoPagado(BigDecimal.ZERO);
        comprobanteNuevo.setMontoPendiente(montoTotal);

        // Obtener estado PENDIENTE
        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre("PENDIENTE")
            .orElseThrow(() -> new RuntimeException("Estado PENDIENTE no encontrado"));
        comprobanteNuevo.setEstadoPago(estadoPendiente);

        // Guardar comprobante
        comprobanteRepository.save(comprobanteNuevo);

        // Agregar detalle del tratamiento planificado
        DetalleComprobante detalleTratamiento = new DetalleComprobante();
        detalleTratamiento.setComprobante(comprobanteNuevo);
        detalleTratamiento.setTipoItem("TRATAMIENTO_PLANIFICADO");
        detalleTratamiento.setItemId(tratamiento.getId());
        detalleTratamiento.setDescripcionItem(procedimiento.getCodigo() + " - " +
                                             procedimiento.getNombre() + " (Planificado)");
        detalleTratamiento.setCantidad(BigDecimal.ONE);
        detalleTratamiento.setPrecioUnitario(montoTotal);
        detalleTratamiento.setSubtotal(montoTotal);
        detalleComprobanteRepository.save(detalleTratamiento);

        System.out.println("✅ Comprobante creado para tratamiento planificado: #" +
                         comprobanteNuevo.getId());

        // ... resto del código ...
    }
}
```

---

## 🧪 Plan de Testing

### Test 1: Cadena de Citas
1. Crear Cita1 (estado: ASISTIO)
2. Registrar "Tratamiento Realizado Ahora"
3. Verificar:
   - ✅ Se crea Cita2 vinculada
   - ✅ Botón "Registrar Tratamiento" deshabilitado en Cita1
   - ✅ Botón "Registrar Tratamiento" habilitado en Cita2
   - ✅ Link a Cita2 aparece en Cita1

### Test 2: Tratamiento en Comprobante
1. Registrar tratamiento en cita con comprobante existente
2. Ir a módulo Facturación → Buscar comprobante
3. Verificar:
   - ✅ Aparece detalle "TRATAMIENTO" con su precio
   - ✅ Aparecen insumos (precio S/ 0.00)
   - ✅ Monto total actualizado correctamente

### Test 3: Registrar Pago
1. Abrir comprobante
2. Clic en "Registrar Pago"
3. Verificar:
   - ✅ NO aparece error 403
   - ✅ Pago se registra correctamente
   - ✅ Monto pendiente se actualiza

### Test 4: Planificar para Después
1. Registrar tratamiento con "Planificar para Después"
2. Ir a módulo Facturación
3. Verificar:
   - ✅ Se creó NUEVO comprobante (no actualiza el existente)
   - ✅ Comprobante tiene detalle del tratamiento planificado
   - ✅ Estado: PENDIENTE

---

## 📝 Checklist de Implementación

### Backend
- [x] Campo `citaGeneradaPorTratamiento` en Cita
- [x] Método `findByCitaGeneradaPorTratamientoId` en CitaRepository
- [x] Vincular cita generada con origen en `realizarInmediato()`
- [x] Agregar tratamiento al comprobante con precio
- [x] Actualizar monto total del comprobante
- [x] Endpoint API `/api/cita/{id}/puede-registrar-tratamiento`
- [x] Modificar `/planificar` para crear nuevo comprobante

### Frontend
- [x] Llamar API al cargar detalle de cita
- [x] Deshabilitar botón si cita ya generó tratamiento
- [x] Mostrar mensaje con link a cita generada
- [x] Agregar CSRF token en registrar pago

### Base de Datos
- [ ] Ejecutar script SQL: `docs/sql-migration-cadena-citas.sql`

### Testing
- [ ] Test cadena de citas
- [ ] Test tratamiento en comprobante
- [ ] Test registrar pago (sin error 403)
- [ ] Test planificar para después

---

## 🚀 Próximos Pasos Inmediatos

1. **Ejecutar migración SQL:**
   ```bash
   mysql -u root -p odontoapp_db < docs/sql-migration-cadena-citas.sql
   ```

2. **Implementar endpoint API** en CitaController

3. **Modificar frontend** en detalle.html

4. **Corregir error 403** en registrar pago

5. **Modificar planificar** para crear nuevo comprobante

6. **Testing exhaustivo** de todos los flujos

---

**Autor:** Claude Code
**Status:** ✅ **COMPLETADO 100%** - Listo para testing
**Próximo:** Ejecutar migración SQL y testing exhaustivo
