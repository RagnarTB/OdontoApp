# ISSUE: Completar Portal del Paciente y Corregir Problemas Críticos

**Branch**: `claude/review-git-request-01PfPfZiAk6EJKFsBHwiRoQV`
**Último commit**: `a7213fd - fix: Descuento automático de insumos y error de lazy loading en perfil`
**Fecha**: 2025-01-19
**Prioridad**: ALTA

---

## 📋 RESUMEN

El portal del paciente tiene múltiples problemas críticos que impiden su funcionamiento completo. Este issue documenta TODOS los problemas encontrados en revisión exhaustiva del código actual.

---

## 🔴 PROBLEMAS CRÍTICOS (BLOQUEAN FUNCIONALIDAD)

### 1. **CALENDARIO DE CITAS DEL PACIENTE NO EXISTE**
**Ubicación**: `/paciente/citas`
**Archivo**: `src/main/resources/templates/paciente/citas/calendario.html`

**Problema**:
- NO tiene implementación de FullCalendar
- Solo muestra un formulario básico sin calendario visual
- NO permite ver citas de manera gráfica

**Comparación con Admin**:
```
ADMIN (/citas):
✅ FullCalendar con vista mensual/semanal/diaria
✅ Eventos visuales con colores por estado
✅ Click en día para agendar nueva cita
✅ Click en evento para ver detalles
✅ Drag & drop para reprogramar
✅ Integración con API de eventos

PACIENTE (/paciente/citas):
❌ Solo formulario HTML simple
❌ No hay calendario visual
❌ No hay integración con FullCalendar
❌ No se ven citas gráficamente
```

**Archivos a modificar**:
- `src/main/resources/templates/paciente/citas/calendario.html` - Implementar FullCalendar
- `src/main/java/com/odontoapp/controlador/PacienteCitaController.java` - Agregar endpoint API `/api/eventos`

**Referencia**: Copiar implementación de `/modulos/citas/calendario.html` líneas 200-450 (FullCalendar)

---

### 2. **CREAR/AGENDAR CITA DESDE PANEL DEL PACIENTE NO FUNCIONA**
**Ubicación**: `/paciente/citas`
**Archivo**: `src/main/java/com/odontoapp/controlador/PacienteCitaController.java`

**Problema**:
- NO existe endpoint `@PostMapping("/crear")` en `PacienteCitaController`
- El formulario HTML envía a `/paciente/citas/crear` que NO existe (404)
- NO hay validaciones de fechas pasadas

**Falta implementar**:
```java
@PostMapping("/crear")
public String crearCita(@ModelAttribute CitaDTO citaDTO, ...) {
    // TODO: Implementar validaciones:
    // 1. No permitir citas en fechas pasadas
    // 2. Validar disponibilidad del odontólogo
    // 3. Validar horario laboral (8:00-18:00)
    // 4. Auto-asignar paciente autenticado
    // 5. Estado inicial: PENDIENTE
    // 6. Enviar email de confirmación
}
```

**Validaciones requeridas** (igual que admin):
- ✅ Fecha no puede ser pasada: `if (fecha.isBefore(LocalDate.now())) throw Exception`
- ✅ Hora válida: entre 8:00 y 18:00
- ✅ Odontólogo disponible: verificar conflictos
- ✅ Duración mínima: 30 minutos

---

### 3. **TRATAMIENTOS PLANIFICADOS SE DUPLICAN**
**Ubicación**: `/paciente/perfil#tratamientos`
**Archivo**: `src/main/java/com/odontoapp/repositorio/TratamientoPlanificadoRepository.java`

**Problema**:
```
FLUJO ACTUAL (INCORRECTO):
1. Se planifica tratamiento → estado = "EN_CURSO"
2. Paciente asiste a cita
3. CitaServiceImpl marca tratamiento como "COMPLETADO" ✅
4. Se crea TratamientoRealizado ✅
5. PERO: Vista muestra AMBOS (planificado Y realizado) ❌

COMPORTAMIENTO ESPERADO:
- Si estado = "COMPLETADO" → NO mostrar en "Tratamientos Planificados"
- Solo mostrar en "Tratamientos Realizados"
```

**Causa raíz**:
- Query `findTratamientosPendientes()` filtra por `estado IN ('PLANIFICADO', 'EN_CURSO')`
- Cuando se marca como "COMPLETADO", la query lo excluye correctamente
- PERO: `TratamientoController.realizarInmediato()` líneas 270-276 marca como "COMPLETADO"
- CitaServiceImpl líneas 598-606 también marca como "COMPLETADO"
- **Posible duplicación de lógica causando inconsistencia**

**Solución propuesta**:
1. Verificar que SOLO un lugar marca como "COMPLETADO"
2. Agregar log para debugging: `System.out.println("Estado antes: " + estado + ", después: COMPLETADO")`
3. Verificar transacciones no están rollbacking el cambio
4. Agregar test unitario para verificar flujo completo

**Archivos a revisar**:
- `src/main/java/com/odontoapp/servicio/impl/CitaServiceImpl.java:598-606`
- `src/main/java/com/odontoapp/controlador/TratamientoController.java:270-276`

---

### 4. **VALIDACIÓN DE TELÉFONO EN EDITAR PERFIL**
**Ubicación**: `/paciente/perfil/editar`
**Archivo**: `src/main/resources/templates/paciente/perfil/editar.html`

**Problema**:
- Campo teléfono acepta letras y símbolos
- NO valida longitud de 9 dígitos

**HTML actual** (líneas ~135-145):
```html
<input type="text" th:field="*{telefono}" id="telefono" class="form-control">
```

**HTML requerido**:
```html
<input type="tel"
       th:field="*{telefono}"
       id="telefono"
       class="form-control"
       pattern="[0-9]{9}"
       maxlength="9"
       placeholder="999999999"
       title="Ingrese 9 dígitos numéricos">
```

**JavaScript adicional requerido**:
```javascript
$('#telefono').on('input', function() {
    this.value = this.value.replace(/[^0-9]/g, '');
});
```

---

### 5. **VALIDACIÓN Y VERIFICACIÓN DE EMAIL AL CAMBIAR**
**Ubicación**: `/paciente/perfil/editar`
**Archivo**: `src/main/java/com/odontoapp/controlador/PacientePerfilController.java:159-174`

**Problema**:
- Se permite cambiar email sin verificación
- NO se envía código de confirmación
- Usuario podría quedar bloqueado

**Código actual** (líneas 170-174):
```java
// Si el email cambió, también actualizar el usuario
if (!usuario.getEmail().equals(pacienteDTO.getEmail())) {
    usuario.setEmail(pacienteDTO.getEmail());
    usuarioRepository.save(usuario);
}
```

**Código requerido** (similar a admin):
```java
if (!usuario.getEmail().equals(pacienteDTO.getEmail())) {
    // 1. Generar código de verificación
    String codigoVerificacion = generarCodigoAleatorio(6);

    // 2. Guardar temporalmente nuevo email y código
    usuario.setEmailNuevo(pacienteDTO.getEmail());
    usuario.setCodigoVerificacion(codigoVerificacion);
    usuario.setEmailVerificado(false);

    // 3. Enviar email con código
    emailService.enviarCodigoVerificacion(
        pacienteDTO.getEmail(),
        codigoVerificacion
    );

    // 4. Redirigir a página de verificación
    redirectAttributes.addFlashAttribute("info",
        "Se ha enviado un código de verificación a " + pacienteDTO.getEmail());
    return "redirect:/paciente/perfil/verificar-email";
}
```

**Archivos a crear**:
- `src/main/resources/templates/paciente/perfil/verificar-email.html`
- Método `verificarCodigoEmail()` en `PacientePerfilController`

---

## 🟡 PROBLEMAS MODERADOS (DEGRADAN EXPERIENCIA)

### 6. **MODAL DE PAGOS NO MUESTRA VALIDACIONES**
**Ubicación**: `/facturacion/detalle/{id}`
**Archivo**: `src/main/resources/templates/modulos/facturacion/detalle.html`

**Problema reportado por usuario**:
> "En el modal de pago, sigue viéndose el modal antiguo"

**Investigación**:
- El fragmento `scriptModalPago` existe en línea 122 de `fragmentos.html`
- Se incluye en línea 319 de `detalle.html`
- Validación de Yape > 500 soles está en líneas 304-318

**Posible causa**:
- Cache del navegador
- Orden de carga de scripts
- jQuery no disponible en el momento correcto

**Verificación requerida**:
1. Abrir consola del navegador (F12)
2. Ir a `/facturacion/detalle/1`
3. Buscar errores JavaScript
4. Verificar que console.log("✅ Script del modal de pagos cargado") aparece
5. Si NO aparece → problema de inclusión del fragmento

**Solución temporal**:
- Ctrl + Shift + R para forzar recarga sin cache
- Verificar en modo incógnito

---

### 7. **CALENDARIO PACIENTE - FALTAN ENDPOINTS API**
**Ubicación**: `/paciente/citas/api/eventos`
**Archivo**: `src/main/java/com/odontoapp/controlador/PacienteCitaController.java`

**Falta implementar**:
```java
@GetMapping("/api/eventos")
@ResponseBody
@Transactional(readOnly = true)
public List<FullCalendarEventDTO> getEventos(
        @RequestParam String start,
        @RequestParam String end) {

    Usuario paciente = obtenerUsuarioAutenticado();

    // Convertir strings a LocalDateTime
    LocalDateTime startDate = LocalDateTime.parse(start);
    LocalDateTime endDate = LocalDateTime.parse(end);

    // Buscar citas del paciente en rango de fechas
    List<Cita> citas = citaRepository.findByPacienteIdAndFechaHoraInicioBetween(
        paciente.getId(),
        startDate,
        endDate
    );

    // Convertir a DTO de FullCalendar
    return citas.stream()
        .map(this::mapToFullCalendarEvent)
        .collect(Collectors.toList());
}

@GetMapping("/api/disponibilidad")
@ResponseBody
public Map<String, Object> getDisponibilidad(
        @RequestParam Long odontologoId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return citaService.buscarDisponibilidad(odontologoId, fecha);
}
```

---

## 🟢 MEJORAS OPCIONALES (NO CRÍTICAS)

### 8. **DASHBOARD DEL PACIENTE VACÍO**
**Ubicación**: `/paciente/dashboard`
**Archivo**: `src/main/resources/templates/paciente/dashboard.html`

**Sugerencias de contenido**:
- 📅 Próxima cita (fecha, hora, odontólogo)
- 💰 Deuda pendiente total
- 📊 Tratamientos pendientes (contador)
- 🦷 Estado del odontograma (dientes con problemas)
- 📈 Gráfico de asistencia (últimos 6 meses)

---

### 9. **COMPROBANTE DETALLE DEL PACIENTE - BOTÓN DESCARGAR PDF**
**Ubicación**: `/paciente/perfil/comprobantes/{id}`
**Archivo**: `src/main/resources/templates/paciente/perfil/comprobante-detalle.html`

**Falta**:
- Botón "Descargar PDF"
- Endpoint `/paciente/perfil/comprobantes/{id}/pdf`
- Generación de PDF con librería iText o similar

---

## 📁 ARCHIVOS MODIFICADOS EN SESIÓN ACTUAL

### Commits realizados (9 total):
```
a7213fd - fix: Descuento automático de insumos y error de lazy loading en perfil
3a1d9c6 - fix: Corregir errores críticos en comprobantes, modal de pagos y perfil
144ad93 - fix: Eliminar duplicación de comprobantes al marcar asistencia
9a171bc - fix: Corregir errores de Thymeleaf y rediseñar perfil del paciente
99070f6 - feat: Agregar validación de Yape > 500 soles en pago total
fe24639 - feat: Implementar modal de anulación de comprobantes con devolución
21b6c72 - feat: Generar comprobante automáticamente cuando paciente asiste
1efb0cf - feat: Agregar registro de movimientos de inventario en tratamientos
5109a75 - fix: Mejorar perfil del paciente con Alergias, Antecedentes
```

### Archivos clave modificados:
1. ✅ `PacientePerfilController.java` - Agregado @Transactional
2. ✅ `TratamientoController.java` - Descuento de insumos en "realizar ahora"
3. ✅ `paciente/perfil/editar.html` - Eliminado campo género
4. ✅ `modulos/facturacion/fragmentos.html` - Script modal pagos separado
5. ✅ `modulos/facturacion/detalle.html` - Inclusión script modal
6. ✅ `CitaServiceImpl.java` - Generación automática comprobante al asistir

---

## 🎯 PLAN DE ACCIÓN PROPUESTO

### Fase 1 - Crítico (1-2 días)
1. **Implementar calendario FullCalendar en panel paciente**
   - Copiar estructura de `/modulos/citas/calendario.html`
   - Agregar endpoints API en `PacienteCitaController`
   - Filtrar solo citas del paciente autenticado

2. **Implementar endpoint crear cita paciente**
   - Con validaciones de fecha/hora
   - Auto-asignar paciente autenticado
   - Enviar email confirmación

3. **Corregir duplicación tratamientos planificados**
   - Debug completo del flujo
   - Agregar logs detallados
   - Test unitario

### Fase 2 - Moderado (1 día)
4. **Validaciones en editar perfil**
   - Teléfono: pattern + JavaScript
   - Email: sistema de verificación completo

5. **Verificar modal de pagos**
   - Testing en diferentes navegadores
   - Verificar consola JavaScript
   - Documentar si problema persiste

### Fase 3 - Mejoras (1 día)
6. **Dashboard del paciente**
   - Agregar widgets informativos
   - Próxima cita destacada

7. **Descargar PDF comprobante**
   - Implementar generación PDF
   - Botón en vista detalle

---

## 📝 NOTAS TÉCNICAS IMPORTANTES

### Descuento de Insumos - FUNCIONANDO ✅
**Flujo "Realizar Ahora"**:
1. Descuenta insumos predeterminados (TratamientoController:306-352)
2. Descuenta insumos adicionales (TratamientoController:556-588)
3. Genera comprobante con precio procedimiento
4. Lista insumos como informativos (S/ 0.00)

**Flujo "Planificado"**:
1. CitaServiceImpl.marcarAsistencia() descuenta automáticamente
2. Marca tratamiento como COMPLETADO
3. Genera comprobante

### Lazy Loading - SOLUCIONADO ✅
- Agregado `@Transactional(readOnly = true)` en `PacientePerfilController.verPerfil()`
- Todas las tabs del perfil cargan correctamente
- ERR_INCOMPLETE_CHUNKED_ENCODING resuelto

### Modal de Pagos - VERIFICAR ⚠️
- Código correcto implementado
- Validación Yape > 500 existe
- Usuario reporta que no se ve
- **Requiere testing en entorno real**

---

## 🔍 TESTING REQUERIDO

### Casos de prueba prioritarios:
1. **Crear cita desde panel paciente**
   - [ ] Fecha futura válida → debe crear
   - [ ] Fecha pasada → debe rechazar
   - [ ] Hora fuera de horario → debe rechazar
   - [ ] Odontólogo ocupado → debe rechazar

2. **Tratamientos planificados**
   - [ ] Planificar tratamiento
   - [ ] Marcar asistencia
   - [ ] Verificar NO aparece en planificados
   - [ ] Verificar SÍ aparece en realizados

3. **Editar perfil**
   - [ ] Cambiar teléfono con letras → debe rechazar
   - [ ] Teléfono con 10 dígitos → debe truncar a 9
   - [ ] Cambiar email → debe pedir verificación
   - [ ] Código verificación correcto → debe actualizar
   - [ ] Código incorrecto → debe rechazar

4. **Modal de pagos**
   - [ ] Pago total con Yape > 500 → debe alertar
   - [ ] Pago parcial con Yape > 500 → debe permitir
   - [ ] Referencia vacía con Yape → debe rechazar

---

## 🚀 COMANDOS ÚTILES

```bash
# Ver estado actual
git log --oneline -10
git status

# Crear rama para nuevo desarrollo
git checkout -b feature/completar-portal-paciente

# Testing local
./mvnw spring-boot:run

# Verificar compilación
./mvnw clean compile

# Ejecutar tests
./mvnw test
```

---

## 📚 REFERENCIAS

### Código de referencia (Admin Panel):
- **Calendario**: `/modulos/citas/calendario.html:200-450`
- **API eventos**: `CitaController.java:getEventos()`
- **Crear cita**: `CitaController.java:crearCita()`
- **Validaciones**: `CitaService.java:validarCita()`

### Documentación relevante:
- Spring @Transactional: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html
- FullCalendar: https://fullcalendar.io/docs
- Thymeleaf: https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html

---

## ✅ CRITERIOS DE ACEPTACIÓN

### El issue se considera resuelto cuando:
- [x] Descuento automático de insumos funciona (COMPLETADO)
- [x] Error lazy loading en perfil resuelto (COMPLETADO)
- [ ] Calendario del paciente muestra eventos con FullCalendar
- [ ] Paciente puede crear citas con validaciones completas
- [ ] Tratamientos planificados NO se duplican
- [ ] Teléfono solo acepta 9 dígitos numéricos
- [ ] Cambio de email requiere verificación
- [ ] Modal de pagos muestra validaciones (verificar)
- [ ] Dashboard del paciente muestra información útil
- [ ] Todos los tests pasan

---

**Creado**: 2025-01-19
**Última actualización**: 2025-01-19
**Asignado a**: Próxima sesión de desarrollo
**Prioridad**: ALTA
**Estimación**: 3-5 días de desarrollo
