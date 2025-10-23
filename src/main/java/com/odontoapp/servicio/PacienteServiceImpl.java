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
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class PacienteServiceImpl implements PacienteService {

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
            TipoDocumentoRepository tipoDocumentoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    private void validarUnicidadDocumento(String numeroDocumento, Long tipoDocumentoId, Long idPacienteExcluir) {
        Optional<Paciente> existentePorDoc = pacienteRepository
                .findByNumeroTipoDocumentoIgnorandoSoftDelete(numeroDocumento, tipoDocumentoId);
        if (existentePorDoc.isPresent()
                && (idPacienteExcluir == null || !existentePorDoc.get().getId().equals(idPacienteExcluir))) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(tipoDocumentoId).orElse(new TipoDocumento());
            throw new DataIntegrityViolationException(
                    "El documento '" + tipoDoc.getCodigo() + " " + numeroDocumento + "' ya está registrado.");
        }
    }

    @Override
    @Transactional
    public void guardarPaciente(PacienteDTO pacienteDTO) {

        // 1️⃣ VALIDACIONES DE DOCUMENTO Y EMAIL (MISMO BLOQUE BASE)
        validarUnicidadDocumento(pacienteDTO.getNumeroDocumento(), pacienteDTO.getTipoDocumentoId(),
                pacienteDTO.getId());

        if (pacienteDTO.getEmail() != null && !pacienteDTO.getEmail().isEmpty()) {
            Optional<Paciente> existentePorEmail = pacienteRepository.findByEmail(pacienteDTO.getEmail());
            if (existentePorEmail.isPresent()
                    && (pacienteDTO.getId() == null || !existentePorEmail.get().getId().equals(pacienteDTO.getId()))) {
                throw new DataIntegrityViolationException(
                        "El Email '" + pacienteDTO.getEmail() + "' ya está en uso por otro paciente activo.");
            }

            Optional<Usuario> usuarioConEmail = usuarioRepository.findByEmail(pacienteDTO.getEmail());
            if (usuarioConEmail.isPresent()) {
                if (pacienteDTO.getId() == null ||
                        !usuarioConEmail.get().getId().equals(
                                pacienteRepository.findById(pacienteDTO.getId())
                                        .map(p -> p.getUsuario() != null ? p.getUsuario().getId() : null)
                                        .orElse(null))) {
                    throw new DataIntegrityViolationException(
                            "El email '" + pacienteDTO.getEmail() + "' ya está en uso por otro usuario del sistema.");
                }
            }
        }

        // 2️⃣ PREPARACIÓN DE ENTIDADES
        Paciente paciente;
        boolean esNuevo = pacienteDTO.getId() == null;
        Usuario usuarioPaciente = null;
        boolean emailCambiado = false;

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
            usuarioPaciente.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuarioPaciente.setEstaActivo(false);
            usuarioPaciente.setVerificationToken(UUID.randomUUID().toString());
            usuarioPaciente.setRoles(Set.of(rolPaciente));

            // 🔥 Ahora dejamos que la cascada del Paciente persista al Usuario
            paciente.setUsuario(usuarioPaciente);

        } else {
            // ✳️ EDICIÓN DE PACIENTE EXISTENTE
            paciente = pacienteRepository.findById(pacienteDTO.getId())
                    .orElseThrow(() -> new IllegalStateException("Paciente no encontrado."));

            if (paciente.getUsuario() != null) {
                usuarioPaciente = paciente.getUsuario();
                if (!usuarioPaciente.getEmail().equals(pacienteDTO.getEmail())) {
                    emailCambiado = true;
                }
            }
        }

        // 3️⃣ ACTUALIZAR USUARIO ASOCIADO (INCLUIDO CASO DE EMAIL CAMBIADO)
        if (usuarioPaciente != null) {
            usuarioPaciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
            usuarioPaciente.setEmail(pacienteDTO.getEmail());

            if (emailCambiado) {
                usuarioPaciente.setEstaActivo(false);
                usuarioPaciente.setVerificationToken(UUID.randomUUID().toString());
            }

            // Se guarda explícitamente para asegurar consistencia antes del paciente
            usuarioRepository.save(usuarioPaciente);
        }

        // 4️⃣ ACTUALIZAR DATOS DEL PACIENTE
        paciente.setNumeroDocumento(pacienteDTO.getNumeroDocumento());
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
        paciente.setEmail(pacienteDTO.getEmail());
        paciente.setTelefono(pacienteDTO.getTelefono());
        paciente.setFechaNacimiento(pacienteDTO.getFechaNacimiento());
        paciente.setDireccion(pacienteDTO.getDireccion());
        paciente.setAlergias(pacienteDTO.getAlergias());
        paciente.setAntecedentesMedicos(pacienteDTO.getAntecedentesMedicos());

        Paciente pacienteGuardado = pacienteRepository.save(paciente);

        // 5️⃣ ENVIAR EMAIL DE ACTIVACIÓN O RE-VERIFICACIÓN
        if (esNuevo || emailCambiado) {
            emailService.enviarEmailActivacionAdmin(
                    pacienteGuardado.getEmail(),
                    pacienteGuardado.getNombreCompleto(),
                    pacienteGuardado.getUsuario().getVerificationToken());
        }
    }

    // 🔥 CREAR USUARIO TEMPORAL PARA REGISTRO (SELF-SERVICE)
    @Override
    @Transactional
    public Usuario crearUsuarioTemporalParaRegistro(String email) {

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmailIgnorandoSoftDelete(email);
        if (usuarioExistente.isPresent() && !usuarioExistente.get().isEliminado()) {
            throw new IllegalStateException(
                    "El email ya está en uso. Si no recuerdas tu clave, contacta al administrador.");
        }

        Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() -> new IllegalStateException("El rol 'PACIENTE' no se encuentra en el sistema."));
        Usuario usuarioTemp = new Usuario();
        usuarioTemp.setEmail(email);
        usuarioTemp.setNombreCompleto("Paciente Pendiente");
        usuarioTemp.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuarioTemp.setEstaActivo(false); // INACTIVO hasta completar el formulario final
        usuarioTemp.setVerificationToken(UUID.randomUUID().toString());
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
        Optional<Paciente> pacienteOpt = pacienteRepository.findById(id);
        if (pacienteOpt.isEmpty()) {
            throw new IllegalStateException("Paciente no encontrado para eliminar con ID: " + id);
        }

        Paciente paciente = pacienteOpt.get();
        Long usuarioId = (paciente.getUsuario() != null) ? paciente.getUsuario().getId() : null;

        // 1. Ejecutar el Soft Delete del Paciente
        // Esto ejecuta la @SQLDelete y establece paciente.eliminado = true en la base
        // de datos
        pacienteRepository.deleteById(id);

        // 2. Desactivar el usuario asociado (si existe)
        if (usuarioId != null) {
            try {
                // Usamos el servicio de usuario con estado forzado (false)
                // Se debe obtener el usuario por separado para evitar el conflicto de estado de
                // persistencia
                Usuario usuarioParaDesactivar = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Usuario asociado no encontrado para desactivar (ID: " + usuarioId + ")"));

                // Si el usuario no está ya eliminado (para no modificar su estado eliminado)
                if (!usuarioParaDesactivar.isEliminado()) {
                    usuarioService.cambiarEstadoUsuario(usuarioId, false);
                    System.out.println(">>> Usuario asociado " + paciente.getUsuario().getEmail()
                            + " desactivado por eliminación de paciente.");
                }

            } catch (Exception e) {
                // En este punto, el paciente ya está marcado como eliminado. Si falla
                // desactivar el usuario,
                // solo se registra la advertencia, pero no se revierte la eliminación del
                // paciente.
                System.err.println("Error al desactivar usuario asociado al paciente " + id + ": " + e.getMessage());
            }
        } else {
            System.out.println(">>> Paciente " + id + " no tenía usuario asociado.");
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