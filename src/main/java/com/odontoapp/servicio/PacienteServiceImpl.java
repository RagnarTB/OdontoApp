// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\PacienteServiceImpl.java
package com.odontoapp.servicio;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.dto.RegistroPacienteDTO;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class PacienteServiceImpl implements PacienteService {
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository; // Nuevo

    @Autowired
    public PacienteServiceImpl(PacienteRepository pacienteRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, EmailService emailService,
            UsuarioService usuarioService, UsuarioRepository usuarioRepository,
            TipoDocumentoRepository tipoDocumentoRepository, CitaRepository citaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.citaRepository = citaRepository;
    }

    // Método modificado para validar DNI también en Usuarios
    private void validarUnicidadDocumento(String numeroDocumento, Long tipoDocumentoId, Long idPacienteExcluir) {
        // 1. Validar en Pacientes (como antes)
        Optional<Paciente> existentePorDocPaciente = pacienteRepository
                .findByNumeroTipoDocumentoIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);
        if (existentePorDocPaciente.isPresent()
                && (idPacienteExcluir == null || !existentePorDocPaciente.get().getId().equals(idPacienteExcluir))) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
            throw new DataIntegrityViolationException(
                    "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento
                            + "' ya está registrado para otro paciente.");
        }

        // 2. Validar en Usuarios (NUEVO)
        Optional<Usuario> existentePorDocUsuario = usuarioRepository
                .findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);

        if (existentePorDocUsuario.isPresent()) {
            Usuario usuarioConDoc = existentePorDocUsuario.get();
            boolean perteneceAlPacienteActual = false;

            // Si estamos editando, verificar si el usuario encontrado es el asociado al
            // paciente actual
            if (idPacienteExcluir != null) {
                Optional<Paciente> pacienteActualOpt = pacienteRepository.findById(idPacienteExcluir);
                if (pacienteActualOpt.isPresent() && pacienteActualOpt.get().getUsuario() != null) {
                    perteneceAlPacienteActual = pacienteActualOpt.get().getUsuario().getId()
                            .equals(usuarioConDoc.getId());
                }
            }

            // Si el documento existe en Usuarios y NO pertenece al paciente que estamos
            // guardando/editando
            if (!perteneceAlPacienteActual) {
                TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
                // Podríamos diferenciar si el usuario es paciente o personal, pero por ahora un
                // mensaje general
                throw new DataIntegrityViolationException(
                        "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento
                                + "' ya está registrado para otro usuario del sistema (personal o paciente inactivo).");
            }
        }
    }

    @Override
    @Transactional
    public void guardarPaciente(PacienteDTO pacienteDTO) {

        // 1️⃣ VALIDACIÓN DE DOCUMENTO
        validarUnicidadDocumento(pacienteDTO.getNumeroDocumento(), pacienteDTO.getTipoDocumentoId(),
                pacienteDTO.getId());

        // 2️⃣ VALIDACIÓN MEJORADA DE EMAIL
        if (pacienteDTO.getEmail() != null && !pacienteDTO.getEmail().isEmpty()) {

            Optional<Usuario> existenteConEmailOpt = usuarioRepository
                    .findByEmailIgnorandoSoftDelete(pacienteDTO.getEmail());

            if (existenteConEmailOpt.isPresent()) {
                Usuario usuarioExistente = existenteConEmailOpt.get();
                Long idPacienteActual = pacienteDTO.getId();

                // Comprobar si el email encontrado pertenece al PACIENTE que estamos editando
                boolean emailPerteneceAPacienteActual = false;
                if (idPacienteActual != null) {
                    Optional<Paciente> pacienteActualOpt = pacienteRepository.findById(idPacienteActual);
                    if (pacienteActualOpt.isPresent() && pacienteActualOpt.get().getUsuario() != null) {
                        emailPerteneceAPacienteActual = pacienteActualOpt.get().getUsuario().getId()
                                .equals(usuarioExistente.getId());
                    }
                }

                // Si el email existe y NO pertenece al paciente que estamos editando...
                if (!emailPerteneceAPacienteActual) {
                    if (usuarioExistente.isEliminado()) {
                        // Email pertenece a usuario eliminado
                        throw new DataIntegrityViolationException(
                                "EMAIL_ELIMINADO:" + usuarioExistente.getId() + ":" + pacienteDTO.getEmail());
                    } else {
                        // Email pertenece a otro usuario activo (sea paciente o personal)
                        throw new DataIntegrityViolationException(
                                "El email '" + pacienteDTO.getEmail()
                                        + "' ya está en uso por otro usuario del sistema.");
                    }
                }
                // Si el email existe PERO pertenece al paciente actual, no hay problema.
            }

            // Comprobación adicional (por si acaso) de la tabla Paciente
            Optional<Paciente> existentePorEmailPaciente = pacienteRepository
                    .findByEmailIgnorandoSoftDelete(pacienteDTO.getEmail());
            if (existentePorEmailPaciente.isPresent()
                    && (pacienteDTO.getId() == null
                            || !existentePorEmailPaciente.get().getId().equals(pacienteDTO.getId()))) {
                throw new DataIntegrityViolationException(
                        "El Email '" + pacienteDTO.getEmail()
                                + "' ya está en uso por otro paciente (activo o inactivo).");
            }
        }

        // 3️⃣ PREPARACIÓN DE ENTIDADES
        Paciente paciente;
        boolean esNuevo = pacienteDTO.getId() == null;
        Usuario usuarioPaciente = null;
        boolean emailCambiado = false;
        boolean enviarEmailActivacion = false; // Flag para enviar email

        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(pacienteDTO.getTipoDocumentoId())
                .orElseThrow(() -> new IllegalStateException("Tipo de documento no encontrado."));

        if (esNuevo) {
            // 🟢 CREACIÓN DE NUEVO PACIENTE + USUARIO INACTIVO ASOCIADO
            paciente = new Paciente();
            Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                    .orElseThrow(() -> new IllegalStateException("El rol 'PACIENTE' no se encuentra en el sistema."));

            usuarioPaciente = new Usuario();
            usuarioPaciente.setEmail(pacienteDTO.getEmail());
            usuarioPaciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
            usuarioPaciente.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Pass temporal
            usuarioPaciente.setEstaActivo(false); // Inactivo hasta que se active por email
            usuarioPaciente.setVerificationToken(UUID.randomUUID().toString());
            usuarioPaciente.setRoles(Set.of(rolPaciente));

            paciente.setUsuario(usuarioPaciente); // El save del paciente persistirá al usuario
            enviarEmailActivacion = true; // Marcar para enviar email

        } else {
            // ✳️ EDICIÓN DE PACIENTE EXISTENTE
            paciente = pacienteRepository.findById(pacienteDTO.getId())
                    .orElseThrow(() -> new IllegalStateException("Paciente no encontrado."));

            if (paciente.getUsuario() != null) {
                usuarioPaciente = paciente.getUsuario();
                // Comprobar si el email cambió
                if (!usuarioPaciente.getEmail().equals(pacienteDTO.getEmail())) {
                    emailCambiado = true;
                    enviarEmailActivacion = true; // Marcar para enviar re-verificación
                }
            } else {
                // Caso raro: Paciente existe pero no tiene usuario. Creamos uno.
                Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                        .orElseThrow(
                                () -> new IllegalStateException("El rol 'PACIENTE' no se encuentra en el sistema."));
                usuarioPaciente = new Usuario();
                usuarioPaciente.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                usuarioPaciente.setEstaActivo(false);
                paciente.setUsuario(usuarioPaciente);
                enviarEmailActivacion = true;
            }
        }

        // 4️⃣ ACTUALIZAR USUARIO ASOCIADO (siempre se actualiza nombre/email)
        usuarioPaciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
        usuarioPaciente.setEmail(pacienteDTO.getEmail());

        if (emailCambiado) { // Si el email cambió, forzar re-activación
            usuarioPaciente.setEstaActivo(false);
            usuarioPaciente.setVerificationToken(UUID.randomUUID().toString());
        }

        // El usuario se guardará por cascada al guardar el paciente
        // usuarioRepository.save(usuarioPaciente); // No es necesario si
        // CascadeType.PERSIST/MERGE está en Paciente.usuario

        // 5️⃣ ACTUALIZAR DATOS DEL PACIENTE
        paciente.setNumeroDocumento(pacienteDTO.getNumeroDocumento());
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
        paciente.setEmail(pacienteDTO.getEmail());
        paciente.setTelefono(pacienteDTO.getTelefono());
        paciente.setFechaNacimiento(pacienteDTO.getFechaNacimiento());
        paciente.setDireccion(pacienteDTO.getDireccion());
        paciente.setAlergias(pacienteDTO.getAlergias());
        paciente.setAntecedentesMedicos(pacienteDTO.getAntecedentesMedicos());

        Paciente pacienteGuardado = pacienteRepository.save(paciente); // Esto guarda paciente Y usuario

        // 6️⃣ ENVIAR EMAIL DE ACTIVACIÓN O RE-VERIFICACIÓN
        if (enviarEmailActivacion && pacienteGuardado.getUsuario().getVerificationToken() != null) {
            emailService.enviarEmailActivacionAdmin( // Usa el flujo de activación (establecer contraseña)
                    pacienteGuardado.getEmail(),
                    pacienteGuardado.getNombreCompleto(),
                    pacienteGuardado.getUsuario().getVerificationToken());
        }
    }

    // 🔥 CREAR USUARIO TEMPORAL PARA REGISTRO (SELF-SERVICE)
    @Override
    @Transactional
    public Usuario crearUsuarioTemporalParaRegistro(String email) {

        Optional<Usuario> usuarioExistenteOpt = usuarioRepository.findByEmailIgnorandoSoftDelete(email);

        if (usuarioExistenteOpt.isPresent()) {
            Usuario usuarioExistente = usuarioExistenteOpt.get();

            if (usuarioExistente.isEliminado()) {
                // Email pertenece a una cuenta eliminada → controlador decidirá si restaurar
                throw new IllegalStateException(
                        "EMAIL_ELIMINADO_REGISTRO:" + email // Mensaje especial para el controlador
                );
            } else {
                // Email pertenece a cuenta activa o inactiva
                throw new IllegalStateException(
                        "El email ya está registrado en el sistema. Si olvidaste tu contraseña, contáctanos.");
            }
        }

        // --- Si no existe, proceder a crear el usuario temporal ---
        Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() -> new IllegalStateException("El rol 'PACIENTE' no se encuentra en el sistema."));

        Usuario usuarioTemp = new Usuario();
        usuarioTemp.setEmail(email);
        usuarioTemp.setNombreCompleto("Paciente Pendiente"); // Temporal hasta completar datos
        usuarioTemp.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Password inválida hasta
                                                                                       // registro final
        usuarioTemp.setEstaActivo(false); // INACTIVO hasta completar el registro
        usuarioTemp.setVerificationToken(UUID.randomUUID().toString()); // Token para el siguiente paso (formulario
                                                                        // datos)
        usuarioTemp.setRoles(Set.of(rolPaciente));

        return usuarioRepository.save(usuarioTemp);
    }

    // 🔥 COMPLETAR REGISTRO PACIENTE (SELF-SERVICE)
    @Override
    @Transactional
    public void completarRegistroPaciente(RegistroPacienteDTO registroDTO, String token, String password) {

        Usuario usuario = usuarioRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalStateException("Token de registro inválido o ya expirado."));

        if (usuario.isEstaActivo()) {
            throw new IllegalStateException("La cuenta ya ha sido activada anteriormente.");
        }

        // 1. Validación de unicidad de documento antes de proceder
        validarUnicidadDocumento(registroDTO.getNumeroDocumento(), registroDTO.getTipoDocumentoId(), null);

        // 2. Actualizar Usuario
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNombreCompleto(registroDTO.getNombreCompleto());
        usuario.setEstaActivo(true); // 🔥 ACTIVACIÓN CLAVE
        usuario.setVerificationToken(null);
        usuarioRepository.save(usuario);

        // 3. Crear Paciente asociado
        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(registroDTO.getTipoDocumentoId())
                .orElseThrow(() -> new IllegalStateException("Tipo de documento no encontrado."));

        // Llenar datos del paciente
        paciente.setNumeroDocumento(registroDTO.getNumeroDocumento());
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNombreCompleto(registroDTO.getNombreCompleto());
        paciente.setEmail(registroDTO.getEmail());
        paciente.setTelefono(registroDTO.getTelefono());
        paciente.setFechaNacimiento(registroDTO.getFechaNacimiento());
        paciente.setDireccion(registroDTO.getDireccion());
        paciente.setAlergias(registroDTO.getAlergias());
        paciente.setAntecedentesMedicos(registroDTO.getAntecedentesMedicos());

        pacienteRepository.save(paciente);
    }

    @Override
    public Page<Paciente> listarTodosLosPacientes(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return pacienteRepository.findByKeyword(keyword, pageable);
        }
        return pacienteRepository.findAll(pageable);
    }

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteRepository.findById(id);
    }

    @Override
    @Transactional
    public void eliminarPaciente(Long id) {
        // Buscar paciente (ignorando soft delete para asegurar que lo encontramos
        // aunque ya esté marcado)
        Paciente paciente = pacienteRepository.findByIdIgnorandoSoftDelete(id)
                .orElseThrow(() -> new IllegalStateException("Paciente no encontrado para eliminar con ID: " + id));

        // Si ya está eliminado, no hacer nada más
        if (paciente.isEliminado()) {
            System.out.println(">>> Paciente con ID " + id + " ya estaba eliminado lógicamente.");
            return;
        }

        // ⚠️ VALIDAR: No permitir eliminación si tiene citas activas
        if (paciente.getUsuario() != null) {
            long citasActivas = citaRepository.countCitasActivas(paciente.getUsuario().getId());
            if (citasActivas > 0) {
                throw new IllegalStateException(
                        "No se puede eliminar el paciente porque tiene " + citasActivas +
                                " cita(s) activa(s). Debe cancelar o completar todas las citas antes de eliminar el paciente.");
            }
        }

        Usuario usuarioAsociado = paciente.getUsuario();
        Long usuarioId = (usuarioAsociado != null) ? usuarioAsociado.getId() : null;

        // 1. Intentar Soft Delete del Usuario asociado PRIMERO (si existe y SOLO es
        // Paciente)
        if (usuarioAsociado != null && !usuarioAsociado.isEliminado()) {
            // Verificar si el usuario SOLO tiene el rol PACIENTE
            boolean soloEsPaciente = usuarioAsociado.getRoles().stream()
                    .allMatch(rol -> "PACIENTE".equals(rol.getNombre()))
                    && usuarioAsociado.getRoles().size() == 1;

            if (soloEsPaciente) {
                try {
                    // Aplicar soft delete al usuario (activa @SQLDelete de Usuario)
                    usuarioRepository.deleteById(usuarioId);
                    System.out.println(">>> Usuario asociado " + usuarioAsociado.getEmail()
                            + " eliminado (soft delete) por eliminación de paciente.");
                } catch (Exception e) {
                    // Si falla eliminar el usuario, detenemos la operación para evitar
                    // inconsistencias.
                    System.err.println("Error Crítico: No se pudo eliminar (soft delete) el usuario asociado "
                            + usuarioAsociado.getEmail() + ". Cancelando eliminación del paciente. Error: "
                            + e.getMessage());
                    // Podrías lanzar una excepción personalizada o DataIntegrityViolationException
                    throw new RuntimeException("No se pudo eliminar el usuario asociado. Operación cancelada.", e);
                }
            } else {
                // Si el usuario tiene otros roles (es personal), NO lo eliminamos, solo lo
                // desactivamos.
                try {
                    if (usuarioAsociado.isEstaActivo()) { // Solo desactivar si está activo
                        usuarioService.cambiarEstadoUsuario(usuarioId, false);
                        System.out.println(">>> Usuario asociado " + usuarioAsociado.getEmail()
                                + " (personal) desactivado por eliminación de paciente.");
                    }
                } catch (Exception e) {
                    System.err.println("Advertencia: No se pudo desactivar el usuario (personal) asociado al paciente "
                            + id + ": " + e.getMessage());
                    // Continuamos, ya que el paciente se eliminará de todas formas.
                }
            }
        } else if (usuarioAsociado != null && usuarioAsociado.isEliminado()) {
            System.out.println(
                    ">>> Usuario asociado " + usuarioAsociado.getEmail() + " ya estaba eliminado lógicamente.");
        } else {
            System.out.println(">>> Paciente " + id + " no tenía usuario asociado o ya fue procesado.");
        }

        // 2. Ejecutar el Soft Delete del Paciente DESPUÉS del usuario (si aplica)
        try {
            pacienteRepository.deleteById(id); // Activa @SQLDelete de Paciente
            System.out.println(">>> Paciente con ID " + id + " eliminado (soft delete) con éxito.");
        } catch (Exception e) {
            // Si llega aquí, es un error inesperado al marcar el paciente como eliminado.
            System.err.println("Error Crítico al intentar soft delete del paciente " + id + ": " + e.getMessage());
            // Considera relanzar para que la transacción haga rollback
            throw new RuntimeException("Error al marcar el paciente como eliminado.", e);
        }
    }

    // 🔥 MODIFICADO: Buscar por documento (reemplaza buscarPorDni)
    @Override
    public Optional<Paciente> buscarPorDocumento(String numeroDocumento, Long tipoDocumentoId) {
        return pacienteRepository.findByNumeroTipoDocumento(numeroDocumento, tipoDocumentoId);
    }

    @Override
    @Transactional
    public void restablecerPaciente(Long id) {

        // 🔥 CORRECCIÓN: Usar el método que ignora el @Where para encontrar el registro
        // eliminado
        Paciente paciente = pacienteRepository.findByIdIgnorandoSoftDelete(id)
                .orElseThrow(() -> new IllegalStateException("El paciente con ID " + id + " no existe."));

        if (!paciente.isEliminado()) {
            throw new IllegalStateException("El paciente no está en estado de eliminado.");
        }

        // 1. Restablecer Paciente
        paciente.setEliminado(false);
        pacienteRepository.save(paciente);

        // 2. Restablecer y activar Usuario asociado
        if (paciente.getUsuario() != null) {
            Usuario usuario = paciente.getUsuario();
            usuario.setEstaActivo(true); // Activar usuario
            usuario.setFechaEliminacion(null);
            usuario.setEliminado(false);
            usuarioRepository.save(usuario);
        }
    }
}