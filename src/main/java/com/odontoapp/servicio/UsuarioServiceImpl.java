package com.odontoapp.servicio;

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
import com.odontoapp.entidad.Rol; // NUEVO import
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.entidad.Usuario; // NUEVO import
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.util.PasswordUtil;

import jakarta.transaction.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private final EmailService emailService;
    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    // Inyecta las dependencias necesarias
    public UsuarioServiceImpl(EmailService emailService, PacienteRepository pacienteRepository,
            PasswordEncoder passwordEncoder, RolRepository rolRepository,
            TipoDocumentoRepository tipoDocumentoRepository, UsuarioRepository usuarioRepository) {
        this.emailService = emailService;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
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
        Optional<Usuario> existenteConEmailOpt = usuarioRepository.findByEmailIgnorandoSoftDelete(emailNuevo);

        if (esNuevo) {
            if (existenteConEmailOpt.isPresent()) {
                Usuario usuarioExistente = existenteConEmailOpt.get();
                if (usuarioExistente.isEliminado()) {
                    throw new DataIntegrityViolationException(
                            "EMAIL_ELIMINADO:" + usuarioExistente.getId() + ":" + emailNuevo);
                } else {
                    throw new DataIntegrityViolationException(
                            "El email '" + emailNuevo + "' ya estÃ¡ en uso por otro usuario activo.");
                }
            }
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
                if (existenteConEmailOpt.isPresent()) {
                    Usuario usuarioConNuevoEmail = existenteConEmailOpt.get();
                    if (usuarioConNuevoEmail.isEliminado()) {
                        throw new DataIntegrityViolationException(
                                "EMAIL_ELIMINADO:" + usuarioConNuevoEmail.getId() + ":" + emailNuevo);
                    } else {
                        throw new DataIntegrityViolationException(
                                "El email '" + emailNuevo + "' ya estÃ¡ en uso por otro usuario activo.");
                    }
                }
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
            java.time.Period edad = java.time.Period.between(usuarioDTO.getFechaNacimiento(), java.time.LocalDate.now());
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
        usuario.setRoles(new HashSet<>(rolesSeleccionados));

        // --- VALIDAR Y ESTABLECER FECHA DE VIGENCIA ---
        boolean tieneRolAdmin = rolesSeleccionados.stream().anyMatch(r -> "ADMIN".equals(r.getNombre()));

        if (tieneRolAdmin) {
            // Si tiene rol ADMIN, la fecha de vigencia no es requerida (puede ser null o infinita)
            usuario.setFechaVigencia(usuarioDTO.getFechaVigencia()); // Puede ser null
        } else {
            // Para cualquier otro rol, fecha de vigencia ES OBLIGATORIA
            if (usuarioDTO.getFechaVigencia() == null) {
                throw new IllegalArgumentException("La fecha de vigencia es obligatoria para usuarios sin rol ADMIN.");
            }
            // Validar que no sea pasada
            if (usuarioDTO.getFechaVigencia().isBefore(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de vigencia no puede ser pasada.");
            }
            usuario.setFechaVigencia(usuarioDTO.getFechaVigencia());
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
            // Considerar buscar también por número de documento si es relevante
            // return usuarioRepository.findByKeywordIncludingDocument(keyword, pageable);
            // // Necesitarías crear este método
            return usuarioRepository.findByKeyword(keyword, pageable);
        }
        return usuarioRepository.findAll(pageable);
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

        // Soft delete del Paciente asociado (si existe y no está eliminado)
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            Long pacienteId = usuario.getPaciente().getId();
            try {
                pacienteRepository.deleteById(pacienteId); // Activa @SQLDelete de Paciente
                System.out.println(
                        ">>> Paciente asociado " + pacienteId + " eliminado (soft delete) por cascada desde usuario.");
            } catch (Exception e) {
                System.err.println("Error Crítico: No se pudo eliminar (soft delete) el paciente asociado " + pacienteId
                        + ". Cancelando eliminación del usuario. Error: " + e.getMessage());
                throw new RuntimeException("No se pudo eliminar el paciente asociado. Operación cancelada.", e);
            }
        } else if (usuario.getPaciente() != null && usuario.getPaciente().isEliminado()) {
            System.out.println(">>> Paciente asociado ya estaba eliminado lógicamente.");
        }

        // Soft delete del Usuario
        try {
            usuarioRepository.deleteById(id); // Activa @SQLDelete de Usuario
            System.out.println(">>> Usuario " + usuario.getEmail() + " eliminado (soft delete) con éxito.");
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

        // --- NUEVA VALIDACIÓN: No desactivar si es el único rol activo de algún
        // usuario ---
        if (!activar && usuario.getRoles() != null) {
            boolean esRolUnicoParaAlguien = usuario.getRoles().stream()
                    .anyMatch(rol -> rol.getUsuarios() != null && rol.getUsuarios().stream()
                            .anyMatch(u -> u.getRoles().stream().filter(Rol::isEstaActivo).count() == 1
                                    && u.getRoles().contains(rol)));
            if (esRolUnicoParaAlguien) {
                // Esta lógica es más para cambiar estado de ROL, pero la dejamos comentada como
                // referencia
                // throw new DataIntegrityViolationException("No se puede desactivar este rol
                // porque dejaría a uno o más usuarios sin roles activos.");
            }
        }
        // --- FIN NUEVA VALIDACIÓN ---

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

        // Restablecer estado
        usuario.setEliminado(false);
        usuario.setFechaEliminacion(null);
        usuario.setEstaActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);

        // Generar nueva contraseÃ±a temporal y forzar cambio
        String passwordTemporal = PasswordUtil.generarPasswordAleatoria(); // Usar tu utilidad
        usuario.setPasswordTemporal(passwordTemporal);
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeActualizarPassword(true);

        usuarioRepository.save(usuario);

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

    // Método de validación de documento
    private void validarUnicidadDocumentoUsuario(String numeroDocumento, Long tipoDocumentoId, Long idUsuarioExcluir) {
        if (!StringUtils.hasText(numeroDocumento) || tipoDocumentoId == null) {
            return; // No validar si falta alguno
        }
        Optional<Usuario> existentePorDoc = usuarioRepository
                .findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);

        if (existentePorDoc.isPresent()
                && (idUsuarioExcluir == null || !existentePorDoc.get().getId().equals(idUsuarioExcluir))) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
            throw new DataIntegrityViolationException(
                    "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento
                            + "' ya está registrado para otro usuario.");
        }
    }

    // Método de validación de teléfono único
    private void validarUnicidadTelefono(String telefono, Long idUsuarioExcluir) {
        if (!StringUtils.hasText(telefono)) {
            return; // No validar si está vacío (campo opcional)
        }
        Optional<Usuario> existentePorTelefono = usuarioRepository
                .findByTelefonoIgnorandoSoftDelete(telefono);

        if (existentePorTelefono.isPresent()
                && (idUsuarioExcluir == null || !existentePorTelefono.get().getId().equals(idUsuarioExcluir))) {
            throw new DataIntegrityViolationException(
                    "El teléfono '" + telefono + "' ya está registrado para otro usuario.");
        }
    }

} // Fin de la clase
