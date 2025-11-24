#  Documentación Completa - OdontoApp

##  Información General del Proyecto

**Nombre**: OdontoApp  
**Versión**: 0.0.1-SNAPSHOT  
**Descripción**: Sistema integral de gestión odontológica  
**Framework**: Spring Boot 3.3.0  
**Java Version**: 21  
**Base de Datos**: MySQL / PostgreSQL

---

##  Arquitectura del Sistema

### Estructura del Proyecto

```
OdontoApp/
├── src/main/java/com/odontoapp/
│   ├── OdontoappApplication.java       # Clase principal
│   ├── configuracion/                  # Configuraciones del sistema
│   ├── controlador/                    # Controladores MVC
│   ├── dto/                            # Data Transfer Objects
│   ├── entidad/                        # Entidades JPA
│   ├── repositorio/                    # Repositorios JPA
│   ├── seguridad/                      # Configuración de seguridad
│   ├── servicio/                       # Lógica de negocio
│   ├── util/                           # Utilidades
│   └── validacion/                     # Validaciones personalizadas
├── src/main/resources/
│   ├── templates/                      # Vistas Thymeleaf
│   ├── static/                         # Recursos estáticos
│   └── application.properties          # Configuración de la aplicación
└── pom.xml                             # Dependencias Maven
```

---

##  Tecnologías y Dependencias

### Dependencias Principales

- **Spring Boot Starter Data JPA**: Persistencia de datos
- **Spring Boot Starter Security**: Seguridad y autenticación
- **Spring Boot Starter Thymeleaf**: Motor de plantillas
- **Spring Boot Starter Validation**: Validación de datos
- **Spring Boot Starter Mail**: Envío de correos electrónicos
- **Spring Boot Starter Web**: Aplicación web
- **Spring Boot Starter Actuator**: Monitoreo de la aplicación
- **Thymeleaf Extras Spring Security 6**: Integración Thymeleaf-Security
- **MySQL Connector**: Conector para MySQL
- **PostgreSQL**: Conector para PostgreSQL
- **Lombok**: Reducción de código boilerplate

---

##  Modelo de Datos (Entidades)

### 1. **Usuario** (`Usuario.java`)
Representa a los usuarios del sistema (administradores, odontólogos, recepcionistas).

**Campos principales**:
- `id`: Identificador único
- `nombreCompleto`: Nombre completo del usuario
- `email`: Correo electrónico (único)
- `password`: Contraseña encriptada
- `estaActivo`: Estado del usuario
- `intentosFallidos`: Contador de intentos fallidos de login
- `fechaBloqueo`: Fecha de bloqueo por intentos fallidos
- `esSuperAdmin`: Indica si es el super administrador
- `verificationToken`: Token para activación de cuenta
- `passwordResetToken`: Token para recuperación de contraseña
- `passwordResetTokenExpiry`: Expiración del token de recuperación
- `tipoDocumento`: Tipo de documento de identidad
- `numeroDocumento`: Número de documento
- `telefono`: Teléfono de contacto
- `direccion`: Dirección
- `fechaNacimiento`: Fecha de nacimiento
- `fechaContratacion`: Fecha de contratación
- `ultimoAcceso`: Último acceso al sistema
- `debeActualizarPassword`: Flag para forzar cambio de contraseña
- `passwordTemporal`: Contraseña temporal generada
- `fechaVigencia`: Fecha hasta la cual el usuario tiene acceso
- `horarioRegular`: Horario semanal (para odontólogos)
- `excepcionesHorario`: Excepciones al horario regular
- `eliminado`: Soft delete flag
- `fechaEliminacion`: Fecha de eliminación lógica

**Relaciones**:
- `roles`: ManyToMany con `Rol`
- `paciente`: OneToOne con `Paciente`
- `odontologoPreferido`: ManyToOne con `Usuario`

### 2. **Paciente** (`Paciente.java`)
Representa a los pacientes del sistema.

**Campos principales**:
- `id`: Identificador único
- `numeroDocumento`: Número de documento
- `nombreCompleto`: Nombre completo
- `email`: Correo electrónico (único)
- `telefono`: Teléfono
- `fechaNacimiento`: Fecha de nacimiento
- `direccion`: Dirección
- `alergias`: Alergias del paciente
- `antecedentesMedicos`: Antecedentes médicos
- `tratamientosActuales`: Tratamientos actuales
- `eliminado`: Soft delete flag

**Relaciones**:
- `tipoDocumento`: ManyToOne con `TipoDocumento`
- `usuario`: OneToOne con `Usuario`

### 3. **Rol** (`Rol.java`)
Define los roles del sistema.

**Campos principales**:
- `id`: Identificador único
- `nombre`: Nombre del rol (ADMIN, ODONTOLOGO, RECEPCIONISTA, PACIENTE)
- `descripcion`: Descripción del rol
- `estaActivo`: Estado del rol
- `eliminado`: Soft delete flag

**Relaciones**:
- `usuarios`: ManyToMany con `Usuario`
- `permisos`: ManyToMany con `Permiso`

### 4. **Permiso** (`Permiso.java`)
Define los permisos granulares del sistema.

**Campos principales**:
- `id`: Identificador único
- `nombre`: Nombre del permiso
- `descripcion`: Descripción del permiso
- `categoria`: Categoría del permiso

**Relaciones**:
- `roles`: ManyToMany con `Rol`

### 5. **Cita** (`Cita.java`)
Representa las citas odontológicas.

**Campos principales**:
- `id`: Identificador único
- `fechaHora`: Fecha y hora de la cita
- `duracionMinutos`: Duración en minutos
- `motivo`: Motivo de la cita
- `observaciones`: Observaciones adicionales
- `estado`: Estado de la cita (PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA)
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `odontologo`: ManyToOne con `Usuario`

### 6. **TratamientoRealizado** (`TratamientoRealizado.java`)
Representa los tratamientos realizados a los pacientes.

**Campos principales**:
- `id`: Identificador único
- `fecha`: Fecha del tratamiento
- `descripcion`: Descripción del tratamiento
- `observaciones`: Observaciones
- `costo`: Costo del tratamiento
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `odontologo`: ManyToOne con `Usuario`
- `cita`: ManyToOne con `Cita`
- `procedimiento`: ManyToOne con `Procedimiento`

### 7. **TratamientoPlanificado** (`TratamientoPlanificado.java`)
Representa los tratamientos planificados.

**Campos principales**:
- `id`: Identificador único
- `fechaPlanificada`: Fecha planificada
- `descripcion`: Descripción
- `prioridad`: Prioridad del tratamiento
- `estado`: Estado (PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO)
- `costoEstimado`: Costo estimado
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `procedimiento`: ManyToOne con `Procedimiento`

### 8. **Procedimiento** (`Procedimiento.java`)
Catálogo de procedimientos odontológicos.

**Campos principales**:
- `id`: Identificador único
- `codigo`: Código del procedimiento
- `nombre`: Nombre del procedimiento
- `descripcion`: Descripción
- `precio`: Precio del procedimiento
- `duracionEstimada`: Duración estimada en minutos
- `estaActivo`: Estado del procedimiento
- `eliminado`: Soft delete flag

**Relaciones**:
- `categoria`: ManyToOne con `CategoriaProcedimiento`

### 9. **OdontogramaDiente** (`OdontogramaDiente.java`)
Representa el estado de cada diente en el odontograma.

**Campos principales**:
- `id`: Identificador único
- `numeroDiente`: Número del diente (1-32)
- `estado`: Estado del diente (SANO, CARIES, OBTURADO, AUSENTE, etc.)
- `observaciones`: Observaciones
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`

### 10. **OdontogramaHistorial** (`OdontogramaHistorial.java`)
Historial de cambios en el odontograma.

**Campos principales**:
- `id`: Identificador único
- `numeroDiente`: Número del diente
- `estadoAnterior`: Estado anterior
- `estadoNuevo`: Estado nuevo
- `fecha`: Fecha del cambio
- `observaciones`: Observaciones

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `odontologo`: ManyToOne con `Usuario`

### 11. **Insumo** (`Insumo.java`)
Catálogo de insumos odontológicos.

**Campos principales**:
- `id`: Identificador único
- `codigo`: Código del insumo
- `nombre`: Nombre del insumo
- `descripcion`: Descripción
- `stockMinimo`: Stock mínimo
- `stockActual`: Stock actual
- `precioUnitario`: Precio unitario
- `estaActivo`: Estado del insumo
- `eliminado`: Soft delete flag

**Relaciones**:
- `categoria`: ManyToOne con `CategoriaInsumo`
- `unidadMedida`: ManyToOne con `UnidadMedida`

### 12. **MovimientoInventario** (`MovimientoInventario.java`)
Movimientos de inventario de insumos.

**Campos principales**:
- `id`: Identificador único
- `fecha`: Fecha del movimiento
- `cantidad`: Cantidad
- `tipoMovimiento`: Tipo (ENTRADA, SALIDA)
- `motivo`: Motivo del movimiento
- `observaciones`: Observaciones
- `eliminado`: Soft delete flag

**Relaciones**:
- `insumo`: ManyToOne con `Insumo`
- `usuario`: ManyToOne con `Usuario`

### 13. **Comprobante** (`Comprobante.java`)
Comprobantes de pago.

**Campos principales**:
- `id`: Identificador único
- `numeroComprobante`: Número del comprobante
- `fecha`: Fecha de emisión
- `subtotal`: Subtotal
- `igv`: IGV
- `total`: Total
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `detalles`: OneToMany con `DetalleComprobante`

### 14. **Pago** (`Pago.java`)
Pagos realizados.

**Campos principales**:
- `id`: Identificador único
- `fecha`: Fecha del pago
- `monto`: Monto
- `metodoPago`: Método de pago
- `estadoPago`: Estado del pago
- `observaciones`: Observaciones
- `eliminado`: Soft delete flag

**Relaciones**:
- `comprobante`: ManyToOne con `Comprobante`

### 15. **ArchivoAdjunto** (`ArchivoAdjunto.java`)
Archivos adjuntos (radiografías, documentos, etc.).

**Campos principales**:
- `id`: Identificador único
- `nombreArchivo`: Nombre del archivo
- `rutaArchivo`: Ruta del archivo
- `tipoArchivo`: Tipo de archivo
- `tamanio`: Tamaño en bytes
- `descripcion`: Descripción
- `fechaSubida`: Fecha de subida
- `eliminado`: Soft delete flag

**Relaciones**:
- `paciente`: ManyToOne con `Paciente`
- `tratamiento`: ManyToOne con `TratamientoRealizado`

---

##  Servicios (Lógica de Negocio)

### 1. **UsuarioService** / **UsuarioServiceImpl**
Gestión de usuarios del sistema.

**Métodos principales**:
- `guardarUsuario(UsuarioDTO)`: Crear/actualizar usuario
- `listarTodosLosUsuarios(keyword, pageable)`: Listar usuarios con paginación
- `buscarPorId(id)`: Buscar usuario por ID
- `eliminarUsuario(id)`: Eliminación lógica de usuario
- `restablecerUsuario(id)`: Restaurar usuario eliminado (genera contraseña temporal)
- `cambiarEstadoUsuario(id)`: Activar/desactivar usuario
- `procesarLoginFallido(email)`: Procesar intento fallido de login
- `resetearIntentosFallidos(email)`: Resetear intentos fallidos

**Lógica especial**:
- Al crear un usuario, se genera una contraseña temporal aleatoria
- Se envía email con la contraseña temporal
- Al restaurar un usuario eliminado, se genera nueva contraseña temporal
- Protección del super-administrador contra eliminación/desactivación
- Validación de unicidad de email, documento y teléfono
- Gestión de horarios para odontólogos
- Validación de fecha de vigencia según roles

### 2. **PacienteService** / **PacienteServiceImpl**
Gestión de pacientes.

**Métodos principales**:
- `guardarPaciente(PacienteDTO)`: Crear/actualizar paciente
- `listarTodosLosPacientes(keyword, pageable)`: Listar pacientes
- `buscarPorId(id)`: Buscar paciente por ID
- `eliminarPaciente(id)`: Eliminación lógica de paciente
- `restablecerPaciente(id)`: Restaurar paciente eliminado
- `buscarPorDocumento(numeroDocumento, tipoDocumentoId)`: Buscar por documento
- `crearUsuarioTemporalParaRegistro(email)`: Crear usuario temporal para auto-registro
- `completarRegistroPaciente(registroDTO, token, password)`: Completar auto-registro

**Lógica especial**:
- Al crear un paciente desde el admin, se crea un usuario asociado inactivo
- Se envía email de activación al paciente
- Validación de unicidad de documento y email
- Al eliminar un paciente, se elimina/desactiva el usuario asociado
- **PENDIENTE**: Al restaurar un paciente, generar contraseña temporal (igual que usuarios)

### 3. **CitaService** / **CitaServiceImpl**
Gestión de citas odontológicas.

**Métodos principales**:
- `guardarCita(CitaDTO)`: Crear/actualizar cita
- `listarCitas(filtros, pageable)`: Listar citas con filtros
- `buscarPorId(id)`: Buscar cita por ID
- `eliminarCita(id)`: Eliminar cita
- `cambiarEstadoCita(id, estado)`: Cambiar estado de cita
- `obtenerCitasDisponibles(odontologoId, fecha)`: Obtener horarios disponibles
- `validarDisponibilidad(citaDTO)`: Validar disponibilidad de horario

**Lógica especial**:
- Validación de conflictos de horarios
- Validación de horarios laborales del odontólogo
- Notificaciones por email de citas
- Gestión de estados (PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA)

### 4. **TratamientoRealizadoService**
Gestión de tratamientos realizados.

**Métodos principales**:
- `guardarTratamiento(TratamientoDTO)`: Registrar tratamiento
- `listarTratamientos(pacienteId, filtros)`: Listar tratamientos
- `buscarPorId(id)`: Buscar tratamiento por ID
- `eliminarTratamiento(id)`: Eliminar tratamiento
- `generarHistorialPaciente(pacienteId)`: Generar historial completo

### 5. **OdontogramaDienteService**
Gestión del odontograma.

**Métodos principales**:
- `obtenerOdontograma(pacienteId)`: Obtener odontograma del paciente
- `actualizarDiente(odontogramaDTO)`: Actualizar estado de un diente
- `obtenerHistorial(pacienteId)`: Obtener historial de cambios

**Lógica especial**:
- Registro automático de cambios en el historial
- Validación de números de diente (1-32)

### 6. **InsumoService** / **InventarioService**
Gestión de insumos e inventario.

**Métodos principales**:
- `guardarInsumo(InsumoDTO)`: Crear/actualizar insumo
- `listarInsumos(filtros, pageable)`: Listar insumos
- `registrarMovimiento(MovimientoDTO)`: Registrar movimiento de inventario
- `obtenerInsumosConStockBajo()`: Obtener insumos con stock bajo
- `generarReporteInventario()`: Generar reporte de inventario

### 7. **FacturacionService**
Gestión de facturación y pagos.

**Métodos principales**:
- `generarComprobante(comprobanteDTO)`: Generar comprobante
- `registrarPago(pagoDTO)`: Registrar pago
- `obtenerComprobantes(filtros, pageable)`: Listar comprobantes
- `anularComprobante(id)`: Anular comprobante

### 8. **EmailService**
Servicio de envío de correos electrónicos.

**Métodos principales**:
- `enviarPasswordTemporal(email, nombre, password)`: Enviar contraseña temporal
- `enviarEmailActivacionAdmin(email, nombre, token)`: Enviar email de activación
- `enviarEmailRecuperacion(email, nombre, token)`: Enviar email de recuperación
- `enviarConfirmacionCita(citaDTO)`: Enviar confirmación de cita
- `enviarRecordatorioCita(citaDTO)`: Enviar recordatorio de cita

### 9. **RolService** / **RolServiceImpl**
Gestión de roles y permisos.

**Métodos principales**:
- `guardarRol(RolDTO)`: Crear/actualizar rol
- `listarRoles()`: Listar todos los roles
- `asignarPermisos(rolId, permisosIds)`: Asignar permisos a un rol
- `obtenerPermisosPorRol(rolId)`: Obtener permisos de un rol

### 10. **DashboardService** / **PacienteDashboardService**
Servicios para dashboards.

**Métodos principales**:
- `obtenerEstadisticasGenerales()`: Estadísticas generales del sistema
- `obtenerCitasProximas()`: Citas próximas
- `obtenerTratamientosPendientes()`: Tratamientos pendientes
- `obtenerInsumosStockBajo()`: Insumos con stock bajo
- `obtenerEstadisticasPaciente(pacienteId)`: Estadísticas del paciente

### 11. **ReniecService**
Integración con API de RENIEC para validación de DNI.

**Métodos principales**:
- `consultarDNI(dni)`: Consultar datos de DNI en RENIEC
- `validarDNI(dni)`: Validar formato de DNI

### 12. **CascadaService**
Servicio para gestión de eliminaciones en cascada.

**Métodos principales**:
- `eliminarUsuarioConCascada(usuarioId)`: Eliminar usuario y datos relacionados
- `eliminarPacienteConCascada(pacienteId)`: Eliminar paciente y datos relacionados

### 13. **SessionInvalidationService**
Gestión de sesiones de usuario.

**Métodos principales**:
- `invalidarSesionesUsuario(usuarioId)`: Invalidar todas las sesiones de un usuario
- `invalidarSesionActual()`: Invalidar sesión actual

---

##  Controladores

### 1. **LoginController**
Gestión de autenticación.

**Endpoints**:
- `GET /login`: Mostrar formulario de login
- `POST /login`: Procesar login
- `GET /logout`: Cerrar sesión

### 2. **RegistroController**
Auto-registro de pacientes.

**Endpoints**:
- `GET /registro`: Mostrar formulario de registro
- `POST /registro/email`: Validar email y enviar link de activación
- `GET /registro/completar`: Mostrar formulario de datos completos
- `POST /registro/completar`: Completar registro

### 3. **ActivacionController**
Activación de cuentas.

**Endpoints**:
- `GET /activar`: Activar cuenta con token
- `POST /activar/establecer-password`: Establecer contraseña

### 4. **RecuperarPasswordController**
Recuperación de contraseña.

**Endpoints**:
- `GET /recuperar-password`: Mostrar formulario de recuperación
- `POST /recuperar-password`: Enviar email de recuperación
- `GET /reset-password`: Mostrar formulario de nueva contraseña
- `POST /reset-password`: Establecer nueva contraseña

### 5. **CambioPasswordController**
Cambio de contraseña.

**Endpoints**:
- `GET /cambiar-password`: Mostrar formulario de cambio
- `POST /cambiar-password`: Cambiar contraseña
- `GET /forzar-cambio-password`: Forzar cambio de contraseña temporal

### 6. **UsuarioController**
CRUD de usuarios.

**Endpoints**:
- `GET /usuarios`: Listar usuarios
- `GET /usuarios/nuevo`: Formulario de nuevo usuario
- `POST /usuarios/guardar`: Guardar usuario
- `GET /usuarios/editar/{id}`: Formulario de edición
- `POST /usuarios/eliminar/{id}`: Eliminar usuario
- `POST /usuarios/restablecer/{id}`: Restaurar usuario eliminado
- `POST /usuarios/cambiar-estado/{id}`: Cambiar estado de usuario

### 7. **PacienteController**
CRUD de pacientes.

**Endpoints**:
- `GET /pacientes`: Listar pacientes
- `GET /pacientes/nuevo`: Formulario de nuevo paciente
- `POST /pacientes/guardar`: Guardar paciente
- `GET /pacientes/editar/{id}`: Formulario de edición
- `POST /pacientes/eliminar/{id}`: Eliminar paciente
- `POST /pacientes/restablecer/{id}`: Restaurar paciente eliminado
- `GET /pacientes/detalle/{id}`: Ver detalle de paciente

### 8. **CitaController**
CRUD de citas.

**Endpoints**:
- `GET /citas`: Listar citas
- `GET /citas/nueva`: Formulario de nueva cita
- `POST /citas/guardar`: Guardar cita
- `GET /citas/editar/{id}`: Formulario de edición
- `POST /citas/eliminar/{id}`: Eliminar cita
- `POST /citas/cambiar-estado/{id}`: Cambiar estado de cita
- `GET /citas/disponibilidad`: Obtener horarios disponibles

### 9. **TratamientoController**
CRUD de tratamientos.

**Endpoints**:
- `GET /tratamientos`: Listar tratamientos
- `GET /tratamientos/nuevo`: Formulario de nuevo tratamiento
- `POST /tratamientos/guardar`: Guardar tratamiento
- `GET /tratamientos/paciente/{pacienteId}`: Tratamientos de un paciente

### 10. **OdontogramaViewController** / **OdontogramaRestController**
Gestión del odontograma.

**Endpoints**:
- `GET /odontograma/{pacienteId}`: Ver odontograma
- `POST /api/odontograma/actualizar`: Actualizar diente (REST)
- `GET /api/odontograma/{pacienteId}`: Obtener odontograma (REST)
- `GET /api/odontograma/historial/{pacienteId}`: Obtener historial (REST)

### 11. **ProcedimientoController** / **ProcedimientoRestController**
Gestión de procedimientos.

**Endpoints**:
- `GET /procedimientos`: Listar procedimientos
- `GET /procedimientos/nuevo`: Formulario de nuevo procedimiento
- `POST /procedimientos/guardar`: Guardar procedimiento
- `GET /api/procedimientos`: Listar procedimientos (REST)
- `GET /api/procedimientos/{id}`: Obtener procedimiento (REST)

### 12. **InsumoController** / **InventarioController**
Gestión de insumos e inventario.

**Endpoints**:
- `GET /insumos`: Listar insumos
- `GET /insumos/nuevo`: Formulario de nuevo insumo
- `POST /insumos/guardar`: Guardar insumo
- `GET /inventario`: Ver inventario
- `POST /inventario/movimiento`: Registrar movimiento

### 13. **FacturacionController**
Gestión de facturación.

**Endpoints**:
- `GET /facturacion`: Listar comprobantes
- `GET /facturacion/nuevo`: Formulario de nuevo comprobante
- `POST /facturacion/guardar`: Guardar comprobante
- `POST /facturacion/pago`: Registrar pago

### 14. **DashboardController**
Dashboard principal.

**Endpoints**:
- `GET /`: Dashboard principal
- `GET /dashboard`: Dashboard principal (alias)
- `GET /dashboard/estadisticas`: Obtener estadísticas

### 15. **PacientePerfilController** / **PacienteCitaController**
Portal del paciente.

**Endpoints**:
- `GET /paciente/perfil`: Ver perfil del paciente
- `GET /paciente/citas`: Ver citas del paciente
- `POST /paciente/citas/solicitar`: Solicitar nueva cita
- `GET /paciente/tratamientos`: Ver tratamientos del paciente
- `GET /paciente/odontograma`: Ver odontograma del paciente

### 16. **RolController** / **PermisosRestController**
Gestión de roles y permisos.

**Endpoints**:
- `GET /roles`: Listar roles
- `GET /roles/nuevo`: Formulario de nuevo rol
- `POST /roles/guardar`: Guardar rol
- `GET /api/permisos`: Listar permisos (REST)
- `POST /api/roles/{id}/permisos`: Asignar permisos (REST)

### 17. **RolSelectorController**
Selector de rol al iniciar sesión.

**Endpoints**:
- `GET /seleccionar-rol`: Mostrar selector de rol
- `POST /seleccionar-rol`: Seleccionar rol

### 18. **AdministracionController**
Panel de administración.

**Endpoints**:
- `GET /admin`: Panel de administración
- `GET /admin/configuracion`: Configuración del sistema

### 19. **ReniecApiController**
API de integración con RENIEC.

**Endpoints**:
- `GET /api/reniec/consultar/{dni}`: Consultar DNI en RENIEC

### 20. **ArchivoAdjuntoController**
Gestión de archivos adjuntos.

**Endpoints**:
- `POST /archivos/subir`: Subir archivo
- `GET /archivos/descargar/{id}`: Descargar archivo
- `POST /archivos/eliminar/{id}`: Eliminar archivo

### 21. **ErrorController**
Manejo de errores.

**Endpoints**:
- `GET /error`: Página de error genérica
- `GET /403`: Acceso denegado
- `GET /404`: Página no encontrada

---

##  Seguridad

### Configuración de Seguridad (`SecurityConfig`)

**Autenticación**:
- Basada en Spring Security
- Autenticación por email y contraseña
- Encriptación de contraseñas con BCrypt

**Autorización**:
- Control de acceso basado en roles
- Permisos granulares por funcionalidad
- Protección de endpoints por rol

**Roles del Sistema**:
1. **ADMIN**: Acceso completo al sistema
2. **ODONTOLOGO**: Gestión de citas, tratamientos, odontogramas
3. **RECEPCIONISTA**: Gestión de citas, pacientes
4. **PACIENTE**: Acceso limitado a su información personal

**Funcionalidades de Seguridad**:
- Bloqueo de cuenta tras 3 intentos fallidos
- Tokens de activación de cuenta
- Tokens de recuperación de contraseña con expiración
- Contraseñas temporales que obligan a cambio
- Soft delete para preservar integridad de datos
- Protección contra eliminación del super-administrador
- Validación de sesiones activas
- Invalidación de sesiones al cambiar estado de usuario

### CustomUserDetailsService
Servicio personalizado para cargar usuarios desde la base de datos.

**Funcionalidades**:
- Carga de usuario por email
- Validación de estado activo
- Validación de bloqueo por intentos fallidos
- Validación de fecha de vigencia
- Carga de roles y permisos

---

##  Sistema de Notificaciones

### EmailService
Servicio de envío de correos electrónicos.

**Tipos de Emails**:
1. **Contraseña Temporal**: Al crear/restaurar usuario
2. **Activación de Cuenta**: Al crear paciente desde admin
3. **Recuperación de Contraseña**: Al solicitar recuperación
4. **Confirmación de Cita**: Al crear/modificar cita
5. **Recordatorio de Cita**: Recordatorio automático

**Configuración**:
- SMTP configurado en `application.properties`
- Plantillas HTML para emails
- Envío asíncrono para no bloquear operaciones

---

##  Repositorios (Acceso a Datos)

Todos los repositorios extienden `JpaRepository` y utilizan Spring Data JPA.

**Repositorios principales**:
- `UsuarioRepository`: Consultas personalizadas de usuarios
- `PacienteRepository`: Consultas personalizadas de pacientes
- `CitaRepository`: Consultas de citas con filtros
- `TratamientoRealizadoRepository`: Consultas de tratamientos
- `OdontogramaDienteRepository`: Consultas de odontograma
- `InsumoRepository`: Consultas de insumos
- `MovimientoInventarioRepository`: Consultas de movimientos
- `ComprobanteRepository`: Consultas de comprobantes
- `RolRepository`: Consultas de roles
- `PermisoRepository`: Consultas de permisos

**Características**:
- Métodos de consulta derivados de nombres
- Consultas personalizadas con `@Query`
- Soporte para paginación y ordenamiento
- Soft delete con `@Where(clause = "eliminado = false")`

---

##  Utilidades

### 1. **PasswordUtil**
Utilidad para generación y validación de contraseñas.

**Métodos**:
- `generarPasswordAleatoria()`: Genera contraseña aleatoria de 12 caracteres
- `validarPasswordRobusta(password)`: Valida requisitos de seguridad

**Requisitos de contraseña**:
- Mínimo 8 caracteres
- Al menos 1 mayúscula
- Al menos 1 minúscula
- Al menos 1 número
- Al menos 1 carácter especial (!@#$%&*)

---

##  Validaciones

### Validaciones Personalizadas

**Anotaciones de validación**:
- `@ValidEmail`: Validación de formato de email
- `@ValidDNI`: Validación de DNI peruano
- `@ValidPhone`: Validación de teléfono
- `@FechaFutura`: Validación de fecha futura
- `@FechaPasada`: Validación de fecha pasada

**Validaciones en DTOs**:
- Validación de campos requeridos
- Validación de longitud de campos
- Validación de rangos numéricos
- Validación de formatos

---

##  Flujos Principales del Sistema

### 1. **Flujo de Creación de Usuario (Admin)**
1. Admin accede a `/usuarios/nuevo`
2. Completa formulario con datos del usuario
3. Sistema valida unicidad de email, documento, teléfono
4. Sistema genera contraseña temporal aleatoria
5. Sistema guarda usuario con `debeActualizarPassword = true`
6. Sistema envía email con contraseña temporal
7. Usuario recibe email y puede hacer login
8. Al hacer login, se fuerza cambio de contraseña

### 2. **Flujo de Restauración de Usuario**
1. Admin accede a lista de usuarios eliminados
2. Selecciona usuario a restaurar
3. Sistema restaura usuario
4. Sistema genera nueva contraseña temporal
5. Sistema envía email con contraseña temporal
6. Usuario debe cambiar contraseña al hacer login

### 3. **Flujo de Auto-Registro de Paciente**
1. Paciente accede a `/registro`
2. Ingresa su email
3. Sistema valida email y crea usuario temporal inactivo
4. Sistema envía email con link de activación
5. Paciente hace clic en link
6. Sistema muestra formulario de datos completos
7. Paciente completa datos y establece contraseña
8. Sistema activa cuenta y crea registro de paciente
9. Paciente puede hacer login

### 4. **Flujo de Creación de Paciente (Admin)**
1. Admin accede a `/pacientes/nuevo`
2. Completa formulario con datos del paciente
3. Sistema valida unicidad de documento y email
4. Sistema crea usuario asociado inactivo
5. Sistema genera token de activación
6. Sistema envía email de activación al paciente
7. Paciente activa cuenta y establece contraseña

### 5. **Flujo de Restauración de Paciente**
**ACTUAL** (sin contraseña temporal):
1. Admin restaura paciente eliminado
2. Sistema restaura paciente y usuario asociado
3. Usuario puede hacer login con contraseña anterior

**PROPUESTO** (con contraseña temporal):
1. Admin restaura paciente eliminado
2. Sistema restaura paciente y usuario asociado
3. Sistema genera nueva contraseña temporal
4. Sistema envía email con contraseña temporal
5. Usuario debe cambiar contraseña al hacer login

### 6. **Flujo de Recuperación de Contraseña**
1. Usuario accede a `/recuperar-password`
2. Ingresa su email
3. Sistema genera token de recuperación con expiración
4. Sistema envía email con link de recuperación
5. Usuario hace clic en link
6. Sistema valida token y expiración
7. Usuario establece nueva contraseña
8. Sistema actualiza contraseña y elimina token

### 7. **Flujo de Creación de Cita**
1. Usuario accede a `/citas/nueva`
2. Selecciona paciente, odontólogo, fecha y hora
3. Sistema valida disponibilidad del odontólogo
4. Sistema valida horario laboral
5. Sistema valida conflictos de horarios
6. Sistema guarda cita
7. Sistema envía email de confirmación al paciente

### 8. **Flujo de Registro de Tratamiento**
1. Odontólogo accede a `/tratamientos/nuevo`
2. Selecciona paciente y procedimiento
3. Completa datos del tratamiento
4. Sistema registra tratamiento
5. Sistema actualiza odontograma si aplica
6. Sistema registra en historial

---

##  Características Especiales

### 1. **Soft Delete**
- Todas las entidades principales usan eliminación lógica
- Campo `eliminado` en cada entidad
- Anotación `@Where(clause = "eliminado = false")` en entidades
- Métodos especiales en repositorios para ignorar soft delete

### 2. **Auditoría**
- Clase base `EntidadAuditable` con campos de auditoría
- `fechaCreacion`: Fecha de creación del registro
- `fechaModificacion`: Fecha de última modificación
- `usuarioCreacion`: Usuario que creó el registro
- `usuarioModificacion`: Usuario que modificó el registro

### 3. **Gestión de Horarios (Odontólogos)**
- Horario regular semanal (Map<DayOfWeek, String>)
- Excepciones de horario para fechas específicas
- Validación de disponibilidad al crear citas
- Formato de horarios: "09:00-13:00,15:00-19:00"

### 4. **Sistema de Permisos Granulares**
- Permisos específicos por funcionalidad
- Asignación de permisos a roles
- Validación de permisos en controladores
- Categorización de permisos

### 5. **Integración con RENIEC**
- Consulta de datos de DNI
- Validación de identidad
- Auto-completado de datos personales

### 6. **Gestión de Inventario**
- Control de stock de insumos
- Alertas de stock mínimo
- Registro de movimientos (entradas/salidas)
- Relación de insumos con procedimientos

### 7. **Facturación**
- Generación de comprobantes
- Registro de pagos
- Métodos de pago múltiples
- Estados de pago

---

##  Configuración y Despliegue

### Requisitos del Sistema
- Java 21
- Maven 3.6+
- MySQL 8.0+ o PostgreSQL 12+
- SMTP Server (para envío de emails)

### Configuración (`application.properties`)

```properties
# Base de Datos
spring.datasource.url=jdbc:mysql://localhost:3306/odontoapp
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update

# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Thymeleaf
spring.thymeleaf.cache=false

# Actuator
management.endpoints.web.exposure.include=health,info

# Logging
logging.level.com.odontoapp=DEBUG
```

### Comandos de Ejecución

```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run

# Generar JAR
mvn package

# Ejecutar JAR
java -jar target/odontoapp-0.0.1-SNAPSHOT.jar
```

### Docker

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/odontoapp-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: odontoapp
      MYSQL_ROOT_PASSWORD: password
    ports:
      - "3306:3306"
  
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
```

---

##  Problemas Conocidos y Pendientes

### 1. **Contraseña Temporal en Restauración de Pacientes** ⚠️
**Problema**: Al restaurar un paciente eliminado, no se genera contraseña temporal como sí ocurre con los usuarios del sistema.

**Solución**: Modificar el método `restablecerPaciente` en `PacienteServiceImpl` para:
- Generar contraseña temporal con `PasswordUtil.generarPasswordAleatoria()`
- Establecer `debeActualizarPassword = true`
- Resetear intentos fallidos y bloqueo
- Enviar email con contraseña temporal

**Archivo de referencia**: `metodo_restablecer_paciente_modificado.java`

### 2. **Validaciones Pendientes**
- Validación de conflictos de citas más robusta
- Validación de stock antes de usar insumos
- Validación de permisos en todos los endpoints

### 3. **Mejoras Sugeridas**
- Implementar caché para consultas frecuentes
- Agregar logs de auditoría más detallados
- Implementar notificaciones push
- Agregar reportes en PDF
- Implementar backup automático de base de datos

---

##  Roles y Usuarios por Defecto

### Super Administrador
- **Email**: admin@odontoapp.com
- **Rol**: ADMIN
- **Permisos**: Todos

### Roles Disponibles
1. **ADMIN**: Administrador del sistema
2. **ODONTOLOGO**: Odontólogo
3. **RECEPCIONISTA**: Recepcionista
4. **PACIENTE**: Paciente

---

##  Soporte y Contacto

Para soporte técnico o consultas sobre el sistema, contactar al equipo de desarrollo.

---

**Última actualización**: 2025-11-24  
**Versión del documento**: 1.0
