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
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
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

    @Autowired // Modifica el constructor para incluir UsuarioService
    public PacienteServiceImpl(PacienteRepository pacienteRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, EmailService emailService,
            UsuarioService usuarioService, UsuarioRepository usuarioRepository) { // <-- Añade UsuarioService aquí
        this.pacienteRepository = pacienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.usuarioService = usuarioService; // <-- Añade esta línea
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void guardarPaciente(PacienteDTO pacienteDTO) {
        // --- VALIDACIONES DE DNI (ignorando soft delete) ---
        Optional<Paciente> existentePorDni = pacienteRepository.findByDniIgnorandoSoftDelete(pacienteDTO.getDni());
        if (existentePorDni.isPresent() && !existentePorDni.get().getId().equals(pacienteDTO.getId())) {
            throw new DataIntegrityViolationException("El DNI '" + pacienteDTO.getDni() + "' ya está registrado.");
        }

        // --- VALIDACIÓN DE EMAIL (permitiendo emails de pacientes eliminados, pero no
        // duplicados activos) ---
        if (pacienteDTO.getEmail() != null && !pacienteDTO.getEmail().isEmpty()) {
            // Solo validar contra pacientes ACTIVOS (no usar IgnorandoSoftDelete)
            Optional<Paciente> existentePorEmail = pacienteRepository.findByEmail(pacienteDTO.getEmail());

            if (existentePorEmail.isPresent() && !existentePorEmail.get().getId().equals(pacienteDTO.getId())) {
                throw new DataIntegrityViolationException("El Email '" + pacienteDTO.getEmail() + "' ya está en uso.");
            }

            // PERO también validar contra USUARIOS existentes (no pacientes)
            Optional<Usuario> usuarioConEmail = usuarioRepository.findByEmail(pacienteDTO.getEmail());
            if (usuarioConEmail.isPresent()) {
                // Verificar que no sea el usuario asociado al paciente que se está editando
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

        Paciente paciente;
        boolean esNuevo = pacienteDTO.getId() == null;

        if (esNuevo) {
            paciente = new Paciente();

            // Buscar el rol PACIENTE
            Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                    .orElseThrow(() -> new IllegalStateException("El rol 'PACIENTE' no se encuentra en el sistema."));

            // Crear usuario asociado
            Usuario usuarioPaciente = new Usuario();
            usuarioPaciente.setEmail(pacienteDTO.getEmail());
            usuarioPaciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
            usuarioPaciente.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Contraseña temporal
            usuarioPaciente.setEstaActivo(false); // Inactivo hasta activar
            usuarioPaciente.setVerificationToken(UUID.randomUUID().toString()); // Token de activación
            usuarioPaciente.setRoles(Set.of(rolPaciente));

            paciente.setUsuario(usuarioPaciente);

        } else {
            // Si es edición, obtener el paciente existente
            paciente = pacienteRepository.findById(pacienteDTO.getId())
                    .orElseThrow(() -> new IllegalStateException("Paciente no encontrado."));

            // 🔥 AÑADIDO: Actualizar email y nombre del usuario asociado
            if (paciente.getUsuario() != null) {
                Usuario usuarioAsociado = paciente.getUsuario();

                // Validar que el nuevo email no esté en uso por OTRO usuario
                if (!usuarioAsociado.getEmail().equals(pacienteDTO.getEmail())) {
                    Optional<Usuario> otroUsuario = usuarioRepository.findByEmail(pacienteDTO.getEmail());
                    if (otroUsuario.isPresent() && !otroUsuario.get().getId().equals(usuarioAsociado.getId())) {
                        throw new DataIntegrityViolationException(
                                "El email '" + pacienteDTO.getEmail() + "' ya está en uso por otro usuario.");
                    }
                    usuarioAsociado.setEmail(pacienteDTO.getEmail());
                }

                // También actualizar el nombre si cambió
                usuarioAsociado.setNombreCompleto(pacienteDTO.getNombreCompleto());
            }
        }

        // --- ACTUALIZAR DATOS DEL PACIENTE ---
        paciente.setDni(pacienteDTO.getDni());
        paciente.setNombreCompleto(pacienteDTO.getNombreCompleto());
        paciente.setEmail(pacienteDTO.getEmail());
        paciente.setTelefono(pacienteDTO.getTelefono());
        paciente.setFechaNacimiento(pacienteDTO.getFechaNacimiento());
        paciente.setDireccion(pacienteDTO.getDireccion());
        paciente.setAlergias(pacienteDTO.getAlergias());
        paciente.setAntecedentesMedicos(pacienteDTO.getAntecedentesMedicos());

        // --- GUARDAR PACIENTE (y usuario asociado en cascada) ---
        Paciente pacienteGuardado = pacienteRepository.save(paciente);

        // --- ENVIAR EMAIL DE ACTIVACIÓN (solo si es nuevo) ---
        if (esNuevo) {
            emailService.enviarEmailActivacion(
                    pacienteGuardado.getEmail(),
                    pacienteGuardado.getNombreCompleto(),
                    pacienteGuardado.getUsuario().getVerificationToken());
        }
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

    // En src/main/java/com/odontoapp/servicio/PacienteServiceImpl.java

    @Override
    @Transactional
    public void eliminarPaciente(Long id) {
        Optional<Paciente> pacienteOpt = pacienteRepository.findById(id);
        if (pacienteOpt.isPresent()) {
            Paciente paciente = pacienteOpt.get();
            Usuario usuarioAsociado = paciente.getUsuario();
            Long usuarioId = (usuarioAsociado != null) ? usuarioAsociado.getId() : null;

            // 🔹 VALIDACIÓN OPCIONAL: Evitar eliminar si tiene citas pendientes
            /*
             * if (paciente.getCitas() != null &&
             * paciente.getCitas().stream().anyMatch(c ->
             * "PENDIENTE".equalsIgnoreCase(c.getEstado()))) {
             * throw new DataIntegrityViolationException(
             * "No se puede eliminar un paciente con citas pendientes. Cancele o complete las citas primero."
             * );
             * }
             */

            // 1️⃣ Primero se marca el paciente para eliminación (soft delete)
            pacienteRepository.deleteById(id);

            // 2️⃣ Luego desactivamos el usuario asociado dentro de la MISMA transacción
            if (usuarioId != null) {
                try {
                    Usuario usuarioParaDesactivar = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Usuario asociado no encontrado para desactivar (ID: " + usuarioId + ")"));

                    if (usuarioParaDesactivar.isEstaActivo()) {
                        usuarioParaDesactivar.setEstaActivo(false);
                        usuarioRepository.save(usuarioParaDesactivar); // ⚡ Forzar UPDATE inmediato
                        System.out.println(">>> Usuario asociado " + usuarioParaDesactivar.getEmail()
                                + " desactivado y guardado.");
                    } else {
                        System.out.println(
                                ">>> Usuario asociado " + usuarioParaDesactivar.getEmail() + " ya estaba inactivo.");
                    }

                } catch (IllegalStateException e) {
                    System.err.println("Advertencia: " + e.getMessage());
                } catch (Exception e) {
                    System.err
                            .println("Error al desactivar usuario asociado al paciente " + id + ": " + e.getMessage());
                    // Opcional: podrías relanzar la excepción si deseas revertir todo
                    // throw new RuntimeException("Fallo al desactivar usuario asociado.", e);
                }
            } else {
                System.out.println(">>> Paciente " + id + " no tenía usuario asociado.");
            }

            // 🔸 Al finalizar, la transacción aplicará ambos cambios (soft delete + update)
        } else {
            throw new IllegalStateException("Paciente no encontrado para eliminar con ID: " + id);
        }
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        return pacienteRepository.findByDni(dni);
    }

}