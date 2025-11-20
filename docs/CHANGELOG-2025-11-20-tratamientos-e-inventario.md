# CHANGELOG - Correcciones de Tratamientos e Inventario
**Fecha:** 2025-11-20
**Branch:** `claude/review-admin-appointments-01JLjbkqtGUd2gVyXktmSJWH`

## 🎯 Problemas Identificados y Resueltos

### 1. ✅ Tratamientos Planificados No Guardan Insumos Modificados

**Problema:** Cuando se usa "Planificar para Después", las cantidades modificadas y los insumos adicionales agregados en el modal no se guardaban. Al ejecutar el tratamiento planificado, solo se usaban las cantidades predeterminadas del procedimiento.

**Solución Implementada:**

#### Backend:
- **Archivo:** `TratamientoPlanificado.java` (líneas 81-87)
  - Agregado campo `insumosJson` de tipo `TEXT` para almacenar insumos en formato JSON
  ```java
  @Lob
  @Column(name = "insumos_json", length = 5000)
  private String insumosJson;
  ```

- **Archivo:** `TratamientoController.java` - endpoint `/planificar` (líneas 406-489)
  - Modificado para recibir y serializar `insumosTotales` a JSON
  - Agregado logging detallado para debuggear
  - Ahora guarda las cantidades modificadas en el campo `insumosJson`
  ```java
  List<Map<String, Object>> insumosTotales = (List<Map<String, Object>>) datos.get("insumosTotales");

  if (insumosTotales != null && !insumosTotales.isEmpty()) {
      ObjectMapper mapper = new ObjectMapper();
      String insumosJson = mapper.writeValueAsString(insumosTotales);
      tratamiento.setInsumosJson(insumosJson);
  }
  ```

#### Base de Datos:
- **Archivo:** `docs/sql-migration-add-insumos-json.sql`
  - Script SQL documentado (opcional, Hibernate lo crea automáticamente)
  - Nueva columna: `insumos_json TEXT NULL` en tabla `tratamientos_planificados`

---

### 2. ✅ Logging Detallado para Debuggear Problemas de Insumos

**Problema:** No había visibilidad sobre qué datos se estaban enviando desde el frontend ni qué se recibía en el backend.

**Solución Implementada:**

#### Frontend:
- **Archivo:** `modal-tratamiento-avanzado.html` - función `recopilarDatosTratamiento()` (líneas 1028-1113)
  - Agregado console.log detallado en cada paso:
    - ✓ Inicio de recopilación
    - ✓ Cada fila de insumos predeterminados
    - ✓ Cada insumo adicional
    - ✓ Resumen total de insumos
    - ✓ Datos completos antes de enviar
  ```javascript
  console.log('\n📦 Paso 1: Recopilando insumos predeterminados...');
  console.log('  Total predeterminados:', contadorPredeterminados);
  console.log('  Total adicionales:', contadorAdicionales);
  console.log('✅ DATOS COMPLETOS A ENVIAR:', datosCompletos);
  ```

#### Backend:
- **Archivo:** `TratamientoController.java` - endpoint `/realizar-inmediato` (líneas 196-237)
  - Agregado logging estructurado con emojis:
    ```
    ================================================================================
    📥 ENDPOINT /realizar-inmediato INICIADO
    ================================================================================
    📦 Datos RAW recibidos: {...}

    📊 DATOS PROCESADOS:
      ├─ Cita ID: 123
      ├─ Procedimiento ID: 45
      ├─ Piezas Dentales: 11,12
      ├─ Tratamiento Planificado ID: null
      └─ Insumos Totales: 3 items

    📦 DETALLE DE INSUMOS RECIBIDOS:
      [1] Insumo ID: 22, Cantidad: 2.5
      [2] Insumo ID: 15, Cantidad: 1.0
      [3] Insumo ID: 8, Cantidad: 3.0
    ```

- **Archivo:** `TratamientoController.java` - sección de comprobante (líneas 361-414)
  - Agregado logging en generación de comprobante:
    ```
    🧾 PROCESAMIENTO DE COMPROBANTE:
      ✓ Comprobante EXISTENTE encontrado: #45 (COMP-2025-00123)
      └─ Agregando 3 insumos al comprobante existente
         ✓ Detalle agregado: Anestesia Lidocaína x 2.5
         ✓ Detalle agregado: Gasas Estériles x 1.0
         ✓ Detalle agregado: Guantes Látex x 3.0

    ✅ TRATAMIENTO COMPLETADO EXITOSAMENTE
      ├─ Tratamiento ID: 678
      ├─ Comprobante ID: 45
      └─ Número Comprobante: COMP-2025-00123
    ================================================================================
    ```

- **Archivo:** `TratamientoController.java` - endpoint `/planificar` (líneas 410-471)
  - Logging similar para tratamientos planificados
  ```java
  System.out.println("📥 ENDPOINT /planificar - Datos recibidos:");
  System.out.println("  Insumos recibidos: " + insumosTotales.size());
  System.out.println("✓ Insumos guardados en JSON: " + insumosJson);
  ```

---

### 3. ✅ Documentación SQL Creada

**Archivo Creado:** `docs/sql-migration-add-insumos-json.sql`

- Script SQL documentado para agregar columna `insumos_json`
- Incluye nota de que Hibernate lo crea automáticamente (ddl-auto=update)
- Útil para referencia y aplicación manual si es necesario

---

## 📋 Archivos Modificados

```
src/main/java/com/odontoapp/
  ├── entidad/
  │   └── TratamientoPlanificado.java            [MODIFICADO] - Campo insumosJson agregado
  └── controlador/
      └── TratamientoController.java             [MODIFICADO] - Logging y guardado de insumos

src/main/resources/templates/modulos/citas/
  └── modal-tratamiento-avanzado.html            [MODIFICADO] - Logging en frontend

docs/
  ├── sql-migration-add-insumos-json.sql         [NUEVO] - Script de migración SQL
  └── CHANGELOG-2025-11-20-tratamientos-e-inventario.md [ESTE ARCHIVO]
```

---

## 🧪 Próximos Pasos para Probar

### Test 1: Planificar Tratamiento con Insumos Modificados
1. Ir a una cita con estado "ASISTIO"
2. Abrir modal "Registrar Tratamiento Dental"
3. Seleccionar un procedimiento (ej: Endodoncia)
4. **MODIFICAR** las cantidades de insumos predeterminados
5. **AGREGAR** insumos adicionales con el botón "+"
6. Clic en "Planificar para Después"
7. **Verificar en consola del navegador:**
   - Debería mostrar: `📦 RESUMEN DE INSUMOS: Total insumos a enviar: X`
   - Debería listar cada insumo con su ID y cantidad modificada
8. **Verificar en logs del servidor:**
   - Debería mostrar: `📥 ENDPOINT /planificar - Datos recibidos`
   - Debería mostrar: `✓ Insumos guardados en JSON: [...]`

### Test 2: Realizar Tratamiento Inmediato
1. Abrir modal "Registrar Tratamiento Dental"
2. Seleccionar procedimiento y modificar insumos
3. Clic en "Tratamiento Realizado Ahora"
4. **Verificar en consola del navegador:**
   - Logging detallado de recopilación de insumos
5. **Verificar en logs del servidor:**
   - `📥 ENDPOINT /realizar-inmediato INICIADO`
   - `📦 DETALLE DE INSUMOS RECIBIDOS: [...]`
   - `🧾 PROCESAMIENTO DE COMPROBANTE:`
   - `✅ TRATAMIENTO COMPLETADO EXITOSAMENTE`
6. **Verificar en base de datos:**
   - Nuevo registro en `tratamientos_realizados`
   - Comprobante generado con detalles de insumos
   - Stock descontado correctamente

### Test 3: Verificar Inventario
1. Ir a módulo de Insumos
2. Buscar el insumo usado en el tratamiento
3. Clic en botón "Historial"  (⏱️)
4. **Verificar:**
   - Nuevo movimiento de SALIDA registrado
   - Cantidad descontada coincide con la cantidad modificada (no la predeterminada)
   - Referencia: "Cita #X - Tratamiento inmediato"

---

## 🔍 Cómo Interpretar los Logs

### Si NO se envían insumos desde el frontend:
```
⚠️ ADVERTENCIA: No se recibieron insumos o la lista está vacía
```
**Posible causa:** Los inputs de cantidad no tienen el atributo `data-insumo-id` o no se están capturando.

### Si sí se envían insumos:
```
📦 DETALLE DE INSUMOS RECIBIDOS:
  [1] Insumo ID: 22, Cantidad: 2.5
  [2] Insumo ID: 15, Cantidad: 1.0
```
**Esto confirma:** El frontend está enviando correctamente y el backend está recibiendo.

### Si el comprobante no se genera:
- Buscar líneas con `🧾 PROCESAMIENTO DE COMPROBANTE:`
- Si no aparecen, hay una excepción antes de llegar ahí
- Revisar stack trace en los logs

---

## ⚠️ Notas Importantes

1. **Hibernate Auto-Update:** La columna `insumos_json` se creará automáticamente al iniciar la aplicación gracias a `spring.jpa.hibernate.ddl-auto=update`

2. **JSON Format:** Los insumos se guardan con este formato:
   ```json
   [
     {"insumoId": "22", "cantidad": "2.5"},
     {"insumoId": "15", "cantidad": "1.0"}
   ]
   ```

3. **Próxima Funcionalidad Pendiente:**
   - Cuando se realice un tratamiento planificado en el futuro, recuperar el `insumosJson` y usarlo para descontar inventario
   - Actualmente solo se guarda, falta la lógica de recuperación

---

## 📞 Soporte

Si encuentras algún problema:

1. **Revisar logs del navegador:** Presiona F12 > Console
2. **Revisar logs del servidor:** Terminal donde corre Spring Boot
3. **Verificar base de datos:**
   ```sql
   SELECT id, insumos_json FROM tratamientos_planificados ORDER BY id DESC LIMIT 5;
   ```
4. **Reportar issue** con los logs completos

---

**Autor:** Claude Code
**Sesión:** claude/review-admin-appointments-01JLjbkqtGUd2gVyXktmSJWH
**Fecha:** 2025-11-20
