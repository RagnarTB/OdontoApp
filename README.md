# OdontoApp - Sistema de Gestión Odontológica

Sistema integral de gestión para clínicas dentales desarrollado con Spring Boot 3.3.0 y MySQL. Provee módulos completos para administración de pacientes, gestión de citas, tratamientos, inventario, facturación y roles de usuario.

## Tabla de Contenidos

- [Características Principales](#características-principales)
- [Tecnologías](#tecnologías)
- [Arquitectura del Sistema](#arquitectura-del-sistema)
- [Módulos del Sistema](#módulos-del-sistema)
- [Requisitos Previos](#requisitos-previos)
- [Instalación y Configuración](#instalación-y-configuración)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Base de Datos](#base-de-datos)
- [Seguridad](#seguridad)
- [API Endpoints](#api-endpoints)
- [Guía de Uso](#guía-de-uso)
- [Comandos Útiles](#comandos-útiles)

---

## Características Principales

### Gestión Integral
- Registro y administración de pacientes con historial clínico completo
- Sistema de citas con calendario interactivo (FullCalendar.js)
- Control de tratamientos realizados con seguimiento de insumos
- Gestión de inventario con movimientos trazables
- Facturación y punto de venta (POS) integrado
- Sistema de roles y permisos granular
- Notificaciones por email automatizadas

### Seguridad Avanzada
- Login dual diferenciado para personal clínico y pacientes
- Autenticación con Spring Security 6
- Cifrado de contraseñas con BCrypt
- Protección a nivel de método con @PreAuthorize
- Validación de sesiones y tokens de verificación
- Soft delete para mantener integridad referencial

### Características Técnicas
- Paginación en todas las listas
- Validación robusta de datos (Bean Validation)
- Integración con API de RENIEC para validación de DNI
- Exportación de datos a PDF
- Gestión de archivos adjuntos
- Auditoría automática de entidades
- Interfaz responsive con AdminLTE 3

---

## Tecnologías

### Backend
- **Spring Boot 3.3.0** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring Security 6** - Autenticación y autorización
- **Spring Mail** - Envío de correos electrónicos
- **Hibernate** - ORM
- **Bean Validation** - Validación de datos
- **Lombok** - Reducción de código boilerplate

### Frontend
- **Thymeleaf** - Motor de plantillas
- **AdminLTE 3** - Plantilla de administración
- **Bootstrap 4** - Framework CSS
- **jQuery 3.x** - Librería JavaScript
- **FullCalendar.js** - Calendario interactivo
- **SweetAlert2** - Alertas modernas
- **Select2** - Selectores mejorados
- **Tailwind CSS** - Estilos en vistas públicas

### Base de Datos
- **MySQL 8.0** - Sistema de gestión de base de datos

### Herramientas de Desarrollo
- **Maven** - Gestión de dependencias
- **Docker & Docker Compose** - Containerización
- **Git** - Control de versiones
- **Java 21** - Lenguaje de programación

---

## Arquitectura del Sistema

### Patrón de Diseño: MVC (Modelo-Vista-Controlador)

```
┌─────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Thymeleaf    │  │  AdminLTE    │  │  jQuery   │ │
│  │  Templates   │  │   Bootstrap  │  │  AJAX     │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                 CAPA DE CONTROLADORES               │
│  ┌──────────────────────────────────────────────┐  │
│  │  @Controller                                 │  │
│  │  - UsuarioController                         │  │
│  │  - PacienteController                        │  │
│  │  - CitaController                            │  │
│  │  - FacturacionController                     │  │
│  │  - InventarioController                      │  │
│  │  - DashboardController                       │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                  CAPA DE SERVICIOS                  │
│  ┌──────────────────────────────────────────────┐  │
│  │  @Service                                    │  │
│  │  - Lógica de negocio                         │  │
│  │  - Validaciones complejas                    │  │
│  │  - Transacciones                             │  │
│  │  - Integración con APIs externas            │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                CAPA DE PERSISTENCIA                 │
│  ┌──────────────────────────────────────────────┐  │
│  │  @Repository (Spring Data JPA)               │  │
│  │  - Acceso a datos                            │  │
│  │  - Consultas personalizadas                  │  │
│  │  - Paginación y ordenamiento                 │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                   BASE DE DATOS                     │
│              MySQL 8.0 (Dockerizado)                │
└─────────────────────────────────────────────────────┘
```

### Componentes Transversales

- **Seguridad**: DualLoginAuthenticationFilter, CustomAuthenticationSuccessHandler
- **DTOs**: Transferencia de datos entre capas
- **Auditoría**: EntidadAuditable (createdAt, updatedAt, createdBy)
- **Excepciones**: GlobalControllerAdvice para manejo centralizado
- **Utilidades**: PasswordUtil, validaciones personalizadas

---

## Módulos del Sistema

### 1. Gestión de Usuarios
**Descripción**: Administración de usuarios del sistema (personal clínico y administrativo).

**Funcionalidades**:
- Registro de usuarios con roles específicos
- Configuración de horarios regulares y excepciones (para odontólogos)
- Gestión de estados: activo, inactivo, bloqueado
- Integración con API RENIEC para validación de DNI
- Restauración de usuarios eliminados (soft delete)
- Cambio obligatorio de contraseña en primer acceso

**Roles disponibles**:
- ADMIN: Acceso total al sistema
- ODONTOLOGO: Gestión de citas, tratamientos, pacientes
- RECEPCIONISTA: Gestión de citas, pacientes, facturación
- AUXILIAR: Visualización y asistencia
- PACIENTE: Portal de paciente (solo lectura)

### 2. Gestión de Pacientes
**Descripción**: Registro y seguimiento de pacientes de la clínica.

**Funcionalidades**:
- Registro completo con datos personales y médicos
- Tipos de documento: DNI, Pasaporte, Carnet de Extranjería
- Consulta automática de datos mediante API RENIEC
- Historial clínico integrado
- Alergias y antecedentes médicos
- Soft delete con opción de restauración
- Auto-registro de pacientes desde portal público

### 3. Gestión de Citas
**Descripción**: Programación y control de citas odontológicas.

**Funcionalidades**:
- Calendario interactivo con FullCalendar.js
- Validación de disponibilidad de odontólogos
- Buffer de 15 minutos entre citas
- Estados: Pendiente, Confirmada, En proceso, Completada, Cancelada, No asistió
- Notificaciones por email (confirmación, cancelación, reprogramación, recordatorio)
- Vista de lista con filtros (estado, odontólogo, rango de fechas)
- Validación de horarios laborales y excepciones
- Reprogramación de citas existentes

### 4. Gestión de Tratamientos
**Descripción**: Registro de tratamientos realizados a pacientes.

**Funcionalidades**:
- Vinculación con citas y procedimientos
- Registro de insumos utilizados
- Descuento automático de stock
- Validación de stock disponible
- Autocompletado de fecha/hora actual
- Notas y observaciones detalladas
- Integración con historial del paciente

### 5. Gestión de Inventario
**Descripción**: Control de insumos odontológicos.

**Funcionalidades**:
- Categorización de insumos
- Unidades de medida configurables
- Gestión de stock (disponible, mínimo, máximo)
- Movimientos de inventario trazables:
  - Entradas: Compra, Devolución, Ajuste positivo
  - Salidas: Venta directa, Uso en tratamiento, Ajuste negativo
- Alertas de stock bajo
- Historial completo de movimientos
- Proveedores y precios

### 6. Facturación y Punto de Venta
**Descripción**: Generación de comprobantes y control de pagos.

**Funcionalidades**:
- Generación de comprobantes desde citas o venta directa
- Numeración automática correlativa (B001-0000001)
- Tipos de comprobante: CITA, VENTA_DIRECTA
- Estados de pago: Pendiente, Pagado Parcial, Pagado Total, Anulado
- Registro de pagos con múltiples métodos:
  - Efectivo
  - Yape/Transferencia (con referencia)
  - Pago Mixto (Efectivo + Yape)
- Validación de pagos mixtos (suma debe igualar total)
- Anulación de comprobantes (con reversión de stock)
- Autocompletado de fecha/hora en registro de pagos
- Listado de comprobantes pendientes con paginación

### 7. Procedimientos e Insumos
**Descripción**: Catálogo de servicios odontológicos.

**Funcionalidades**:
- Gestión de procedimientos con precios base
- Categorización de procedimientos
- Relación procedimiento-insumos (recetas)
- Descripción detallada de cada procedimiento
- Estado activo/inactivo

### 8. Dashboard y Reportes
**Descripción**: Panel de control con métricas clave.

**Funcionalidades**:
- Widgets informativos:
  - Total de pacientes activos
  - Citas del día
  - Ingresos del mes
  - Tratamientos pendientes
- Acceso rápido a funciones principales
- Dashboards diferenciados por rol

### 9. Autenticación y Registro
**Descripción**: Sistema dual de acceso al sistema.

**Funcionalidades**:
- Login diferenciado:
  - Portal de Personal Clínico
  - Portal de Pacientes
- Validación estricta de roles por portal
- Auto-registro de pacientes:
  1. Ingreso de email
  2. Verificación por correo
  3. Completar datos personales
  4. Activación automática
- Cambio de contraseña obligatorio
- Recuperación de contraseña (por implementar)

### 10. Gestión de Roles y Permisos
**Descripción**: Control de acceso basado en roles.

**Funcionalidades**:
- Creación de roles personalizados
- Asignación de permisos granulares
- Roles protegidos (ADMIN, PACIENTE, ODONTOLOGO)
- Protección contra eliminación de roles en uso

---

## Requisitos Previos

- Java Development Kit (JDK) 21
- Maven 3.8+
- Docker y Docker Compose
- MySQL 8.0 (si no se usa Docker)
- Git

---

## Instalación y Configuración

### Opción 1: Usando Docker (Recomendado)

1. **Clonar el repositorio**
```bash
git clone https://github.com/RagnarTB/OdontoApp.git
cd OdontoApp
```

2. **Configurar variables de entorno**
Editar `src/main/resources/application.properties` si es necesario.

3. **Construir la aplicación**
```bash
mvn clean package -DskipTests
```

4. **Iniciar con Docker Compose**
```bash
docker-compose up --build
```

La aplicación estará disponible en: `http://localhost:8080`

### Opción 2: Ejecución Local

1. **Clonar el repositorio**
```bash
git clone https://github.com/RagnarTB/OdontoApp.git
cd OdontoApp
```

2. **Configurar MySQL**
```sql
CREATE DATABASE odontoapp_db;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'leonardo';
GRANT ALL PRIVILEGES ON odontoapp_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

3. **Actualizar application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/odontoapp_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=leonardo
```

4. **Ejecutar la aplicación**
```bash
mvn spring-boot:run
```

### Configuración de Email

Actualizar credenciales SMTP en `application.properties`:
```properties
spring.mail.username=tu_email@gmail.com
spring.mail.password=tu_app_password
```

Para Gmail, generar una contraseña de aplicación en: https://myaccount.google.com/apppasswords

### Configuración de API RENIEC

Obtener token en: https://api.decolecta.com

Actualizar en `application.properties`:
```properties
api.decolecta.token=tu_token_aqui
```

Para desarrollo, el sistema usa datos de prueba con DNI `12345678`.

---

## Estructura del Proyecto

```
odontoapp/
├── src/
│   ├── main/
│   │   ├── java/com/odontoapp/
│   │   │   ├── configuracion/         # Configuraciones de Spring
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controlador/           # Controladores MVC
│   │   │   │   ├── CitaController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── FacturacionController.java
│   │   │   │   ├── InventarioController.java
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── PacienteController.java
│   │   │   │   ├── RegistroController.java
│   │   │   │   ├── TratamientoController.java
│   │   │   │   └── UsuarioController.java
│   │   │   ├── dto/                   # Data Transfer Objects
│   │   │   │   ├── CitaDTO.java
│   │   │   │   ├── ComprobanteDTO.java
│   │   │   │   ├── PacienteDTO.java
│   │   │   │   ├── PagoDTO.java
│   │   │   │   ├── ReniecResponseDTO.java
│   │   │   │   └── UsuarioDTO.java
│   │   │   ├── entidad/               # Entidades JPA
│   │   │   │   ├── Cita.java
│   │   │   │   ├── Comprobante.java
│   │   │   │   ├── DetalleComprobante.java
│   │   │   │   ├── EntidadAuditable.java
│   │   │   │   ├── EstadoCita.java
│   │   │   │   ├── EstadoPago.java
│   │   │   │   ├── HorarioExcepcion.java
│   │   │   │   ├── Insumo.java
│   │   │   │   ├── MetodoPago.java
│   │   │   │   ├── MovimientoInventario.java
│   │   │   │   ├── Paciente.java
│   │   │   │   ├── Pago.java
│   │   │   │   ├── Permiso.java
│   │   │   │   ├── Procedimiento.java
│   │   │   │   ├── Rol.java
│   │   │   │   ├── TratamientoRealizado.java
│   │   │   │   └── Usuario.java
│   │   │   ├── repositorio/           # Repositorios JPA
│   │   │   ├── seguridad/             # Configuración de seguridad
│   │   │   │   ├── CustomAuthenticationSuccessHandler.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── DualLoginAuthenticationFilter.java
│   │   │   ├── servicio/              # Interfaces de servicio
│   │   │   │   └── impl/              # Implementaciones
│   │   │   └── util/                  # Utilidades
│   │   │       └── PasswordUtil.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/                # Recursos estáticos
│   │       │   ├── adminlte/
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── templates/             # Plantillas Thymeleaf
│   │           ├── layout/
│   │           │   └── base.html
│   │           ├── modulos/
│   │           │   ├── categorias_insumo/
│   │           │   ├── citas/
│   │           │   ├── facturacion/
│   │           │   ├── insumos/
│   │           │   ├── pacientes/
│   │           │   ├── roles/
│   │           │   ├── servicios/
│   │           │   └── usuarios/
│   │           └── publico/
│   │               ├── login.html
│   │               ├── registro-email.html
│   │               └── registro-formulario.html
│   └── test/                          # Tests unitarios
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Base de Datos

### Modelo de Datos Principal

#### Entidades Core

**usuarios**
- Gestión de cuentas de usuario
- Campos: id, email, password, nombre_completo, tipo_documento_id, numero_documento
- Relaciones: rol (Many-to-One), paciente (One-to-One), horario_regular (JSON), excepciones_horario

**pacientes**
- Información clínica de pacientes
- Campos: id, usuario_id, fecha_nacimiento, telefono, direccion, alergias, antecedentes_medicos
- Relaciones: usuario, citas, tratamientos, comprobantes

**citas**
- Programación de citas
- Campos: id, paciente_id, odontologo_id, procedimiento_id, estado_cita_id, fecha_hora_inicio, fecha_hora_fin
- Relaciones: paciente, odontólogo, procedimiento, estado
- Validaciones: Buffer 15 min, horarios laborales

**tratamientos_realizados**
- Registro de tratamientos
- Campos: id, cita_id, procedimiento_id, insumo_ajustado_id, cantidad_insumo_ajustada, fecha_realizacion
- Relaciones: cita, procedimiento, insumo
- Funcionalidad: Descuento automático de stock

**comprobantes**
- Documentos de venta
- Campos: id, paciente_id, cita_id, numero_comprobante, tipo_comprobante, monto_total, monto_pagado, monto_pendiente, estado_pago_id
- Relaciones: paciente, cita, detalles, pagos, estado
- Numeración: Automática correlativa (B001-0000001)

**pagos**
- Registro de pagos
- Campos: id, comprobante_id, metodo_pago_id, fecha_pago, monto, referencia_yape, monto_efectivo, monto_yape
- Relaciones: comprobante, método de pago
- Validaciones: Pago mixto debe sumar al total

**insumos**
- Inventario de materiales
- Campos: id, nombre, categoria_id, unidad_medida, cantidad_disponible, stock_minimo, stock_maximo, precio_unitario
- Relaciones: categoría, movimientos, proveedores

**movimientos_inventario**
- Trazabilidad de stock
- Campos: id, insumo_id, tipo_movimiento_id, motivo_movimiento_id, cantidad, referencia
- Relaciones: insumo, tipo, motivo
- Tipos: ENTRADA, SALIDA

#### Catálogos

- **roles**: ADMIN, ODONTOLOGO, RECEPCIONISTA, AUXILIAR, PACIENTE
- **permisos**: Acciones específicas del sistema
- **estados_cita**: PENDIENTE, CONFIRMADA, EN_PROCESO, COMPLETADA, CANCELADA, NO_ASISTIO
- **estados_pago**: PENDIENTE, PAGADO_PARCIAL, PAGADO_TOTAL, ANULADO
- **metodos_pago**: EFECTIVO, YAPE, TRANSFERENCIA, TARJETA, MIXTO
- **tipos_documento**: DNI, PASAPORTE, CARNET_EXTRANJERIA
- **categorias_insumo**: Consumibles, Instrumental, Medicamentos, etc.

### Características de la Base de Datos

- **Soft Delete**: Entidades marcadas como eliminadas sin borrado físico
- **Auditoría**: Campos created_at, updated_at, created_by en todas las entidades
- **Integridad Referencial**: Claves foráneas con restricciones
- **Índices**: En campos de búsqueda frecuente
- **JSON Storage**: Horarios regulares en formato JSON

---

## Seguridad

### Arquitectura de Seguridad

#### 1. Login Dual

El sistema implementa dos portales de acceso diferenciados:

**Portal de Personal Clínico**
- Permite acceso a: ADMIN, ODONTOLOGO, RECEPCIONISTA, AUXILIAR
- Validación mediante `DualLoginAuthenticationFilter`
- Rechaza intentos de login de usuarios con rol PACIENTE

**Portal de Pacientes**
- Permite acceso solo a: PACIENTE
- Validación mediante `DualLoginAuthenticationFilter`
- Rechaza intentos de login de usuarios con otros roles

Implementación técnica:
```java
// Campo oculto en formulario
<input type="hidden" name="loginType" value="personal">

// Validación en DualLoginAuthenticationFilter
if ("paciente".equalsIgnoreCase(loginType)) {
    if (!esPaciente) {
        // Redirigir con error
    }
}
```

#### 2. Autenticación

- **Spring Security 6** con configuración personalizada
- **BCrypt** para hash de contraseñas
- **CustomUserDetailsService** para carga de usuarios desde base de datos
- **Tokens de verificación** para activación de cuentas
- **Sesiones** gestionadas por Spring Security

#### 3. Autorización

**A nivel de URL** (SecurityFilterChain):
```java
.requestMatchers("/login", "/registro/**", "/api/reniec").permitAll()
.requestMatchers("/cambiar-password-obligatorio").authenticated()
.anyRequest().authenticated()
```

**A nivel de método** (@PreAuthorize):
```java
@PreAuthorize("hasAuthority('ADMIN')")
public class UsuarioController { ... }

@PreAuthorize("hasAuthority('ADMIN')")
public class RolController { ... }
```

#### 4. Protecciones Adicionales

- **CSRF Protection**: Habilitado por defecto
- **Session Fixation**: Protección automática
- **Clickjacking**: Headers X-Frame-Options
- **XSS**: Escapado automático en Thymeleaf
- **SQL Injection**: Prevención mediante JPA/Hibernate

#### 5. Validación de Contraseñas

Requisitos obligatorios:
- Mínimo 8 caracteres
- Al menos una letra mayúscula
- Al menos una letra minúscula
- Al menos un número
- Al menos un carácter especial

Implementado en `PasswordUtil.validarPasswordRobusta()`.

---

## API Endpoints

### Autenticación

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/login` | Vista de login dual | Público |
| POST | `/login` | Procesar login | Público |
| GET | `/logout` | Cerrar sesión | Autenticado |
| GET | `/registro` | Inicio de registro de paciente | Público |
| POST | `/registro/enviar-link` | Enviar email de verificación | Público |
| GET | `/registro/completar` | Formulario de registro completo | Público (con token) |
| POST | `/registro/completar` | Completar registro | Público (con token) |

### Dashboard

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/dashboard` | Dashboard del personal | Autenticado (no PACIENTE) |
| GET | `/paciente/dashboard` | Dashboard del paciente | PACIENTE |

### Usuarios

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/usuarios` | Listar usuarios | ADMIN |
| GET | `/usuarios/nuevo` | Formulario nuevo usuario | ADMIN |
| POST | `/usuarios/guardar` | Guardar usuario | ADMIN |
| GET | `/usuarios/editar/{id}` | Formulario editar usuario | ADMIN |
| GET | `/usuarios/eliminar/{id}` | Eliminar usuario | ADMIN |
| GET | `/usuarios/cambiar-estado/{id}` | Cambiar estado (activo/inactivo) | ADMIN |
| GET | `/usuarios/desbloquear/{id}` | Desbloquear usuario | ADMIN |
| GET | `/usuarios/restablecer/{id}` | Restaurar usuario eliminado | ADMIN |
| GET | `/api/buscar-dni` | Buscar usuario por DNI | ADMIN |

### Pacientes

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/pacientes` | Listar pacientes | Autenticado |
| GET | `/pacientes/nuevo` | Formulario nuevo paciente | Autenticado |
| POST | `/pacientes/guardar` | Guardar paciente | Autenticado |
| GET | `/pacientes/editar/{id}` | Formulario editar paciente | Autenticado |
| GET | `/pacientes/eliminar/{id}` | Eliminar paciente | Autenticado |
| GET | `/pacientes/restablecer/{id}` | Restaurar paciente eliminado | Autenticado |
| GET | `/pacientes/historial/{id}` | Ver historial clínico | Autenticado |
| GET | `/api/reniec` | Consultar DNI en RENIEC | Público |

### Citas

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/citas` | Vista de calendario | Autenticado |
| GET | `/citas/lista` | Vista de lista con filtros | Autenticado |
| GET | `/citas/api/eventos` | Obtener eventos del calendario | Autenticado |
| POST | `/citas/agendar` | Agendar nueva cita | Autenticado |
| POST | `/citas/confirmar/{id}` | Confirmar cita | Autenticado |
| POST | `/citas/cancelar/{id}` | Cancelar cita | Autenticado |
| POST | `/citas/reprogramar/{id}` | Reprogramar cita | Autenticado |
| GET | `/citas/api/disponibilidad` | Verificar disponibilidad | Autenticado |

### Tratamientos

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| POST | `/tratamientos/registrar` | Registrar tratamiento | Autenticado |
| GET | `/tratamientos/historial/{pacienteId}` | Historial de tratamientos | Autenticado |

### Facturación

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/facturacion` | Listar comprobantes pendientes | Autenticado |
| GET | `/facturacion/pos` | Vista de punto de venta | Autenticado |
| POST | `/facturacion/generar-venta-directa` | Generar comprobante POS | Autenticado |
| POST | `/facturacion/registrar-pago` | Registrar pago | Autenticado |
| GET | `/facturacion/anular/{id}` | Anular comprobante | Autenticado |
| GET | `/facturacion/detalle/{id}` | Detalle de comprobante | Autenticado |
| GET | `/facturacion/paciente/{pacienteId}` | Comprobantes de paciente | Autenticado |

### Inventario

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/inventario` | Listar insumos | Autenticado |
| GET | `/inventario/nuevo` | Formulario nuevo insumo | Autenticado |
| POST | `/inventario/guardar` | Guardar insumo | Autenticado |
| GET | `/inventario/editar/{id}` | Formulario editar insumo | Autenticado |
| GET | `/inventario/eliminar/{id}` | Eliminar insumo | Autenticado |
| POST | `/inventario/movimiento` | Registrar movimiento | Autenticado |
| GET | `/inventario/historial/{id}` | Historial de movimientos | Autenticado |

### Procedimientos

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/procedimientos` | Listar procedimientos | Autenticado |
| GET | `/procedimientos/nuevo` | Formulario nuevo procedimiento | Autenticado |
| POST | `/procedimientos/guardar` | Guardar procedimiento | Autenticado |
| GET | `/procedimientos/editar/{id}` | Formulario editar procedimiento | Autenticado |
| GET | `/procedimientos/eliminar/{id}` | Eliminar procedimiento | Autenticado |

### Roles y Permisos

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/roles` | Listar roles | ADMIN |
| GET | `/roles/nuevo` | Formulario nuevo rol | ADMIN |
| POST | `/roles/guardar` | Guardar rol | ADMIN |
| GET | `/roles/editar/{id}` | Formulario editar rol | ADMIN |
| GET | `/roles/eliminar/{id}` | Eliminar rol | ADMIN |

---

## Guía de Uso

### Primer Acceso al Sistema

1. **Acceder a la aplicación**: `http://localhost:8080`

2. **Crear usuario ADMIN inicial**:
   - Ejecutar script SQL directamente en la base de datos:
   ```sql
   INSERT INTO roles (nombre, descripcion) VALUES ('ADMIN', 'Administrador del sistema');

   INSERT INTO usuarios (email, password, nombre_completo, esta_activo, debe_actualizar_password)
   VALUES ('admin@odontoapp.com',
           '$2a$10$encrypted_password_here',
           'Administrador',
           1,
           0);

   INSERT INTO usuario_roles (usuario_id, rol_id)
   VALUES (1, 1);
   ```

3. **Iniciar sesión** con las credenciales creadas.

### Configuración Inicial

1. **Crear roles adicionales**:
   - Ir a Roles → Nuevo
   - Crear: ODONTOLOGO, RECEPCIONISTA, AUXILIAR, PACIENTE
   - Asignar permisos correspondientes

2. **Crear catálogos base**:
   - Estados de cita
   - Estados de pago
   - Métodos de pago
   - Tipos de documento
   - Categorías de insumos
   - Categorías de procedimientos

3. **Registrar personal clínico**:
   - Usuarios → Nuevo
   - Completar datos personales
   - Asignar rol (ODONTOLOGO, RECEPCIONISTA, etc.)
   - Para odontólogos: configurar horario regular y excepciones

4. **Configurar procedimientos**:
   - Procedimientos → Nuevo
   - Definir nombre, descripción, precio base
   - Vincular insumos necesarios

5. **Registrar insumos en inventario**:
   - Inventario → Nuevo
   - Definir stock mínimo y máximo
   - Registrar movimiento de entrada inicial

### Flujo: Registro de Paciente (Auto-registro)

1. Paciente accede a `/registro`
2. Ingresa su email
3. Recibe correo de verificación
4. Hace clic en el enlace del correo
5. Completa formulario con:
   - Tipo y número de documento
   - Nombre completo (autocompletado si es DNI vía RENIEC)
   - Teléfono, dirección
   - Fecha de nacimiento
   - Alergias y antecedentes médicos
   - Contraseña segura
6. Sistema crea usuario con rol PACIENTE
7. Redirige a login de pacientes

### Flujo: Agendar Cita

1. Ir a Citas → Calendario
2. Hacer clic en fecha/hora deseada
3. Seleccionar:
   - Paciente
   - Odontólogo
   - Procedimiento
   - Fecha y hora de inicio
   - Duración estimada
4. Sistema valida:
   - Fecha no está en el pasado
   - Odontólogo está disponible
   - Respeta horario laboral
   - Buffer de 15 minutos entre citas
5. Guardar cita
6. Sistema envía email de confirmación al paciente

### Flujo: Registrar Tratamiento

1. En Calendario, hacer clic en cita completada
2. Abrir modal de registrar tratamiento
3. Completar:
   - Procedimiento realizado
   - Insumo utilizado (opcional)
   - Cantidad de insumo
   - Fecha de realización (autocompletada)
   - Notas
4. Sistema valida stock disponible
5. Guardar tratamiento
6. Sistema descuenta stock automáticamente

### Flujo: Generar Comprobante y Pago

**Desde cita**:
1. Marcar cita como "ASISTIO"
2. Generar comprobante desde cita
3. Sistema crea comprobante con procedimiento de la cita
4. Estado: PENDIENTE

**Venta directa (POS)**:
1. Ir a Facturación → POS
2. Seleccionar paciente
3. Agregar procedimientos/insumos al carrito
4. Generar comprobante

**Registrar pago**:
1. Ir a Facturación → Detalle del comprobante
2. Hacer clic en "Registrar Pago"
3. Seleccionar método de pago:
   - Efectivo: solo ingresar monto
   - Yape/Transferencia: ingresar referencia
   - Mixto: ingresar monto efectivo y monto Yape
4. Sistema valida que suma sea correcta (pago mixto)
5. Guardar pago
6. Sistema actualiza estado del comprobante

### Flujo: Gestión de Inventario

**Entrada de insumos**:
1. Ir a Inventario → Ver historial del insumo
2. Hacer clic en "Registrar Movimiento"
3. Tipo: ENTRADA
4. Motivo: Compra / Devolución / Ajuste positivo
5. Cantidad y referencia
6. Sistema incrementa stock

**Salida de insumos**:
1. Mismo proceso con Tipo: SALIDA
2. Motivo: Venta Directa / Uso en Tratamiento / Ajuste negativo
3. Sistema valida stock disponible
4. Sistema decrementa stock

**Automático**:
- Al registrar tratamiento con insumo: salida automática
- Al generar venta directa con insumo: salida automática
- Al anular comprobante de venta: entrada automática (reversión)

---

## Comandos Útiles

### Docker

```bash
# Iniciar contenedores
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener contenedores
docker-compose down

# Limpiar completamente (incluye base de datos)
docker-compose down -v

# Reconstruir imágenes
docker-compose up --build

# Acceder a MySQL en el contenedor
docker exec -it odontoapp-mysql mysql -u root -p
# Contraseña: leonardo
```

### MySQL

```bash
# Conectar a la base de datos
mysql -u root -p

# Comandos útiles
SHOW DATABASES;
USE odontoapp_db;
SHOW TABLES;
DESCRIBE usuarios;
SELECT * FROM usuarios;
exit
```

### Maven

```bash
# Compilar proyecto
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar aplicación
mvn clean package

# Empaquetar sin tests
mvn clean package -DskipTests

# Ejecutar aplicación
mvn spring-boot:run

# Limpiar target
mvn clean
```

### Git

```bash
# Ver estado
git status

# Agregar cambios
git add .
git add nombre_archivo

# Commit
git commit -m "Descripción del cambio"

# Push a rama específica
git push origin nombre_rama

# Ver historial
git log --oneline

# Ver diferencias
git diff

# Crear rama
git checkout -b nueva_rama

# Cambiar de rama
git checkout nombre_rama

# Ver ramas
git branch -a
```

### Aplicación

```bash
# Ver información de la aplicación
curl http://localhost:8080/actuator/health

# Acceso directo a endpoints
# Login personal
http://localhost:8080/login

# Dashboard
http://localhost:8080/dashboard

# Registro de pacientes
http://localhost:8080/registro
```

---

## Características Técnicas Destacadas

### 1. Soft Delete
Todas las entidades principales implementan eliminación lógica:
```java
@SQLDelete(sql = "UPDATE usuarios SET eliminado = 1 WHERE id = ?")
@Where(clause = "eliminado = 0")
public class Usuario { ... }
```

### 2. Auditoría Automática
Clase base `EntidadAuditable`:
```java
@MappedSuperclass
public abstract class EntidadAuditable {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;
}
```

### 3. Validación Robusta
Validaciones en múltiples capas:
- Frontend: JavaScript con SweetAlert2
- DTO: Bean Validation (@NotNull, @Email, etc.)
- Servicio: Lógica de negocio compleja
- Base de datos: Constraints

### 4. Transacciones
Uso de `@Transactional` para garantizar consistencia:
```java
@Transactional
public Pago registrarPago(PagoDTO dto) {
    // Operaciones atómicas
}
```

### 5. Paginación Optimizada
```java
Pageable pageable = PageRequest.of(page, size, Sort.by("campo").descending());
Page<Entidad> resultado = repository.findAll(pageable);
```

### 6. DTOs para Transferencia
Separación entre entidades de persistencia y objetos de transferencia.

### 7. Integración con APIs Externas
- API RENIEC para validación de DNI
- Manejo de errores HTTP
- Modo demo para desarrollo

---

## Contacto y Soporte

Para consultas, reportes de bugs o sugerencias:

- **Repositorio**: https://github.com/RagnarTB/OdontoApp
- **Issues**: https://github.com/RagnarTB/OdontoApp/issues

---

## Licencia

Este proyecto es de uso privado para la gestión de clínicas odontológicas.

---

## Notas de Versión

**Versión Actual**: 0.0.1-SNAPSHOT

**Características Implementadas**:
- Sistema de login dual con validación estricta
- Gestión completa de usuarios y roles
- Registro de pacientes con auto-registro
- Calendario de citas con validaciones
- Registro de tratamientos con descuento de stock
- Facturación con punto de venta
- Gestión de pagos mixtos
- Control de inventario con trazabilidad
- Notificaciones por email
- Dashboard informativo
- Seguridad a nivel de método

**Próximas Características**:
- Reportes estadísticos avanzados
- Exportación de datos a Excel
- Módulo de odontograma interactivo
- Recordatorios automáticos de citas
- Módulo de cuentas por cobrar
- Integración con pasarelas de pago
- App móvil para pacientes
- Sistema de backup automático

---

**Desarrollado con Spring Boot 3.3.0**
