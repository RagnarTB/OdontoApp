# Sistema Global de Loading para Botones

## Descripción

Sistema implementado para prevenir clicks múltiples y mostrar estado de carga en botones de acción en toda la aplicación OdontoApp.

## Archivos Creados/Modificados

### 1. `/src/main/resources/static/js/button-loading.js`
**Nuevo archivo** que contiene:
- Clase `ButtonLoader`: Maneja el estado de loading de botones individuales
- Función global `setButtonLoading()`: API simple para controlar loading
- Auto-loading automático para formularios y botones AJAX
- Helper `createLoadingButton()`: Genera HTML de botones con loading integrado

### 2. `/src/main/resources/templates/layout/base.html`
**Modificado**: Agregada inclusión del script `button-loading.js` después de jQuery y antes de scripts personalizados.

### 3. `/src/main/resources/templates/modulos/pacientes/formulario.html`
**Modificado**: Actualizado botón de guardar para usar el sistema de loading.

---

## Cómo Usar

### Opción 1: Auto-Loading en Formularios (Recomendado)

Agregar el atributo `data-auto-loading="true"` al formulario:

```html
<form action="/pacientes/guardar" method="post" data-auto-loading="true">
    <button type="submit" data-loading-text="Guardando paciente...">
        <i class="fas fa-save"></i> Guardar
    </button>
</form>
```

**Características**:
- Se activa automáticamente al hacer submit
- Valida el formulario antes de mostrar loading
- Deshabilita el botón para prevenir clicks múltiples
- Muestra texto personalizado (configurablecon `data-loading-text`)

---

### Opción 2: Botones AJAX

Para botones que hacen peticiones AJAX:

```html
<button class="btn btn-primary"
        data-ajax-action
        data-loading-text="Procesando...">
    <i class="fas fa-check"></i> Confirmar
</button>
```

Luego en el JavaScript:

```javascript
$('#btnConfirmar').on('click', function() {
    // El loading se activa automáticamente por el data-ajax-action

    $.ajax({
        url: '/api/confirmar',
        method: 'POST',
        data: {...},
        success: function(response) {
            // Detener loading
            setButtonLoading('#btnConfirmar', false);
            // ... resto del código
        },
        error: function() {
            // Detener loading
            setButtonLoading('#btnConfirmar', false);
        }
    });
});
```

---

### Opción 3: Control Manual

Para casos donde necesitas control total:

```javascript
// Iniciar loading
setButtonLoading('#miBoton', true, {
    loadingText: 'Procesando...',
    loadingIcon: 'fa-spinner fa-spin'
});

// Hacer la operación...
await realizarOperacion();

// Detener loading
setButtonLoading('#miBoton', false);
```

---

## Ejemplos de Implementación

### Ejemplo 1: Guardar Usuario

```html
<form action="/usuarios/guardar" method="post" data-auto-loading="true">
    <!-- campos del formulario -->
    <button type="submit" data-loading-text="Guardando usuario...">
        <i class="fas fa-save"></i> Guardar Usuario
    </button>
</form>
```

### Ejemplo 2: Confirmar Cita

```html
<button id="btnConfirmarCita"
        class="btn btn-success"
        data-ajax-action
        data-loading-text="Confirmando cita...">
    <i class="fas fa-check"></i> Confirmar Cita
</button>
```

```javascript
$('#btnConfirmarCita').on('click', function() {
    const citaId = $(this).data('cita-id');

    $.post('/citas/confirmar/' + citaId)
        .done(function() {
            setButtonLoading('#btnConfirmarCita', false);
            Swal.fire('Éxito', 'Cita confirmada', 'success');
        })
        .fail(function() {
            setButtonLoading('#btnConfirmarCita', false);
            Swal.fire('Error', 'No se pudo confirmar', 'error');
        });
});
```

### Ejemplo 3: Botón de Asistió

```javascript
$('.btn-asistio').on('click', function() {
    const $btn = $(this);
    const citaId = $btn.data('cita-id');

    // Activar loading manualmente
    setButtonLoading($btn, true, {
        loadingText: 'Registrando...'
    });

    $.post(`/citas/marcar-asistio/${citaId}`)
        .done(function(response) {
            Swal.fire('Éxito', 'Asistencia registrada', 'success');
            recargarCalendario();
        })
        .fail(function(error) {
            Swal.fire('Error', error.responseJSON?.mensaje || 'Error al registrar', 'error');
        })
        .always(function() {
            setButtonLoading($btn, false);
        });
});
```

---

## Configuración Avanzada

### Personalizar Apariencia

```javascript
const loader = new ButtonLoader('#miBoton', {
    loadingText: 'Aguarde...',
    loadingIcon: 'fa-circle-notch fa-spin',
    disabled: true  // false si quieres que siga habilitado
});

loader.start();  // Iniciar
loader.stop();   // Detener
loader.reset();  // Resetear completamente
```

### Crear Botón con HTML Helper

```javascript
const html = createLoadingButton({
    text: 'Guardar Cambios',
    icon: 'fa-save',
    btnClass: 'btn-primary btn-lg',
    loadingText: 'Guardando...',
    type: 'submit',
    id: 'btnGuardar'
});

$('#contenedor').html(html);
```

---

## Puntos de Aplicación Recomendados

### ✅ Ya Implementado
1. **Formulario de Pacientes** - Auto-loading activado

### 🔄 Pendientes de Implementación

#### Formularios:
2. **Usuarios** - `/modulos/usuarios/formulario.html`
3. **Insumos** - `/modulos/insumos/formulario.html`
4. **Servicios/Procedimientos** - `/modulos/servicios/formulario.html`
5. **Roles** - `/modulos/roles/formulario.html`

#### Operaciones de Citas (`/modulos/citas/calendario.html`):
6. **Agendar Nueva Cita** - Modal de nueva cita
7. **Confirmar Cita** - Botón confirmar en modal detalle
8. **Marcar Asistió** - Botón asistió en lista
9. **Reprogramar Cita** - Formulario de reprogramación
10. **Cancelar Cita** - Confirmación de cancelación

#### Tratamientos (`/modulos/citas/modal-tratamiento-avanzado.html`):
11. **Registrar Tratamiento Inmediato** - Botón "Tratamiento Realizado Ahora"
12. **Planificar Tratamiento** - Botón "Planificar para Después"

---

## Beneficios

✅ **Previene clicks múltiples** - El botón se deshabilita automáticamente
✅ **Mejora UX** - Feedback visual al usuario
✅ **Fácil de usar** - Solo agregar un data-attribute
✅ **Global** - Funciona en toda la aplicación
✅ **Personalizable** - Textos e iconos configurables
✅ **Sin conflictos** - No interfiere con validaciones HTML5
✅ **Restauración automática** - Los botones se restauran al recargar página

---

## Notas Técnicas

- **Dependencias**: jQuery (ya incluido en el proyecto)
- **Compatibilidad**: Todos los navegadores modernos
- **Performance**: Ligero, sin impacto en rendimiento
- **Mantenimiento**: Un solo archivo centralizado

---

## Solución de Problemas

### El loading no se activa
- Verificar que jQuery esté cargado antes
- Verificar que `button-loading.js` esté incluido en `base.html`
- Verificar que el botón tenga `data-auto-loading="true"` en el form

### El loading no se detiene
- Asegurar que se llame `setButtonLoading(selector, false)` en `.always()`
- Verificar que no haya errores JavaScript que interrumpan la ejecución

### El formulario no se envía
- Verificar validaciones HTML5 (`required`, etc.)
- Revisar console del navegador para errores

---

## Contacto

Para dudas o mejoras, contactar al equipo de desarrollo.
