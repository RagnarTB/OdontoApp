package com.odontoapp.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime; // NUEVO import
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // NUEVO import

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page; // NUEVO import
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.HorarioExcepcion; // NUEVO import
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Rol; // NUEVO import
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.entidad.Usuario; // NUEVO import
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.util.PasswordUtil;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import jakarta.transaction.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private final EmailService emailService;
    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final CitaRepository citaRepository;

    // Inyecta las dependencias necesarias
    public UsuarioServiceImpl(EmailService emailService, PacienteRepository pacienteRepository,
            PasswordEncoder passwordEncoder, RolRepository rolRepository,
            TipoDocumentoRepository tipoDocumentoRepository, UsuarioRepository usuarioRepository,
            CitaRepository citaRepository) {
        this.emailService = emailService;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
    }

    @Override
    @Transactional
    public void guardarUsuario(UsuarioDTO usuarioDTO) {

        Usuario usuario;
        boolean esNuevo = usuarioDTO.getId() == null;
        String emailNuevo = usuarioDTO.getEmail();
        String emailOriginal = null;
        boolean enviarEmailTemporal = false;
        // --- 1. VALIDACIÓN PREVIA DE DOCUMENTO ---
        validarUnicidadDocumentoUsuario(
                usuarioDTO.getNumeroDocumento(),
                usuarioDTO.getTipoDocumentoId(),
                usuarioDTO.getId());

        // --- 2. VALIDACIÓN PREVIA DE TELÉFONO ---
        validarUnicidadTelefono(
                usuarioDTO.getTelefono(),
                usuarioDTO.getId());

        // --- 3. VALIDACIÓN PREVIA DE EMAIL ---
        validarUnicidadEmail(emailNuevo, usuarioDTO.getId(), esNuevo);

        if (esNuevo) {
            usuario = new Usuario();
            // Aseguramos inicialización de colecciones para evitar NullPointerException
            usuario.setHorarioRegular(new java.util.HashMap<>()); // Usar HashMap aquí, JPA lo mapeará bien
            usuario.setExcepcionesHorario(new java.util.ArrayList<>());
            usuario.setFechaContratacion(java.time.LocalDate.now()); // Establecer fecha contratación para nuevos

        } else {
            usuario = usuarioRepository.findById(usuarioDTO.getId())
                    .orElseThrow(
                            () -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioDTO.getId()));

            // Proteger al super-administrador de ediciones no permitidas
            if (usuario.isEsSuperAdmin()) {
                throw new UnsupportedOperationException("No se puede modificar al super-administrador del sistema.");
            }

            emailOriginal = usuario.getEmail();

            if (!emailNuevo.equals(emailOriginal)) {
                // Validar si se intenta cambiar email del admin principal (si aplica)
                if ("admin@odontoapp.com".equals(emailOriginal)) {
                    throw new IllegalArgumentException("No se puede cambiar el email del administrador principal.");
                }

                // --- LÓGICA DE CAMBIO DE EMAIL CON RESETEO DE CONTRASEÑA ---
                // Si el email cambió, generar nueva contraseña temporal por seguridad
                String passwordTemporal = PasswordUtil.generarPasswordAleatoria();
                usuario.setPasswordTemporal(passwordTemporal);
                usuario.setPassword(passwordEncoder.encode(passwordTemporal));
                usuario.setDebeActualizarPassword(true); // Forzar cambio al siguiente login
                enviarEmailTemporal = true; // Activar envío de email a la nueva dirección

                System.out.println(">>> Cambio de email detectado para usuario " + emailOriginal +
                        " -> " + emailNuevo + ". Se generará nueva contraseña temporal.");
            }
            // Aseguramos inicialización si las colecciones fueran null (aunque no debería
            // pasar con JPA)
            if (usuario.getHorarioRegular() == null) {
                usuario.setHorarioRegular(new java.util.HashMap<>());
            }
            if (usuario.getExcepcionesHorario() == null) {
                usuario.setExcepcionesHorario(new java.util.ArrayList<>());
            }
        }

        // --- 3. OBTENER TIPO DE DOCUMENTO ---
        TipoDocumento tipoDocumento = null;
        if (usuarioDTO.getTipoDocumentoId() != null) {
            tipoDocumento = tipoDocumentoRepository.findById(usuarioDTO.getTipoDocumentoId())
                    .orElseThrow(() -> new IllegalStateException("Tipo de documento no encontrado."));
        }

        // --- 4. ACTUALIZAR CAMPOS BÁSICOS ---
        usuario.setNombreCompleto(usuarioDTO.getNombreCompleto());
        usuario.setEmail(emailNuevo);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNumeroDocumento(usuarioDTO.getNumeroDocumento());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setFechaNacimiento(usuarioDTO.getFechaNacimiento());
        usuario.setDireccion(usuarioDTO.getDireccion());

        // Validar fecha de contratación no futura
        if (usuarioDTO.getFechaContratacion() != null) {
            if (usuarioDTO.getFechaContratacion().isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de contratación no puede ser futura.");
            }
            if (esNuevo) {
                usuario.setFechaContratacion(usuarioDTO.getFechaContratacion());
            }
        } else if (esNuevo) {
            usuario.setFechaContratacion(java.time.LocalDate.now());
        }

        // Validar edad mínima (18 años) para empleados
        if (usuarioDTO.getFechaNacimiento() != null) {
            java.time.Period edad = java.time.Period.between(usuarioDTO.getFechaNacimiento(),
                    java.time.LocalDate.now());
            if (edad.getYears() < 18) {
                throw new IllegalArgumentException("El trabajador debe ser mayor de 18 años.");
            }
        }

        // --- ContraseÃ±a SOLO si es nuevo ---
        if (esNuevo) {
            String passwordTemporal = PasswordUtil.generarPasswordAleatoria();
            usuario.setPasswordTemporal(passwordTemporal);
            usuario.setPassword(passwordEncoder.encode(passwordTemporal));
            usuario.setDebeActualizarPassword(true); // Forzar cambio al primer login
            usuario.setEstaActivo(true); // Activar directamente al usuario creado por admin
            enviarEmailTemporal = true;
        }

        // --- Roles y protecciÃ³n del admin principal ---
        boolean esAdminPrincipal = "admin@odontoapp.com".equals(usuario.getEmail());
        Rol rolAdmin = rolRepository.findByNombre("ADMIN").orElse(null);
        boolean tieneRolAdminOriginalmente = !esNuevo
                && usuario.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getNombre()));
        boolean seIntentaQuitarRolAdmin = tieneRolAdminOriginalmente
                && (rolAdmin == null || !usuarioDTO.getRoles().contains(rolAdmin.getId()));

        if (esAdminPrincipal && seIntentaQuitarRolAdmin) {
            throw new IllegalArgumentException("No se puede quitar el rol ADMIN al administrador principal.");
        }

        // Obtener los roles seleccionados desde la base de datos
        List<Rol> rolesSeleccionados = rolRepository.findAllById(usuarioDTO.getRoles());

        // ⚠️ VALIDACIÓN DE EDAD PARA ROLES ADMINISTRATIVOS
        validarEdadParaRolesAdministrativos(usuarioDTO.getFechaNacimiento(), rolesSeleccionados);

        usuario.setRoles(new HashSet<>(rolesSeleccionados));

        boolean tieneRolPaciente = rolesSeleccionados.stream()
                .anyMatch(r -> "PACIENTE".equals(r.getNombre()));
        if (tieneRolPaciente) {
            // ⚠️ LÓGICA COMPLETA DE CREACIÓN/VINCULACIÓN/RESTAURACIÓN DE PACIENTE
            Paciente paciente = null;

            // PASO 1: Buscar paciente asociado al usuario (puede estar eliminado)
            if (!esNuevo && usuario.getId() != null) {
                Optional<Paciente> pacienteDelUsuario = pacienteRepository.findByUsuario(usuario);
                if (pacienteDelUsuario.isPresent()) {
                    paciente = pacienteDelUsuario.get();

                    // Si estaba eliminado, restaurarlo
                    if (paciente.isEliminado()) {
                        paciente.setEliminado(false);
                        System.out.println("✅ Restaurando paciente ID " + paciente.getId() +
                                " (estaba eliminado) para usuario ID " + usuario.getId());
                    } else {
                        System.out.println("✅ Actualizando paciente existente ID " + paciente.getId() +
                                " para usuario ID " + usuario.getId());
                    }
                }
            }

            // PASO 2: Si no tiene paciente asociado, buscar por documento (puede ser standalone)
            if (paciente == null) {
                Optional<Paciente> pacienteExistente = pacienteRepository
                        .findByNumeroTipoDocumentoIgnorandoSoftDelete(
                                usuario.getNumeroDocumento(),
                                usuario.getTipoDocumento().getId());

                if (pacienteExistente.isPresent()) {
                    // Ya existe un paciente con este documento, vincularlo
                    paciente = pacienteExistente.get();
                    paciente.setUsuario(usuario);

                    // Restaurar si estaba eliminado
                    if (paciente.isEliminado()) {
                        paciente.setEliminado(false);
                        System.out.println("✅ Vinculando y restaurando paciente standalone ID " + paciente.getId() +
                                " para usuario ID " + usuario.getId());
                    } else {
                        System.out.println("✅ Vinculando paciente standalone ID " + paciente.getId() +
                                " para usuario ID " + usuario.getId());
                    }
                }
            }

            // PASO 3: Si aún no hay paciente, crear uno nuevo
            if (paciente == null) {
                paciente = new Paciente();
                paciente.setUsuario(usuario);
                System.out.println("✅ Creando nuevo registro de paciente para usuario ID " + usuario.getId());
            }

            // PASO 4: Siempre actualizar datos del paciente con los del usuario
            paciente.setTipoDocumento(usuario.getTipoDocumento());
            paciente.setNumeroDocumento(usuario.getNumeroDocumento());
            paciente.setNombreCompleto(usuario.getNombreCompleto());
            paciente.setEmail(usuario.getEmail());
            paciente.setTelefono(usuario.getTelefono());
            paciente.setFechaNacimiento(usuario.getFechaNacimiento());
            paciente.setDireccion(usuario.getDireccion());

            // PASO 5: Actualizar campos específicos de paciente (opcionales)
            paciente.setAlergias(usuarioDTO.getAlergias());
            paciente.setAntecedentesMedicos(usuarioDTO.getAntecedentesMedicos());
            paciente.setTratamientosActuales(usuarioDTO.getTratamientosActuales());

            pacienteRepository.save(paciente);
            usuario.setPaciente(paciente);
        }

        // --- VALIDAR Y ESTABLECER FECHA DE VIGENCIA ---
        boolean tieneRolAdmin = rolesSeleccionados.stream().anyMatch(r -> "ADMIN".equals(r.getNombre()));
        boolean soloTieneRolPaciente = rolesSeleccionados.size() == 1 && tieneRolPaciente;

        if (tieneRolAdmin || soloTieneRolPaciente) {
            // Si tiene rol ADMIN o SOLO tiene rol PACIENTE, la fecha de vigencia no es requerida
            // Los pacientes no necesitan fecha de vigencia (no son personal)
            usuario.setFechaVigencia(usuarioDTO.getFechaVigencia()); // Puede ser null
        } else {
            // Para personal (roles distintos a ADMIN y PACIENTE), fecha de vigencia ES OBLIGATORIA
            if (usuarioDTO.getFechaVigencia() == null) {
                throw new IllegalArgumentException("La fecha de vigencia es obligatoria para usuarios de personal sin rol ADMIN.");
            }
            // Validar que no sea pasada
            if (usuarioDTO.getFechaVigencia().isBefore(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de vigencia no puede ser pasada.");
            }
            usuario.setFechaVigencia(usuarioDTO.getFechaVigencia());
        }

        // ⚠️ VALIDACIÓN: Prevenir que un usuario ACTIVO quede solo con rol PACIENTE
        // Si estamos editando (no es nuevo) y solo tiene rol PACIENTE, mostrar advertencia
        if (!esNuevo && soloTieneRolPaciente) {
            System.out.println("⚠️ ADVERTENCIA: Usuario ID " + usuario.getId() + " (" + usuario.getEmail() +
                    ") ahora solo tiene el rol PACIENTE. Ya no aparecerá en la lista de usuarios de personal.");
        }

        // --- 5. ACTUALIZAR HORARIOS (SI APLICA) ---
        // Limpiamos los horarios existentes antes de añadir los nuevos (importante para
        // edición)
        usuario.getHorarioRegular().clear();
        usuario.getExcepcionesHorario().clear();

        // Solo procesar horarios si el DTO los incluye y si el usuario tiene rol
        // ODONTOLOGO (o el que decidas)
        boolean esOdontologo = rolesSeleccionados.stream().anyMatch(rol -> "ODONTOLOGO".equals(rol.getNombre()));

        if (esOdontologo) {
            // Guardar Horario Regular
            if (usuarioDTO.getHorarioRegular() != null) {
                usuarioDTO.getHorarioRegular().forEach((dia, horas) -> {
                    // Guardar solo si las horas no están vacías
                    if (StringUtils.hasText(horas)) {
                        // Validar formato aquí si la anotación @Pattern no funcionara directamente en
                        // el Map value
                        // Pattern pattern =
                        // Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9](,([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9])*$");
                        // if (!pattern.matcher(horas).matches()) {
                        // throw new IllegalArgumentException("Formato de horas inválido para " + dia +
                        // ": " + horas);
                        // }
                        usuario.getHorarioRegular().put(dia, horas);
                    }
                });
            }

            // Guardar Excepciones de Horario
            if (usuarioDTO.getExcepcionesHorario() != null) {
                List<HorarioExcepcion> excepciones = usuarioDTO.getExcepcionesHorario().stream()
                        .filter(dto -> dto.getFecha() != null && StringUtils.hasText(dto.getHoras())) // Asegurar datos
                                                                                                      // mínimos
                        .map(dto -> new HorarioExcepcion(dto.getFecha(), dto.getHoras(), dto.getMotivo()))
                        .collect(Collectors.toList());
                usuario.setExcepcionesHorario(excepciones); // JPA se encargará de persistir la colección de @Embeddable
            }
        }
        // Si no es odontólogo, las colecciones de horario permanecerán vacías.

        // --- 6. GUARDAR USUARIO ---
        Usuario usuarioGuardado;
        try {
            usuarioGuardado = usuarioRepository.save(usuario);
            if (esNuevo) {
                crearPacienteParaUsuario(usuarioGuardado);
            }

        } catch (DataIntegrityViolationException e) {
            // Capturar específicamente constraint violations (ej. email duplicado a nivel
            // de BD)
            // Intentar dar un mensaje más específico si es posible
            String mensaje = "Error al guardar: Verifique que el email o número de documento no estén ya registrados.";
            if (e.getMessage().contains("usuarios.UKkfsp0s1tflm1cwlj8idhqsad0")
                    || e.getMessage().toLowerCase().contains("duplicate entry") && e.getMessage().contains("email")) {
                mensaje = "Error al guardar: El email '" + emailNuevo + "' ya existe.";
            } else if (e.getMessage().toLowerCase().contains("duplicate entry")
                    && e.getMessage().contains("numero_documento")) {
                mensaje = "Error al guardar: El número de documento '" + usuarioDTO.getNumeroDocumento()
                        + "' ya existe para ese tipo de documento.";
            }
            throw new DataIntegrityViolationException(mensaje, e);
        }

        // --- 7. Enviar email con contraseÃ±a temporal (si es nuevo) ---
        if (enviarEmailTemporal && usuarioGuardado.getPasswordTemporal() != null) {
            try {
                emailService.enviarPasswordTemporal(
                        usuarioGuardado.getEmail(),
                        usuarioGuardado.getNombreCompleto(),
                        usuarioGuardado.getPasswordTemporal());
            } catch (Exception e) {
                // Loggear el error pero no fallar toda la operación si el guardado fue exitoso
                System.err.println("ALERTA: Usuario guardado (" + usuarioGuardado.getEmail()
                        + ") pero falló el envío de email con contraseña temporal: " + e.getMessage());
                // Podrías añadir un mensaje flash secundario para informar al admin
                // redirectAttributes.addFlashAttribute("warning", "Usuario guardado, pero hubo
                // un problema al enviar el email con la contraseña temporal.");
            }
        }
    }

    // --- MÉTODOS EXISTENTES (listar, buscar, eliminar, cambiarEstado, fuerza
    // bruta, restablecer) ---
    // Revisar si necesitan ajustes menores, pero la lógica principal se mantiene.

    @Override
    public Page<Usuario> listarTodosLosUsuarios(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return usuarioRepository.findUsuariosConRolesDePersonalByKeyword(keyword, pageable);
        }
        return usuarioRepository.findUsuariosConRolesDePersonal(pageable);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        // Podríamos hacer JOIN FETCH de los horarios si siempre se muestran al editar
        // Optional<Usuario> usuarioOpt = usuarioRepository.findByIdWithSchedules(id);
        // // Crear método en repo
        return usuarioRepository.findById(id); // Mantenemos LAZY por ahora
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        if (usuario.isEliminado()) {
            System.out.println(">>> Usuario " + usuario.getEmail() + " ya estaba eliminado lógicamente.");
            return;
        }

        // Proteger al super-administrador
        if (usuario.isEsSuperAdmin()) {
            throw new UnsupportedOperationException("No se puede eliminar al super-administrador del sistema.");
        }

        // ⚠️ VALIDACIÓN: Solo permitir eliminar usuarios con UN SOLO rol
        if (usuario.getRoles() != null && usuario.getRoles().size() > 1) {
            List<String> nombresRoles = usuario.getRoles().stream()
                    .map(Rol::getNombre)
                    .collect(Collectors.toList());
            throw new IllegalStateException(
                    "No se puede eliminar un usuario con múltiples roles. " +
                    "El usuario tiene " + usuario.getRoles().size() + " roles asignados: " +
                    String.join(", ", nombresRoles) + ". " +
                    "Por favor, edite el usuario y deje solo un rol antes de eliminarlo.");
        }

        // Validar que el usuario odontólogo no tenga citas activas
        boolean esOdontologo = usuario.getRoles().stream()
                .anyMatch(rol -> "ODONTOLOGO".equals(rol.getNombre()));

        if (esOdontologo) {
            long citasActivas = citaRepository.countCitasActivasByOdontologo(id);
            if (citasActivas > 0) {
                throw new IllegalStateException(
                        "No se puede eliminar el odontólogo porque tiene " + citasActivas +
                                " cita(s) activa(s). Debe cancelar o completar las citas primero.");
            }
        }

        // Validar que el paciente asociado no tenga citas activas
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            long citasActivas = citaRepository.countCitasActivas(usuario.getPaciente().getId());
            if (citasActivas > 0) {
                throw new IllegalStateException(
                        "No se puede eliminar el usuario porque su perfil de paciente tiene " + citasActivas +
                                " cita(s) activa(s). Debe cancelar o completar las citas primero.");
            }
        }

        // Soft delete del Paciente asociado (si existe y no está eliminado)
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            Paciente paciente = usuario.getPaciente();
            try {
                paciente.setEliminado(true);
                pacienteRepository.save(paciente); // Soft delete manual
                System.out.println(
                        ">>> Paciente asociado " + paciente.getId()
                                + " eliminado (soft delete) por cascada desde usuario.");
            } catch (Exception e) {
                System.err.println(
                        "Error Crítico: No se pudo eliminar (soft delete) el paciente asociado " + paciente.getId()
                                + ". Cancelando eliminación del usuario. Error: " + e.getMessage());
                throw new RuntimeException("No se pudo eliminar el paciente asociado. Operación cancelada.", e);
            }
        } else if (usuario.getPaciente() != null && usuario.getPaciente().isEliminado()) {
            System.out.println(">>> Paciente asociado ya estaba eliminado lógicamente.");
        }

        // Soft delete del Usuario (manual para preservar relaciones con roles)
        try {
            usuario.setEliminado(true);
            usuario.setFechaEliminacion(java.time.LocalDateTime.now());
            usuario.setEstaActivo(false); // Desactivar también al eliminar
            usuarioRepository.save(usuario); // Guardar cambios sin tocar la tabla usuarios_roles
            System.out.println(
                    ">>> Usuario " + usuario.getEmail() + " eliminado (soft delete) con éxito. Roles preservados.");
        } catch (Exception e) {
            System.err.println(
                    "Error Crítico al intentar soft delete del usuario " + usuario.getEmail() + ": " + e.getMessage());
            throw new RuntimeException("Error al marcar el usuario como eliminado.", e);
        }
    }

    @Override
    @Transactional // Añadir @Transactional por si acaso modifica estado
    public void cambiarEstadoUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));
        cambiarEstadoUsuario(id, !usuario.isEstaActivo()); // Llama al método sobrecargado
    }

    @Override
    @Transactional // Añadir @Transactional
    public void cambiarEstadoUsuario(Long id, boolean activar)
            throws UnsupportedOperationException, IllegalStateException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        // Proteger al super-administrador de ser desactivado
        if (usuario.isEsSuperAdmin() && !activar) {
            throw new UnsupportedOperationException("No se puede desactivar al super-administrador del sistema.");
        }

        // --- VALIDACIÓN: No desactivar usuarios con citas activas ---
        if (!activar) {
            // Validar si es odontólogo con citas activas
            boolean esOdontologo = usuario.getRoles().stream()
                    .anyMatch(rol -> "ODONTOLOGO".equals(rol.getNombre()));

            if (esOdontologo) {
                long citasActivas = citaRepository.countCitasActivasByOdontologo(id);
                if (citasActivas > 0) {
                    throw new IllegalStateException(
                            "No se puede desactivar al odontólogo porque tiene " + citasActivas +
                                    " cita(s) activa(s). Debe cancelar o completar las citas primero.");
                }
            }

            // Validar si tiene paciente asociado con citas activas
            if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
                long citasActivas = citaRepository.countCitasActivas(usuario.getPaciente().getId());
                if (citasActivas > 0) {
                    throw new IllegalStateException(
                            "No se puede desactivar el usuario porque su perfil de paciente tiene " + citasActivas +
                                    " cita(s) activa(s). Debe cancelar o completar las citas primero.");
                }
            }
        }
        // --- FIN VALIDACIÓN ---

        usuario.setEstaActivo(activar);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional // Añadir @Transactional
    public void procesarLoginFallido(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email); // Usar findByEmail normal
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Solo incrementar si no está ya bloqueado y está activo
            if (usuario.isEstaActivo() && usuario.getFechaBloqueo() == null) {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
                if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
                    usuario.setFechaBloqueo(LocalDateTime.now());
                    // Considera si quieres marcarlo como inactivo también
                    // usuario.setEstaActivo(false);
                }
                usuarioRepository.save(usuario);
            }
        }
    }

    @Override
    @Transactional // Añadir @Transactional
    public void resetearIntentosFallidos(String email) {
        // Usar un método que pueda encontrarlo aunque esté inactivo o bloqueado
        // temporalmente
        // Necesitaríamos findByEmailIgnorandoSoftDelete si el bloqueo implica soft
        // delete
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email); // Asumimos que no está con soft delete
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Solo resetear si los intentos no son ya 0 o si está bloqueado
            if (usuario.getIntentosFallidos() > 0 || usuario.getFechaBloqueo() != null) {
                usuario.setIntentosFallidos(0);
                usuario.setFechaBloqueo(null);
                // Si el bloqueo lo marcó como inactivo, decidir si reactivarlo aquí
                // if (!usuario.isEstaActivo() && usuario.getFechaBloqueo() != null) {
                // usuario.setEstaActivo(true);
                // }
                usuarioRepository.save(usuario);
            }
        }
    }

    @Override
    @Transactional
    public void restablecerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findByIdIgnorandoSoftDelete(id) // Correcto usar este
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        if (!usuario.isEliminado()) {
            throw new IllegalStateException("El usuario con email " + usuario.getEmail() + " no estÃ¡ eliminado.");
        }

        // Validar si tiene roles asignados y al menos uno activo (antes de restablecer)
        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            throw new IllegalStateException("El usuario no tiene roles asignados. Asigne roles antes de restablecer.");
        }
        boolean tieneAlMenosUnRolValido = usuario.getRoles().stream()
                .anyMatch(rol -> rol.isEstaActivo() && !rol.isEliminado()); // Chequear ambos estados del Rol
        if (!tieneAlMenosUnRolValido) {
            throw new IllegalStateException(
                    "Todos los roles asignados al usuario estÃ¡n inactivos o eliminados. Actualice los roles del usuario antes de restablecer la cuenta.");
        }

        // Restablecer estado del usuario
        usuario.setEliminado(false);
        usuario.setFechaEliminacion(null);
        usuario.setEstaActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);

        // Generar nueva contraseña temporal y forzar cambio
        String passwordTemporal = PasswordUtil.generarPasswordAleatoria(); // Usar tu utilidad
        usuario.setPasswordTemporal(passwordTemporal);
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeActualizarPassword(true);

        usuarioRepository.save(usuario);

        // ✅ RESTAURAR PACIENTE ASOCIADO (si existe y está eliminado)
        if (usuario.getPaciente() != null && usuario.getPaciente().isEliminado()) {
            Paciente paciente = usuario.getPaciente();
            paciente.setEliminado(false);
            pacienteRepository.save(paciente);
            System.out.println("✅ Paciente asociado ID " + paciente.getId() + " restaurado junto con usuario ID " + usuario.getId());
        }

        // Enviar email
        try {
            emailService.enviarPasswordTemporal(
                    usuario.getEmail(),
                    usuario.getNombreCompleto(),
                    passwordTemporal);
        } catch (Exception e) {
            System.err.println("ALERTA: Usuario restablecido (" + usuario.getEmail()
                    + ") pero falló el envío de email con nueva contraseña temporal: " + e.getMessage());
            // Considera relanzar una RuntimeException específica o manejarla en el
            // controller
            throw new RuntimeException("Usuario restablecido, pero falló el envío del email.", e);
        }
    }

    // Método de validación de email único - VALIDACIÓN CRUZADA USUARIOS Y PACIENTES
    private void validarUnicidadEmail(String email, Long idUsuarioExcluir, boolean esNuevo) {
        if (!StringUtils.hasText(email)) {
            return; // No validar si está vacío
        }

        // 1️⃣ Validar en tabla USUARIOS
        Optional<Usuario> existenteConEmailOpt = usuarioRepository.findByEmailIgnorandoSoftDelete(email);

        if (existenteConEmailOpt.isPresent()) {
            Usuario usuarioExistente = existenteConEmailOpt.get();

            // Si es edición, verificar que no sea el mismo usuario
            if (!esNuevo && idUsuarioExcluir != null && usuarioExistente.getId().equals(idUsuarioExcluir)) {
                // Es el mismo usuario, no hay conflicto
                return;
            }

            if (usuarioExistente.isEliminado()) {
                throw new DataIntegrityViolationException(
                        "EMAIL_ELIMINADO:" + usuarioExistente.getId() + ":" + email);
            } else {
                throw new DataIntegrityViolationException(
                        "El email '" + email + "' ya está en uso por otro usuario activo.");
            }
        }

        // 2️⃣ Validar en tabla PACIENTES (NUEVO)
        Optional<Paciente> existentePorEmailPaciente = pacienteRepository.findByEmailIgnorandoSoftDelete(email);

        if (existentePorEmailPaciente.isPresent()) {
            Paciente pacienteConEmail = existentePorEmailPaciente.get();
            boolean perteneceAlUsuarioActual = false;

            // Si estamos editando, verificar si el paciente encontrado pertenece al usuario actual
            if (idUsuarioExcluir != null) {
                Optional<Usuario> usuarioActualOpt = usuarioRepository.findById(idUsuarioExcluir);
                if (usuarioActualOpt.isPresent() && usuarioActualOpt.get().getPaciente() != null) {
                    perteneceAlUsuarioActual = usuarioActualOpt.get().getPaciente().getId()
                            .equals(pacienteConEmail.getId());
                }
            }

            // Si el email existe en Pacientes y NO pertenece al usuario que estamos guardando/editando
            if (!perteneceAlUsuarioActual) {
                throw new DataIntegrityViolationException(
                        "El email '" + email + "' ya está registrado para otro paciente del sistema.");
            }
        }
    }

    // Método de validación de documento - VALIDACIÓN CRUZADA USUARIOS Y PACIENTES
    private void validarUnicidadDocumentoUsuario(String numeroDocumento, Long tipoDocumentoId, Long idUsuarioExcluir) {
        if (!StringUtils.hasText(numeroDocumento) || tipoDocumentoId == null) {
            return; // No validar si falta alguno
        }

        // 1️⃣ Validar en tabla USUARIOS
        Optional<Usuario> existentePorDoc = usuarioRepository
                .findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);

        if (existentePorDoc.isPresent()
                && (idUsuarioExcluir == null || !existentePorDoc.get().getId().equals(idUsuarioExcluir))) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
            throw new DataIntegrityViolationException(
                    "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento
                            + "' ya está registrado para otro usuario.");
        }

        // 2️⃣ Validar en tabla PACIENTES (NUEVO)
        Optional<Paciente> existentePorDocPaciente = pacienteRepository
                .findByNumeroTipoDocumentoIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);

        if (existentePorDocPaciente.isPresent()) {
            Paciente pacienteConDoc = existentePorDocPaciente.get();
            boolean perteneceAlUsuarioActual = false;

            // Si estamos editando, verificar si el paciente encontrado pertenece al usuario actual
            if (idUsuarioExcluir != null) {
                Optional<Usuario> usuarioActualOpt = usuarioRepository.findById(idUsuarioExcluir);
                if (usuarioActualOpt.isPresent() && usuarioActualOpt.get().getPaciente() != null) {
                    perteneceAlUsuarioActual = usuarioActualOpt.get().getPaciente().getId()
                            .equals(pacienteConDoc.getId());
                }
            }

            // Si el documento existe en Pacientes y NO pertenece al usuario que estamos guardando/editando
            if (!perteneceAlUsuarioActual) {
                TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
                throw new DataIntegrityViolationException(
                        "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento
                                + "' ya está registrado para otro paciente del sistema.");
            }
        }
    }

    /**
     * Valida que el usuario tenga al menos 18 años para roles administrativos
     * Solo permite menores de edad si ÚNICAMENTE tienen el rol PACIENTE
     */
    private void validarEdadParaRolesAdministrativos(LocalDate fechaNacimiento, List<Rol> rolesSeleccionados) {
        if (fechaNacimiento == null) {
            return; // Sin fecha de nacimiento, no podemos validar
        }

        // Calcular edad del usuario
        int edad = java.time.Period.between(fechaNacimiento, LocalDate.now()).getYears();

        // Verificar si tiene roles administrativos (cualquier rol que NO sea PACIENTE)
        boolean tieneRolesAdministrativos = rolesSeleccionados.stream()
                .anyMatch(rol -> !"PACIENTE".equals(rol.getNombre()));

        // Si es menor de 18 años y tiene roles administrativos, rechazar
        if (edad < 18 && tieneRolesAdministrativos) {
            throw new IllegalArgumentException(
                    "No se pueden asignar roles administrativos a menores de edad. " +
                    "El usuario tiene " + edad + " años y debe tener al menos 18 años para roles de personal.");
        }
    }

    /**
     * Valida el formato del teléfono para números celulares peruanos
     * - Debe tener exactamente 9 dígitos
     * - Debe empezar con 9
     * - No puede ser un número repetido (ej: 999999999)
     * - No puede ser una secuencia ascendente/descendente (ej: 123456789, 987654321)
     */
    private void validarFormatoTelefono(String telefono) {
        if (!StringUtils.hasText(telefono)) {
            return; // Campo opcional
        }

        // Remover espacios y guiones
        String telefonoLimpio = telefono.replaceAll("[\\s\\-]", "");

        // Validar que solo contenga dígitos
        if (!telefonoLimpio.matches("\\d+")) {
            throw new IllegalArgumentException("El teléfono solo debe contener números.");
        }

        // Validar longitud (9 dígitos para celulares peruanos)
        if (telefonoLimpio.length() != 9) {
            throw new IllegalArgumentException("El teléfono debe tener exactamente 9 dígitos.");
        }

        // Validar que empiece con 9 (celulares peruanos)
        if (!telefonoLimpio.startsWith("9")) {
            throw new IllegalArgumentException("El número celular debe empezar con 9.");
        }

        // Validar que no sea un número repetido (ej: 999999999, 111111111)
        if (telefonoLimpio.matches("(\\d)\\1{8}")) {
            throw new IllegalArgumentException("El teléfono no puede tener todos los dígitos iguales.");
        }

        // Validar que no sea una secuencia ascendente (123456789) o descendente (987654321)
        if (telefonoLimpio.equals("123456789") || telefonoLimpio.equals("987654321")) {
            throw new IllegalArgumentException("El teléfono no puede ser una secuencia numérica simple.");
        }
    }

    // Método de validación de teléfono único - VALIDACIÓN CRUZADA USUARIOS Y PACIENTES
    private void validarUnicidadTelefono(String telefono, Long idUsuarioExcluir) {
        if (!StringUtils.hasText(telefono)) {
            return; // No validar si está vacío (campo opcional)
        }

        // Primero validar formato
        validarFormatoTelefono(telefono);

        // 1️⃣ Validar en tabla USUARIOS
        Optional<Usuario> existentePorTelefono = usuarioRepository
                .findByTelefonoIgnorandoSoftDelete(telefono);

        if (existentePorTelefono.isPresent()
                && (idUsuarioExcluir == null || !existentePorTelefono.get().getId().equals(idUsuarioExcluir))) {
            throw new DataIntegrityViolationException(
                    "El teléfono '" + telefono + "' ya está registrado para otro usuario.");
        }

        // 2️⃣ Validar en tabla PACIENTES (NUEVO)
        Optional<Paciente> existentePorTelefonoPaciente = pacienteRepository
                .findByTelefonoIgnorandoSoftDelete(telefono);

        if (existentePorTelefonoPaciente.isPresent()) {
            Paciente pacienteConTelefono = existentePorTelefonoPaciente.get();
            boolean perteneceAlUsuarioActual = false;

            // Si estamos editando, verificar si el paciente encontrado pertenece al usuario actual
            if (idUsuarioExcluir != null) {
                Optional<Usuario> usuarioActualOpt = usuarioRepository.findById(idUsuarioExcluir);
                if (usuarioActualOpt.isPresent() && usuarioActualOpt.get().getPaciente() != null) {
                    perteneceAlUsuarioActual = usuarioActualOpt.get().getPaciente().getId()
                            .equals(pacienteConTelefono.getId());
                }
            }

            // Si el teléfono existe en Pacientes y NO pertenece al usuario que estamos guardando/editando
            if (!perteneceAlUsuarioActual) {
                throw new DataIntegrityViolationException(
                        "El teléfono '" + telefono + "' ya está registrado para otro paciente del sistema.");
            }
        }
    }

    @Override
    @Transactional
    public void promoverPacienteAPersonal(Long pacienteId, List<Long> rolesIds,
            LocalDate fechaContratacion, LocalDate fechaVigencia) {
        // Validar que se proporcionen roles
        if (rolesIds == null || rolesIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un rol de personal");
        }
        // Validar fecha de contratación (obligatoria, no futura)
        if (fechaContratacion == null) {
            throw new IllegalArgumentException("La fecha de contratación es obligatoria");
        }
        if (fechaContratacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de contratación no puede ser futura");
        }
        // Validar fecha de vigencia (obligatoria)
        if (fechaVigencia == null) {
            throw new IllegalArgumentException("La fecha de vigencia es obligatoria");
        }
        // Buscar el paciente
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        // Verificar que el paciente esté activo
        if (paciente.isEliminado()) {
            throw new IllegalStateException("No se puede promocionar un paciente eliminado");
        }
        // Obtener el usuario asociado al paciente
        Usuario usuario = paciente.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("El paciente no tiene usuario asociado");
        }
        // Validar que el usuario esté activo
        if (!usuario.isEstaActivo()) {
            throw new IllegalStateException("No se puede promocionar un usuario inactivo");
        }

        // ⚠️ VALIDACIÓN: Verificar que el paciente sea mayor de edad para promoción a personal
        if (paciente.getFechaNacimiento() != null) {
            int edad = java.time.Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 18) {
                throw new IllegalArgumentException(
                        "No se puede promocionar a personal a un paciente menor de edad. " +
                        "El paciente tiene " + edad + " años y debe tener al menos 18 años para ser parte del personal.");
            }
        }

        // ✅ SINCRONIZAR datos del paciente al usuario
        // Esto asegura que el usuario tenga todos los datos necesarios para el
        // formulario de edición
        usuario.setTipoDocumento(paciente.getTipoDocumento());
        usuario.setNumeroDocumento(paciente.getNumeroDocumento());
        usuario.setNombreCompleto(paciente.getNombreCompleto());
        usuario.setFechaNacimiento(paciente.getFechaNacimiento());

        // Actualizar teléfono y dirección solo si el paciente los tiene
        if (paciente.getTelefono() != null && !paciente.getTelefono().trim().isEmpty()) {
            usuario.setTelefono(paciente.getTelefono());
        }
        if (paciente.getDireccion() != null && !paciente.getDireccion().trim().isEmpty()) {
            usuario.setDireccion(paciente.getDireccion());
        }

        // Actualizar datos de personal
        usuario.setFechaContratacion(fechaContratacion);
        usuario.setFechaVigencia(fechaVigencia);

        // Agregar los nuevos roles de personal (sin quitar el rol PACIENTE)
        List<Rol> nuevosRoles = rolRepository.findAllById(rolesIds);
        if (nuevosRoles.isEmpty()) {
            throw new IllegalArgumentException("Los roles seleccionados no son válidos");
        }
        // Agregar nuevos roles manteniendo los existentes
        usuario.getRoles().addAll(nuevosRoles);
        // Guardar cambios
        usuarioRepository.save(usuario);
        System.out.println(">>> Paciente " + paciente.getNombreCompleto() +
                " promocionado exitosamente a personal con " + nuevosRoles.size() + " rol(es)");
    }

    /**
     * Crea automáticamente un registro de Paciente para un Usuario con rol PACIENTE
     * Solo se ejecuta si el usuario tiene rol PACIENTE y no tiene registro de
     * paciente
     * 
     * @param usuario Usuario recién creado o actualizado
     */
    private void crearPacienteParaUsuario(Usuario usuario) {
        // Verificar si el usuario tiene rol PACIENTE
        boolean tieneRolPaciente = usuario.getRoles().stream()
                .anyMatch(rol -> "PACIENTE".equals(rol.getNombre()));

        if (!tieneRolPaciente) {
            return; // No es paciente, no hacer nada
        }

        // Verificar si ya tiene registro de paciente
        Optional<Paciente> pacienteExistente = pacienteRepository.findByUsuario(usuario);
        if (pacienteExistente.isPresent()) {
            System.out.println(">>> Usuario ID " + usuario.getId() + " ya tiene registro de paciente");
            return; // Ya tiene registro, no crear duplicado
        }

        // Crear registro de paciente
        Paciente nuevoPaciente = new Paciente();
        nuevoPaciente.setUsuario(usuario);
        nuevoPaciente.setNombreCompleto(usuario.getNombreCompleto());
        nuevoPaciente.setNumeroDocumento(usuario.getNumeroDocumento());
        nuevoPaciente.setTipoDocumento(usuario.getTipoDocumento());
        nuevoPaciente.setEmail(usuario.getEmail());
        nuevoPaciente.setTelefono(usuario.getTelefono());
        nuevoPaciente.setDireccion(usuario.getDireccion());
        nuevoPaciente.setFechaNacimiento(usuario.getFechaNacimiento());
        nuevoPaciente.setAlergias(null);
        nuevoPaciente.setAntecedentesMedicos(null);
        nuevoPaciente.setTratamientosActuales(null);
        nuevoPaciente.setEliminado(false);
        nuevoPaciente.setCreadoPor("SISTEMA");

        pacienteRepository.save(nuevoPaciente);

        System.out.println("✅ Registro de paciente creado automáticamente para usuario: "
                + usuario.getEmail() + " (ID: " + usuario.getId() + ")");
    }

} // Fin de la clase
