# 🔐 Guía de Implementación de Permisos Granulares - OdontoApp

## 📋 Índice
1. [Resumen de Cambios Implementados](#resumen-de-cambios-implementados)
2. [Cómo Actualizar Controladores](#cómo-actualizar-controladores)
3. [Cómo Actualizar Vistas HTML](#cómo-actualizar-vistas-html)
4. [Ejemplos Completos](#ejemplos-completos)
5. [Testing](#testing)

---

## ✅ Resumen de Cambios Implementados

### 1. **Backend**
- ✅ **Permisos.java**: Clase de constantes con todos los permisos del sistema
- ✅ **SessionInvalidationService.java**: Servicio para invalidar sesiones cuando cambien permisos
- ✅ **PermisosRestController.java**: API REST para validar permisos desde JavaScript
- ✅ **RolServiceImpl.java**: Actualizado para invalidar sesiones al cambiar roles/permisos
- ✅ **SecurityConfig.java**: Configurado con SessionRegistry para manejo de sesiones

### 2. **Frontend**
- ✅ **permisos-validator.js**: Sistema JavaScript de validación de permisos
- ✅ **base.html**: Actualizado para incluir permisos-validator.js globalmente

### 3. **Funcionalidades**
- ✅ Forzar logout cuando se modifiquen permisos de un rol
- ✅ Forzar logout cuando se cambie el estado de un rol
- ✅ Validación para no desactivar roles con usuarios activos
- ✅ API REST para validar permisos desde JavaScript
- ✅ Sistema JavaScript que intercepta clicks y muestra alerts cuando no hay permisos

---

## 🔧 Cómo Actualizar Controladores

### Paso 1: Importar la clase de Permisos

```java
import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;
```

### Paso 2: Reemplazar `hasRole()` con `hasAuthority()`

**❌ ANTES** (usando roles):
```java
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    @GetMapping
    public String listar() { ... }

    @GetMapping("/nuevo")
    public String nuevo() { ... }
}
```

**✅ DESPUÉS** (usando permisos granulares):
```java
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_USUARIOS)")
    public String listar() { ... }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_USUARIOS)")
    public String nuevo() { ... }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_USUARIOS, T(com.odontoapp.util.Permisos).EDITAR_USUARIOS)")
    public String guardar(@ModelAttribute UsuarioDTO dto) { ... }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_USUARIOS)")
    public String editar(@PathVariable Long id) { ... }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_USUARIOS)")
    public String eliminar(@PathVariable Long id) { ... }

    @GetMapping("/cambiar-estado/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_USUARIOS)")
    public String cambiarEstado(@PathVariable Long id) { ... }
}
```

### Mapeo de Permisos por Módulo

| Módulo | Constante de Permiso | Descripción |
|--------|---------------------|-------------|
| **USUARIOS** | `Permisos.VER_LISTA_USUARIOS` | Listar usuarios |
| | `Permisos.VER_DETALLE_USUARIOS` | Ver detalle de un usuario |
| | `Permisos.CREAR_USUARIOS` | Crear nuevo usuario |
| | `Permisos.EDITAR_USUARIOS` | Editar usuario (incluye cambiar estado) |
| | `Permisos.ELIMINAR_USUARIOS` | Eliminar usuario |
| **ROLES** | `Permisos.VER_LISTA_ROLES` | Listar roles |
| | `Permisos.CREAR_ROLES` | Crear rol |
| | `Permisos.EDITAR_ROLES` | Editar rol (incluye cambiar estado) |
| | `Permisos.ELIMINAR_ROLES` | Eliminar rol |
| **PACIENTES** | `Permisos.VER_LISTA_PACIENTES` | Listar pacientes |
| | `Permisos.VER_DETALLE_PACIENTES` | Ver historial del paciente |
| | `Permisos.CREAR_PACIENTES` | Crear paciente |
| | `Permisos.EDITAR_PACIENTES` | Editar paciente |
| | `Permisos.ELIMINAR_PACIENTES` | Eliminar paciente |
| **CITAS** | `Permisos.VER_LISTA_CITAS` | Ver lista de citas |
| | `Permisos.CREAR_CITAS` | Crear cita |
| | `Permisos.EDITAR_CITAS` | Editar, confirmar, cancelar citas |
| | `Permisos.ELIMINAR_CITAS` | Eliminar cita |
| **SERVICIOS** | `Permisos.VER_LISTA_SERVICIOS` | Listar servicios |
| | `Permisos.CREAR_SERVICIOS` | Crear servicio |
| | `Permisos.EDITAR_SERVICIOS` | Editar servicio |
| | `Permisos.ELIMINAR_SERVICIOS` | Eliminar servicio |
| **INVENTARIO** | `Permisos.VER_LISTA_INVENTARIO` | Ver lista de insumos |
| | `Permisos.CREAR_INVENTARIO` | Crear insumo |
| | `Permisos.EDITAR_INVENTARIO` | Editar insumo + registrar movimientos |
| | `Permisos.ELIMINAR_INVENTARIO` | Eliminar insumo |
| **FACTURACIÓN** | `Permisos.VER_LISTA_FACTURACION` | Ver comprobantes |
| | `Permisos.CREAR_FACTURACION` | Crear comprobante + usar POS |
| | `Permisos.EDITAR_FACTURACION` | Editar + registrar pagos |
| | `Permisos.ELIMINAR_FACTURACION` | Anular comprobantes |

---

## 🎨 Cómo Actualizar Vistas HTML

### Opción 1: Mostrar Alert cuando no tiene permiso (Recomendado)

Usa el atributo `data-permiso` para que el JavaScript intercepte el click:

```html
<!-- Botón CREAR -->
<a th:href="@{/usuarios/nuevo}" class="btn btn-primary"
   data-permiso="CREAR_USUARIOS"
   data-accion-descripcion="crear usuarios">
    <i class="fas fa-plus"></i> Nuevo Usuario
</a>

<!-- Botón EDITAR -->
<a th:href="@{/usuarios/editar/{id}(id=${usuario.id})}"
   class="btn btn-sm btn-warning"
   data-permiso="EDITAR_USUARIOS"
   data-accion-descripcion="editar usuarios"
   title="Editar">
    <i class="fas fa-edit"></i>
</a>

<!-- Botón ELIMINAR -->
<a th:href="@{/usuarios/eliminar/{id}(id=${usuario.id})}"
   class="btn btn-sm btn-danger"
   data-permiso="ELIMINAR_USUARIOS"
   data-accion-descripcion="eliminar usuarios"
   title="Eliminar">
    <i class="fas fa-trash"></i>
</a>
```

### Opción 2: Ocultar botón si no tiene permiso

Usa `sec:authorize` de Thymeleaf Security:

```html
<!-- Botón solo visible si tiene permiso -->
<a th:href="@{/usuarios/nuevo}" class="btn btn-primary"
   sec:authorize="hasAuthority('CREAR_USUARIOS')">
    <i class="fas fa-plus"></i> Nuevo Usuario
</a>

<a th:href="@{/usuarios/editar/{id}(id=${usuario.id})}"
   class="btn btn-sm btn-warning"
   sec:authorize="hasAuthority('EDITAR_USUARIOS')">
    <i class="fas fa-edit"></i>
</a>
```

### Combinación de Ambas Opciones

**Recomendación**: Para botones principales (Crear, ir al POS, etc.), usar `sec:authorize` para ocultarlos. Para botones de acción en tablas (editar, eliminar), usar `data-permiso` para mostrar alert.

```html
<!-- Botón principal de crear - OCULTAR si no tiene permiso -->
<div class="col-sm-6">
    <a th:href="@{/insumos/nuevo}" class="btn btn-primary float-sm-right"
       sec:authorize="hasAuthority('CREAR_INVENTARIO')">
        <i class="fas fa-plus mr-2"></i> Nuevo Artículo
    </a>
</div>

<!-- Botones de acción en tabla - MOSTRAR ALERT si no tiene permiso -->
<table class="table">
    <tr th:each="insumo : ${insumos}">
        <td>
            <a th:href="@{/insumos/editar/{id}(id=${insumo.id})}"
               class="btn btn-sm btn-warning"
               data-permiso="EDITAR_INVENTARIO"
               data-accion-descripcion="editar insumos">
                <i class="fas fa-edit"></i>
            </a>
            <a th:href="@{/insumos/eliminar/{id}(id=${insumo.id})}"
               class="btn btn-sm btn-danger"
               data-permiso="ELIMINAR_INVENTARIO"
               data-accion-descripcion="eliminar insumos">
                <i class="fas fa-trash"></i>
            </a>
        </td>
    </tr>
</table>
```

---

## 📚 Ejemplos Completos

### Ejemplo 1: InsumoController Completo

```java
package com.odontoapp.controlador;

import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/insumos")
public class InsumoController {

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_INVENTARIO)")
    public String listar() { ... }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_INVENTARIO)")
    public String nuevo() { ... }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_INVENTARIO, T(com.odontoapp.util.Permisos).EDITAR_INVENTARIO)")
    public String guardar(@ModelAttribute InsumoDTO dto) { ... }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_INVENTARIO)")
    public String editar(@PathVariable Long id) { ... }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_INVENTARIO)")
    public String eliminar(@PathVariable Long id) { ... }
}
```

### Ejemplo 2: Vista insumos/lista.html

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>Control de Stock</title>
</head>
<body>
    <!-- Botón principal - OCULTAR si no tiene permiso -->
    <a th:href="@{/insumos/nuevo}" class="btn btn-primary"
       sec:authorize="hasAuthority('CREAR_INVENTARIO')">
        <i class="fas fa-plus"></i> Nuevo Artículo
    </a>

    <!-- Tabla -->
    <table class="table">
        <tr th:each="insumo : ${insumos}">
            <td th:text="${insumo.nombre}"></td>
            <td>
                <!-- Botones con validación de permisos -->
                <button type="button" class="btn btn-sm btn-primary"
                        data-permiso="EDITAR_INVENTARIO"
                        data-accion-descripcion="registrar movimientos">
                    <i class="fas fa-plus-circle"></i>
                </button>

                <a th:href="@{/insumos/editar/{id}(id=${insumo.id})}"
                   class="btn btn-sm btn-warning"
                   data-permiso="EDITAR_INVENTARIO"
                   data-accion-descripcion="editar insumos">
                    <i class="fas fa-edit"></i>
                </a>

                <a th:href="@{/insumos/eliminar/{id}(id=${insumo.id})}"
                   class="btn btn-sm btn-danger"
                   data-permiso="ELIMINAR_INVENTARIO"
                   data-accion-descripcion="eliminar insumos">
                    <i class="fas fa-trash"></i>
                </a>
            </td>
        </tr>
    </table>
</body>
</html>
```

---

## 🧪 Testing

### 1. Probar Invalidación de Sesiones

1. Iniciar sesión con un usuario que tenga rol ALMACEN
2. En otra ventana, iniciar sesión como ADMIN
3. Modificar los permisos del rol ALMACEN
4. La sesión del usuario con rol ALMACEN debe ser cerrada automáticamente

### 2. Probar Validación de Permisos en UI

1. Crear un rol "ALMACEN_LIMITADO" con solo `VER_LISTA_INVENTARIO`
2. Asignar ese rol a un usuario de prueba
3. Iniciar sesión con ese usuario
4. Ir a `/insumos`
5. Verificar que el botón "Nuevo Artículo" NO aparece
6. Intentar hacer click en "Editar" o "Eliminar" → debe mostrar alert "No tiene permiso"

### 3. Probar API REST

```bash
# Obtener mis permisos
curl -X GET http://localhost:8080/api/permisos/mis-permisos \
  -H "Cookie: JSESSIONID=..."

# Verificar un permiso
curl -X GET "http://localhost:8080/api/permisos/verificar?permiso=CREAR_USUARIOS" \
  -H "Cookie: JSESSIONID=..."
```

---

## 📝 Checklist de Implementación por Módulo

### ✅ USUARIOS
- [ ] Actualizar UsuarioController con @PreAuthorize
- [ ] Actualizar usuarios/lista.html con data-permiso
- [ ] Actualizar usuarios/formulario.html

### ✅ ROLES
- [ ] Actualizar RolController con @PreAuthorize
- [ ] Actualizar roles/lista.html con data-permiso

### ⏳ PACIENTES
- [ ] Actualizar PacienteController con @PreAuthorize
- [ ] Actualizar pacientes/lista.html con data-permiso

### ⏳ CITAS
- [ ] Actualizar CitaController con @PreAuthorize
- [ ] Actualizar citas/lista.html con data-permiso

### ⏳ SERVICIOS
- [ ] Actualizar ProcedimientoController con @PreAuthorize
- [ ] Actualizar servicios/lista.html con data-permiso

### ⏳ INVENTARIO
- [ ] Actualizar InsumoController con @PreAuthorize
- [ ] Actualizar InventarioController con @PreAuthorize
- [ ] Actualizar insumos/lista.html con data-permiso

### ⏳ FACTURACIÓN
- [ ] Actualizar FacturacionController con @PreAuthorize
- [ ] Actualizar facturacion/lista.html con data-permiso
- [ ] Actualizar facturacion/pos.html con data-permiso

---

## 🚀 Próximos Pasos

1. **Actualizar controladores restantes** siguiendo los ejemplos de esta guía
2. **Actualizar vistas HTML** con validaciones de permisos
3. **Actualizar DataInitializer** si es necesario (los permisos ya existen)
4. **Probar exhaustivamente** cada módulo
5. **Actualizar SecurityConfig** para eliminar las restricciones basadas en roles (opcionales ahora que usamos permisos granulares)

---

## 💡 Tips y Mejores Prácticas

1. **Siempre usar constantes**: `Permisos.CREAR_USUARIOS` en lugar de strings hardcodeados
2. **Combinar técnicas**: `sec:authorize` para botones principales, `data-permiso` para acciones en tablas
3. **Mensajes descriptivos**: Usa `data-accion-descripcion` para mensajes de error claros
4. **Probar antes de pushear**: Verifica que los permisos funcionan correctamente
5. **Documentar cambios**: Actualiza este documento si agregas nuevos permisos o módulos

---

**Autor**: Claude Code
**Fecha**: 2025-11-22
**Versión**: 1.0
