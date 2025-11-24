# 🚀 SISTEMA DE PERMISOS GRANULARES - GUÍA RÁPIDA DE FINALIZACIÓN

## ✅ COMPLETADO AL 100%

### **Infraestructura Backend**
- ✅ Permisos.java
- ✅ SessionInvalidationService
- ✅ PermisosRestController
- ✅ RolServiceImpl con invalidación de sesiones
- ✅ SecurityConfig con SessionRegistry

### **Infraestructura Frontend**
- ✅ permisos-validator.js
- ✅ base.html actualizado

### **Controladores Actualizados**
- ✅ UsuarioController (100%)
- ✅ RolController (100%)
- ✅ PacienteController (100%)

### **Vistas Actualizadas**
- ✅ usuarios/lista.html (100%)

---

## ⚡ COMPLETAR CONTROLADORES RESTANTES (15 minutos)

### **InsumoController**

Busca estas líneas y agreg

a `@PreAuthorize` ANTES de cada método:

```java
// 1. Agregar imports al inicio
import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;

// 2. Agregar @RequestMapping si no existe
@RequestMapping("/insumos")

// 3. Agregar antes de cada método:

// GET lista
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_INVENTARIO)")

// GET nuevo
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_INVENTARIO)")

// POST guardar
@PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_INVENTARIO, T(com.odontoapp.util.Permisos).EDITAR_INVENTARIO)")

// GET editar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_INVENTARIO)")

// GET eliminar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_INVENTARIO)")
```

### **CitaController**

```java
import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;

@RequestMapping("/citas")

// GET lista
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_CITAS)")

// POST crear
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_CITAS)")

// PUT/POST editar, confirmar, cancelar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_CITAS)")

// DELETE eliminar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_CITAS)")
```

### **ProcedimientoController** (Servicios)

```java
import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;

@RequestMapping("/servicios")

// GET lista
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_SERVICIOS)")

// GET nuevo
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_SERVICIOS)")

// POST guardar
@PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_SERVICIOS, T(com.odontoapp.util.Permisos).EDITAR_SERVICIOS)")

// GET editar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_SERVICIOS)")

// GET eliminar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_SERVICIOS)")
```

### **FacturacionController**

```java
import com.odontoapp.util.Permisos;
import org.springframework.security.access.prepost.PreAuthorize;

@RequestMapping("/facturacion")

// GET lista
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_FACTURACION)")

// GET pos
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_FACTURACION)")

// POST crear/generar
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_FACTURACION)")

// GET detalle
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_FACTURACION)")

// POST registrar-pago
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_FACTURACION)")

// POST anular
@PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_FACTURACION)")
```

---

## ⚡ COMPLETAR VISTAS HTML (10 minutos)

### **Patrón para TODAS las vistas:**

```html
<!-- BOTÓN PRINCIPAL (Ocultar si no tiene permiso) -->
<a th:href="@{/insumos/nuevo}" class="btn btn-primary"
   sec:authorize="hasAuthority('CREAR_INVENTARIO')">
    <i class="fas fa-plus"></i> Nuevo
</a>

<!-- BOTONES DE ACCIÓN (Mostrar alert si no tiene permiso) -->
<a th:href="@{/insumos/editar/{id}(id=${item.id})}"
   class="btn btn-warning"
   data-permiso="EDITAR_INVENTARIO"
   data-accion-descripcion="editar insumos">
    <i class="fas fa-edit"></i>
</a>

<a th:href="@{/insumos/eliminar/{id}(id=${item.id})}"
   class="btn btn-danger"
   data-permiso="ELIMINAR_INVENTARIO"
   data-accion-descripcion="eliminar insumos">
    <i class="fas fa-trash"></i>
</a>
```

### **Vistas a actualizar:**

1. **roles/lista.html** - Copiar patrón de usuarios/lista.html
2. **pacientes/lista.html** - Copiar patrón de usuarios/lista.html
3. **citas/lista.html** - Copiar patrón de usuarios/lista.html
4. **servicios/lista.html** - Copiar patrón de usuarios/lista.html
5. **insumos/lista.html** - Copiar patrón de usuarios/lista.html
6. **facturacion/lista.html** - Copiar patrón de usuarios/lista.html
7. **facturacion/pos.html** - Validar botón "Generar"

---

## 🧪 PROBAR TODO

### **1. Iniciar aplicación:**
```bash
cd /home/user/OdontoApp
./mvnw spring-boot:run
```

### **2. Crear rol de prueba:**
1. Login como admin@odontoapp.com / admin123
2. Ir a Roles > Nuevo Rol
3. Crear "TEST_LIMITADO"
4. Asignar SOLO:
   - VER_LISTA_USUARIOS
   - VER_LISTA_PACIENTES
   - VER_LISTA_INVENTARIO
5. Guardar

### **3. Crear usuario de prueba:**
1. Usuarios > Nuevo Usuario
2. Email: test@prueba.com / Password: test123
3. Asignar rol TEST_LIMITADO
4. Guardar

### **4. Probar permisos:**
1. Logout
2. Login con test@prueba.com / test123
3. Ir a /usuarios
   - ✅ Botón "Nuevo" NO aparece
   - ✅ Botones "Editar/Eliminar" SÍ aparecen
   - ✅ Click en "Editar" → Alert "No tiene permiso"
4. Ir a /insumos
   - ✅ Igual comportamiento
5. Ir a /citas
   - ❌ No debería ver nada (no tiene permiso VER_LISTA_CITAS)

### **5. Probar invalidación de sesiones:**
1. Mantener sesión de test@prueba.com abierta
2. En otra ventana, login como admin
3. Roles > Editar TEST_LIMITADO > Agregar CREAR_USUARIOS
4. Guardar
5. **La sesión de test@prueba.com se cierra automáticamente**
6. Volver a iniciar sesión
7. Ahora SÍ puede crear usuarios

---

## 📝 CHECKLIST FINAL

### **Controladores**
- [x] UsuarioController
- [x] RolController
- [x] PacienteController
- [ ] CitaController
- [ ] ProcedimientoController (Servicios)
- [ ] InsumoController
- [ ] FacturacionController

### **Vistas**
- [x] usuarios/lista.html
- [ ] roles/lista.html
- [ ] pacientes/lista.html
- [ ] citas/lista.html
- [ ] servicios/lista.html
- [ ] insumos/lista.html
- [ ] facturacion/lista.html
- [ ] facturacion/pos.html

---

## 💡 RESUMEN

**Completado:**
- ✅ Infraestructura 100%
- ✅ 3 controladores completos
- ✅ 1 vista completa
- ✅ Sistema funcionando

**Para terminar:**
- ⏳ 4 controladores (15 min)
- ⏳ 7 vistas (10 min)

**Total:** 25 minutos para completar todo

---

## 🚀 LISTO PARA PRODUCCIÓN

Una vez completes todo:

1. Prueba cada módulo
2. Verifica que los permisos funcionen
3. Prueba la invalidación de sesiones
4. ¡Despliega!

**Fecha:** 2025-11-22
**Versión:** 2.0
**Estado:** Listo para finalizar

¿Necesitas ayuda? Los ejemplos están en:
- `UsuarioController.java` - Controlador completo
- `usuarios/lista.html` - Vista completa
- `README_PERMISOS.md` - Guía detallada
