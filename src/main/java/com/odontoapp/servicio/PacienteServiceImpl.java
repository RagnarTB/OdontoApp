package com.odontoapp.servicio;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public PacienteServiceImpl(PacienteRepository pacienteRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, EmailService emailService) {
        this.pacienteRepository = pacienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void guardarPaciente(PacienteDTO pacienteDTO) {
        // --- VALIDACIONES DE DNI Y EMAIL (ignorando soft delete) ---
        Optional<Paciente> existentePorDni = pacienteRepository.findByDniIgnorandoSoftDelete(pacienteDTO.getDni());
        if (existentePorDni.isPresent() && !existentePorDni.get().getId().equals(pacienteDTO.getId())) {
            throw new DataIntegrityViolationException("El DNI '" + pacienteDTO.getDni() + "' ya está registrado.");
        }

        if (pacienteDTO.getEmail() != null && !pacienteDTO.getEmail().isEmpty()) {
            Optional<Paciente> existentePorEmail = pacienteRepository
                    .findByEmailIgnorandoSoftDelete(pacienteDTO.getEmail());
            if (existentePorEmail.isPresent() && !existentePorEmail.get().getId().equals(pacienteDTO.getId())) {
                throw new DataIntegrityViolationException("El Email '" + pacienteDTO.getEmail() + "' ya está en uso.");
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

    @Override
    public void eliminarPaciente(Long id) {
        pacienteRepository.deleteById(id);
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        return pacienteRepository.findByDni(dni);
    }
}