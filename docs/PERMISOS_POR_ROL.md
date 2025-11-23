# Recomendaciones de Permisos por Rol

Este documento detalla los permisos recomendados para cada rol del sistema OdontoApp, basado en las responsabilidades típicas de cada usuario.

## Resumen de Roles

1. **ADMIN** - Administrador del Sistema
2. **ODONTOLOGO** - Odontólogo/Dentista
3. **RECEPCIONISTA** - Recepcionista/Asistente Administrativa
4. **PACIENTE** - Paciente (solo lectura de su propia información)
5. **ALMACEN** - Encargado de Almacén/Inventario

---

## 1. ROL: ADMIN (Administrador)

**Descripción**: Tiene acceso completo a todas las funcionalidades del sistema.

### Permisos Recomendados:

#### Usuarios
- ✅ VER_LISTA_USUARIOS
- ✅ VER_DETALLE_USUARIOS
- ✅ CREAR_USUARIOS
- ✅ EDITAR_USUARIOS
- ✅ ELIMINAR_USUARIOS
- ✅ RESTAURAR_USUARIOS

#### Roles
- ✅ VER_LISTA_ROLES
- ✅ VER_DETALLE_ROLES
- ✅ CREAR_ROLES
- ✅ EDITAR_ROLES
- ✅ ELIMINAR_ROLES
- ✅ RESTAURAR_ROLES

#### Pacientes
- ✅ VER_LISTA_PACIENTES
- ✅ VER_DETALLE_PACIENTES
- ✅ CREAR_PACIENTES
- ✅ EDITAR_PACIENTES
- ✅ ELIMINAR_PACIENTES
- ✅ RESTAURAR_PACIENTES

#### Citas
- ✅ VER_LISTA_CITAS
- ✅ VER_DETALLE_CITAS
- ✅ CREAR_CITAS
- ✅ EDITAR_CITAS
- ✅ ELIMINAR_CITAS

#### Servicios/Procedimientos
- ✅ VER_LISTA_SERVICIOS
- ✅ VER_DETALLE_SERVICIOS
- ✅ CREAR_SERVICIOS
- ✅ EDITAR_SERVICIOS
- ✅ ELIMINAR_SERVICIOS
- ✅ RESTAURAR_SERVICIOS

#### Facturación
- ✅ VER_LISTA_FACTURACION
- ✅ VER_DETALLE_FACTURACION
- ✅ CREAR_FACTURACION
- ✅ EDITAR_FACTURACION
- ✅ ELIMINAR_FACTURACION

#### Inventario
- ✅ VER_LISTA_INVENTARIO
- ✅ VER_DETALLE_INVENTARIO
- ✅ CREAR_INVENTARIO
- ✅ EDITAR_INVENTARIO
- ✅ ELIMINAR_INVENTARIO
- ✅ RESTAURAR_INVENTARIO

#### Tratamientos
- ✅ VER_LISTA_TRATAMIENTOS
- ✅ VER_DETALLE_TRATAMIENTOS
- ✅ CREAR_TRATAMIENTOS
- ✅ EDITAR_TRATAMIENTOS
- ✅ ELIMINAR_TRATAMIENTOS

#### Odontograma
- ✅ VER_LISTA_ODONTOGRAMA
- ✅ VER_DETALLE_ODONTOGRAMA
- ✅ CREAR_ODONTOGRAMA
- ✅ EDITAR_ODONTOGRAMA
- ✅ ELIMINAR_ODONTOGRAMA

#### Administración
- ✅ VER_REGISTROS_ELIMINADOS

**Total**: TODOS los permisos (39 permisos)

---

## 2. ROL: ODONTOLOGO (Odontólogo)

**Descripción**: Realiza diagnósticos, tratamientos y gestiona información clínica de pacientes.

### Permisos Recomendados:

#### Usuarios
- ❌ (No requiere acceso a gestión de usuarios)

#### Roles
- ❌ (No requiere acceso a gestión de roles)

#### Pacientes
- ✅ VER_LISTA_PACIENTES
- ✅ VER_DETALLE_PACIENTES
- ✅ CREAR_PACIENTES (puede registrar nuevos pacientes)
- ✅ EDITAR_PACIENTES (puede actualizar datos médicos)
- ❌ ELIMINAR_PACIENTES (solo Admin)
- ❌ RESTAURAR_PACIENTES (solo Admin)

#### Citas
- ✅ VER_LISTA_CITAS (ve todas las citas, especialmente las suyas)
- ✅ VER_DETALLE_CITAS
- ✅ CREAR_CITAS
- ✅ EDITAR_CITAS (confirmar, cancelar, reprogramar)
- ❌ ELIMINAR_CITAS (solo Admin o Recepcionista)

#### Servicios/Procedimientos
- ✅ VER_LISTA_SERVICIOS
- ✅ VER_DETALLE_SERVICIOS
- ❌ CREAR_SERVICIOS (solo Admin)
- ❌ EDITAR_SERVICIOS (solo Admin)
- ❌ ELIMINAR_SERVICIOS (solo Admin)
- ❌ RESTAURAR_SERVICIOS (solo Admin)

#### Facturación
- ✅ VER_LISTA_FACTURACION (puede ver facturas de sus pacientes)
- ✅ VER_DETALLE_FACTURACION
- ❌ CREAR_FACTURACION (generalmente lo hace Recepcionista)
- ❌ EDITAR_FACTURACION
- ❌ ELIMINAR_FACTURACION

#### Inventario
- ✅ VER_LISTA_INVENTARIO (consulta disponibilidad de insumos)
- ✅ VER_DETALLE_INVENTARIO
- ❌ CREAR_INVENTARIO (lo gestiona Almacén)
- ❌ EDITAR_INVENTARIO
- ❌ ELIMINAR_INVENTARIO
- ❌ RESTAURAR_INVENTARIO

#### Tratamientos
- ✅ VER_LISTA_TRATAMIENTOS
- ✅ VER_DETALLE_TRATAMIENTOS
- ✅ CREAR_TRATAMIENTOS
- ✅ EDITAR_TRATAMIENTOS
- ❌ ELIMINAR_TRATAMIENTOS (solo Admin)

#### Odontograma
- ✅ VER_LISTA_ODONTOGRAMA
- ✅ VER_DETALLE_ODONTOGRAMA
- ✅ CREAR_ODONTOGRAMA
- ✅ EDITAR_ODONTOGRAMA
- ❌ ELIMINAR_ODONTOGRAMA (solo Admin)

#### Administración
- ❌ VER_REGISTROS_ELIMINADOS (solo Admin)

**Total**: 20 permisos

---

## 3. ROL: RECEPCIONISTA

**Descripción**: Gestiona citas, facturación y atención al cliente. No accede a información clínica detallada.

### Permisos Recomendados:

#### Usuarios
- ❌ (No requiere acceso a gestión de usuarios)

#### Roles
- ❌ (No requiere acceso a gestión de roles)

#### Pacientes
- ✅ VER_LISTA_PACIENTES
- ✅ VER_DETALLE_PACIENTES (datos básicos, no historial clínico completo)
- ✅ CREAR_PACIENTES
- ✅ EDITAR_PACIENTES (datos de contacto, no datos médicos)
- ❌ ELIMINAR_PACIENTES (solo Admin)
- ❌ RESTAURAR_PACIENTES (solo Admin)

#### Citas
- ✅ VER_LISTA_CITAS
- ✅ VER_DETALLE_CITAS
- ✅ CREAR_CITAS
- ✅ EDITAR_CITAS (confirmar, cancelar, reprogramar)
- ✅ ELIMINAR_CITAS (puede cancelar citas)

#### Servicios/Procedimientos
- ✅ VER_LISTA_SERVICIOS (para cotizar servicios)
- ✅ VER_DETALLE_SERVICIOS
- ❌ CREAR_SERVICIOS (solo Admin)
- ❌ EDITAR_SERVICIOS (solo Admin)
- ❌ ELIMINAR_SERVICIOS (solo Admin)
- ❌ RESTAURAR_SERVICIOS (solo Admin)

#### Facturación
- ✅ VER_LISTA_FACTURACION
- ✅ VER_DETALLE_FACTURACION
- ✅ CREAR_FACTURACION (emite comprobantes, registra pagos)
- ✅ EDITAR_FACTURACION (registra pagos parciales)
- ❌ ELIMINAR_FACTURACION (solo Admin puede anular)

#### Inventario
- ✅ VER_LISTA_INVENTARIO (consulta disponibilidad)
- ✅ VER_DETALLE_INVENTARIO
- ❌ CREAR_INVENTARIO
- ❌ EDITAR_INVENTARIO
- ❌ ELIMINAR_INVENTARIO
- ❌ RESTAURAR_INVENTARIO

#### Tratamientos
- ✅ VER_LISTA_TRATAMIENTOS (para información al paciente)
- ✅ VER_DETALLE_TRATAMIENTOS
- ❌ CREAR_TRATAMIENTOS
- ❌ EDITAR_TRATAMIENTOS
- ❌ ELIMINAR_TRATAMIENTOS

#### Odontograma
- ❌ (No requiere acceso a odontograma)

#### Administración
- ❌ VER_REGISTROS_ELIMINADOS (solo Admin)

**Total**: 17 permisos

---

## 4. ROL: PACIENTE

**Descripción**: Solo tiene acceso de lectura a su propia información médica y citas.

### Permisos Recomendados:

**NOTA**: El rol PACIENTE tiene acceso restringido únicamente a sus propios datos. La lógica de filtrado debe implementarse en el backend para garantizar que cada paciente solo vea su información.

#### Usuarios
- ❌ (No requiere acceso a gestión de usuarios)

#### Roles
- ❌ (No requiere acceso a gestión de roles)

#### Pacientes
- ✅ VER_DETALLE_PACIENTES (solo su propio perfil)
- ✅ EDITAR_PACIENTES (puede actualizar datos personales básicos)
- ❌ VER_LISTA_PACIENTES
- ❌ CREAR_PACIENTES
- ❌ ELIMINAR_PACIENTES
- ❌ RESTAURAR_PACIENTES

#### Citas
- ✅ VER_LISTA_CITAS (solo sus propias citas)
- ✅ VER_DETALLE_CITAS (solo sus propias citas)
- ✅ CREAR_CITAS (puede agendar citas online si está habilitado)
- ❌ EDITAR_CITAS (debe contactar recepción)
- ❌ ELIMINAR_CITAS (debe contactar recepción)

#### Servicios/Procedimientos
- ✅ VER_LISTA_SERVICIOS (para conocer servicios disponibles)
- ✅ VER_DETALLE_SERVICIOS
- ❌ CREAR_SERVICIOS
- ❌ EDITAR_SERVICIOS
- ❌ ELIMINAR_SERVICIOS
- ❌ RESTAURAR_SERVICIOS

#### Facturación
- ✅ VER_LISTA_FACTURACION (solo sus propias facturas)
- ✅ VER_DETALLE_FACTURACION (solo sus propias facturas)
- ❌ CREAR_FACTURACION
- ❌ EDITAR_FACTURACION
- ❌ ELIMINAR_FACTURACION

#### Inventario
- ❌ (No requiere acceso a inventario)

#### Tratamientos
- ✅ VER_LISTA_TRATAMIENTOS (solo sus propios tratamientos)
- ✅ VER_DETALLE_TRATAMIENTOS (solo sus propios tratamientos)
- ❌ CREAR_TRATAMIENTOS
- ❌ EDITAR_TRATAMIENTOS
- ❌ ELIMINAR_TRATAMIENTOS

#### Odontograma
- ✅ VER_DETALLE_ODONTOGRAMA (solo su propio odontograma)
- ❌ VER_LISTA_ODONTOGRAMA
- ❌ CREAR_ODONTOGRAMA
- ❌ EDITAR_ODONTOGRAMA
- ❌ ELIMINAR_ODONTOGRAMA

#### Administración
- ❌ VER_REGISTROS_ELIMINADOS

**Total**: 11 permisos (con restricción a datos propios)

---

## 5. ROL: ALMACEN (Encargado de Almacén)

**Descripción**: Gestiona el inventario de insumos, registra entradas/salidas y controla stock.

### Permisos Recomendados:

#### Usuarios
- ❌ (No requiere acceso a gestión de usuarios)

#### Roles
- ❌ (No requiere acceso a gestión de roles)

#### Pacientes
- ❌ (No requiere acceso a gestión de pacientes)

#### Citas
- ❌ (No requiere acceso a gestión de citas)

#### Servicios/Procedimientos
- ✅ VER_LISTA_SERVICIOS (para conocer qué insumos se usan en cada servicio)
- ✅ VER_DETALLE_SERVICIOS
- ❌ CREAR_SERVICIOS
- ❌ EDITAR_SERVICIOS
- ❌ ELIMINAR_SERVICIOS
- ❌ RESTAURAR_SERVICIOS

#### Facturación
- ❌ (No requiere acceso a facturación)

#### Inventario
- ✅ VER_LISTA_INVENTARIO
- ✅ VER_DETALLE_INVENTARIO
- ✅ CREAR_INVENTARIO (registra nuevos insumos)
- ✅ EDITAR_INVENTARIO (registra movimientos, ajusta stock)
- ❌ ELIMINAR_INVENTARIO (solo Admin)
- ✅ RESTAURAR_INVENTARIO (puede restaurar insumos eliminados por error)

#### Tratamientos
- ❌ (No requiere acceso a tratamientos)

#### Odontograma
- ❌ (No requiere acceso a odontograma)

#### Administración
- ❌ VER_REGISTROS_ELIMINADOS (solo Admin)

**Total**: 7 permisos

---

## Resumen de Permisos por Rol

| Módulo | ADMIN | ODONTOLOGO | RECEPCIONISTA | PACIENTE | ALMACEN |
|--------|-------|------------|---------------|----------|---------|
| Usuarios | 6/6 | 0/6 | 0/6 | 0/6 | 0/6 |
| Roles | 6/6 | 0/6 | 0/6 | 0/6 | 0/6 |
| Pacientes | 6/6 | 4/6 | 4/6 | 2/6 | 0/6 |
| Citas | 5/5 | 4/5 | 5/5 | 3/5 | 0/5 |
| Servicios | 6/6 | 2/6 | 2/6 | 2/6 | 2/6 |
| Facturación | 5/5 | 2/5 | 4/5 | 2/5 | 0/5 |
| Inventario | 6/6 | 2/6 | 2/6 | 0/6 | 5/6 |
| Tratamientos | 5/5 | 4/5 | 2/5 | 2/5 | 0/5 |
| Odontograma | 5/5 | 4/5 | 0/5 | 1/5 | 0/5 |
| Administración | 1/1 | 0/1 | 0/1 | 0/1 | 0/1 |
| **TOTAL** | **51/51** | **22/51** | **19/51** | **12/51** | **7/51** |

---

## Roles Protegidos (No Editables)

Los siguientes roles deben marcarse como `esRolSistema = true` para evitar que sean modificados o eliminados:

1. ✅ **ADMIN** - Rol crítico del sistema
2. ✅ **ODONTOLOGO** - Rol fundamental para la operación clínica
3. ✅ **PACIENTE** - Rol requerido para el registro público
4. ❓ **RECEPCIONISTA** - Opcional, dependiendo de la estructura organizacional
5. ❓ **ALMACEN** - Opcional, puede ser personalizado según necesidades

### Recomendación:
- Marcar como protegidos: **ADMIN**, **ODONTOLOGO**, **PACIENTE**
- Permitir personalización: **RECEPCIONISTA**, **ALMACEN** (pueden variar según la clínica)

---

## Implementación

Para aplicar estos permisos, ejecutar el siguiente script SQL o implementar en el DataLoader:

```sql
-- Ejemplo para asignar permisos al rol ODONTOLOGO
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT
    (SELECT id FROM roles WHERE nombre = 'ODONTOLOGO'),
    id
FROM permisos
WHERE codigo IN (
    'VER_LISTA_PACIENTES',
    'VER_DETALLE_PACIENTES',
    'CREAR_PACIENTES',
    'EDITAR_PACIENTES',
    -- ... (continuar con todos los permisos del rol)
);
```

---

## Notas Finales

- Los permisos del **PACIENTE** deben estar acompañados de lógica de filtrado en el backend para garantizar que solo accedan a sus propios datos.
- El rol **ADMIN** debe asignarse con precaución y solo a usuarios de confianza.
- Los permisos de **RESTAURAR** generalmente solo deben otorgarse a ADMIN para mantener integridad de datos.
- Considerar crear roles adicionales personalizados según las necesidades específicas de cada clínica (ej: ASISTENTE, GERENTE, etc.)

---

**Fecha de creación**: 2025-11-23
**Versión**: 1.0
**Autor**: Sistema OdontoApp
