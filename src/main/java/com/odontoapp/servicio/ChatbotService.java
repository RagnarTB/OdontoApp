package com.odontoapp.servicio;

import com.odontoapp.dto.ChatRequest;
import com.odontoapp.dto.ChatResponse;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio principal del chatbot con contexto completo del sistema.
 * Incluye información de procedimientos, odontólogos, y guías de uso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final GeminiService geminiService;
    private final ChatMensajeRepository chatMensajeRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaProcedimientoRepository categoriaProcedimientoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final MetodoPagoRepository metodoPagoRepository;

    /**
     * Procesa un mensaje del usuario y genera una respuesta
     */
    @Transactional
    public ChatResponse procesarMensaje(ChatRequest request) {
        try {
            // Obtener el paciente autenticado
            Paciente paciente = obtenerPacienteAutenticado();
            if (paciente == null) {
                return ChatResponse.error("No se pudo identificar al paciente.");
            }

            // Construir el contexto completo del sistema
            String contextoCompleto = construirContextoCompleto(paciente);

            // Construir el prompt completo con instrucciones
            String promptCompleto = construirPrompt(contextoCompleto, request.getMensaje());

            // Generar respuesta con Gemini
            String respuestaIA = geminiService.generarRespuesta(promptCompleto);

            // Guardar en el historial
            ChatMensaje mensaje = new ChatMensaje();
            mensaje.setPaciente(paciente);
            mensaje.setMensajeUsuario(request.getMensaje());
            mensaje.setRespuestaBot(respuestaIA);
            mensaje.setFechaHora(LocalDateTime.now());
            mensaje.setEliminado(false);

            ChatMensaje guardado = chatMensajeRepository.save(mensaje);

            return new ChatResponse(respuestaIA, guardado.getId());

        } catch (Exception e) {
            log.error("Error al procesar mensaje del chatbot: ", e);
            return ChatResponse.error("Ocurrió un error al procesar tu mensaje. Por favor, intenta nuevamente.");
        }
    }

    /**
     * Construye el contexto completo del sistema con toda la información disponible
     */
    private String construirContextoCompleto(Paciente paciente) {
        StringBuilder contexto = new StringBuilder();

        // 1. Información del paciente
        contexto.append(construirContextoPaciente(paciente));

        // 2. Servicios/Procedimientos disponibles
        contexto.append(construirContextoProcedimientos());

        // 3. Odontólogos disponibles
        contexto.append(construirContextoOdontologos());

        // 4. Estados de citas disponibles
        contexto.append(construirContextoEstadosCitas());

        // 5. Métodos de pago
        contexto.append(construirContextoMetodosPago());

        // 6. Guía de funcionalidades del sistema
        contexto.append(construirGuiaFuncionalidades());

        return contexto.toString();
    }

    /**
     * Construye el contexto del paciente con sus datos personales
     */
    private String construirContextoPaciente(Paciente paciente) {
        StringBuilder contexto = new StringBuilder();

        contexto.append("=== INFORMACIÓN DEL PACIENTE ===\n");
        contexto.append("- Nombre: ").append(paciente.getNombreCompleto()).append("\n");
        contexto.append("- Email: ").append(paciente.getEmail()).append("\n");

        if (paciente.getTelefono() != null && !paciente.getTelefono().isEmpty()) {
            contexto.append("- Teléfono: ").append(paciente.getTelefono()).append("\n");
        }

        if (paciente.getAlergias() != null && !paciente.getAlergias().isEmpty()) {
            contexto.append("- Alergias: ").append(paciente.getAlergias()).append("\n");
        }

        if (paciente.getAntecedentesMedicos() != null && !paciente.getAntecedentesMedicos().isEmpty()) {
            contexto.append("- Antecedentes médicos: ").append(paciente.getAntecedentesMedicos()).append("\n");
        }

        // Citas próximas
        try {
            List<Cita> citasProximas = citaRepository
                    .findByFechaHoraInicioAfterAndPacienteId(
                            LocalDateTime.now(),
                            paciente.getId(),
                            PageRequest.of(0, 5))
                    .getContent();

            if (!citasProximas.isEmpty()) {
                contexto.append("\n--- Citas Próximas ---\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (Cita cita : citasProximas) {
                    contexto.append("• ")
                            .append(cita.getFechaHoraInicio().format(formatter))
                            .append(" con Dr(a). ")
                            .append(cita.getOdontologo().getNombreCompleto())
                            .append("\n  Motivo: ")
                            .append(cita.getMotivoConsulta() != null ? cita.getMotivoConsulta() : "Consulta general")
                            .append("\n  Estado: ").append(cita.getEstadoCita().getNombre())
                            .append("\n");
                }
            } else {
                contexto.append("\nNo tienes citas próximas programadas.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener citas próximas: ", e);
        }

        // Tratamientos recientes
        try {
            List<TratamientoRealizado> tratamientos = tratamientoRealizadoRepository
                    .findByPacienteId(paciente.getId(), PageRequest.of(0, 5))
                    .getContent();

            if (!tratamientos.isEmpty()) {
                contexto.append("\n--- Tratamientos Recientes ---\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (TratamientoRealizado tr : tratamientos) {
                    contexto.append("• ")
                            .append(tr.getFechaRealizacion().format(formatter))
                            .append(": ")
                            .append(tr.getProcedimiento() != null ? tr.getProcedimiento().getNombre() : "Tratamiento")
                            .append("\n");
                }
            } else {
                contexto.append("\nNo tienes tratamientos registrados aún.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener tratamientos: ", e);
        }

        // Comprobantes pendientes
        try {
            List<Comprobante> comprobantesPendientes = comprobanteRepository
                    .findByPacienteIdAndMontoPendienteGreaterThan(
                            paciente.getId(),
                            BigDecimal.ZERO);

            if (!comprobantesPendientes.isEmpty()) {
                contexto.append("\n--- Comprobantes Pendientes de Pago ---\n");
                for (Comprobante comp : comprobantesPendientes) {
                    contexto.append("• Comprobante #")
                            .append(comp.getNumeroComprobante())
                            .append(": S/ ")
                            .append(comp.getMontoPendiente())
                            .append(" de S/ ")
                            .append(comp.getMontoTotal())
                            .append("\n");
                }
            } else {
                contexto.append("\nNo tienes comprobantes pendientes de pago.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener comprobantes: ", e);
        }

        contexto.append("\n");
        return contexto.toString();
    }

    /**
     * Construye el contexto de procedimientos/servicios disponibles
     */
    private String construirContextoProcedimientos() {
        StringBuilder contexto = new StringBuilder();
        contexto.append("=== SERVICIOS/PROCEDIMIENTOS DISPONIBLES ===\n");

        try {
            List<CategoriaProcedimiento> categorias = categoriaProcedimientoRepository.findByEstaActivaTrue();

            for (CategoriaProcedimiento categoria : categorias) {
                List<Procedimiento> procedimientos = procedimientoRepository.findByCategoriaId(
                        categoria.getId(),
                        PageRequest.of(0, 100)).getContent();

                if (!procedimientos.isEmpty()) {
                    contexto.append("\n--- ").append(categoria.getNombre()).append(" ---\n");
                    if (categoria.getDescripcion() != null && !categoria.getDescripcion().isEmpty()) {
                        contexto.append("Descripción: ").append(categoria.getDescripcion()).append("\n");
                    }

                    for (Procedimiento proc : procedimientos) {
                        contexto.append("• ").append(proc.getNombre());
                        if (proc.getPrecioBase() != null) {
                            contexto.append(" - S/ ").append(proc.getPrecioBase());
                        }
                        if (proc.getDuracionBaseMinutos() > 0) {
                            contexto.append(" (").append(proc.getDuracionBaseMinutos()).append(" min)");
                        }
                        if (proc.getDescripcion() != null && !proc.getDescripcion().isEmpty()) {
                            contexto.append("\n  ").append(proc.getDescripcion());
                        }
                        contexto.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al obtener procedimientos: ", e);
            contexto.append("No se pudieron cargar los servicios disponibles.\n");
        }

        contexto.append("\n");
        return contexto.toString();
    }

    /**
     * Construye el contexto de odontólogos disponibles
     */
    private String construirContextoOdontologos() {
        StringBuilder contexto = new StringBuilder();
        contexto.append("=== ODONTÓLOGOS DISPONIBLES ===\n");

        try {
            List<Usuario> odontologos = usuarioRepository.findActiveByRolesNombre("ODONTOLOGO");

            if (!odontologos.isEmpty()) {
                for (Usuario odontologo : odontologos) {
                    contexto.append("• Dr(a). ").append(odontologo.getNombreCompleto());

                    if (odontologo.getTelefono() != null && !odontologo.getTelefono().isEmpty()) {
                        contexto.append(" - Tel: ").append(odontologo.getTelefono());
                    }

                    // Horarios de atención
                    Map<DayOfWeek, String> horarios = odontologo.getHorarioRegular();
                    if (horarios != null && !horarios.isEmpty()) {
                        contexto.append("\n  Horarios de atención:\n");
                        for (Map.Entry<DayOfWeek, String> entry : horarios.entrySet()) {
                            contexto.append("  - ").append(traducirDia(entry.getKey()))
                                    .append(": ").append(entry.getValue()).append("\n");
                        }
                    }
                    contexto.append("\n");
                }
            } else {
                contexto.append("No hay odontólogos disponibles en este momento.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener odontólogos: ", e);
            contexto.append("No se pudieron cargar los odontólogos disponibles.\n");
        }

        contexto.append("\n");
        return contexto.toString();
    }

    /**
     * Construye el contexto de estados de citas
     */
    private String construirContextoEstadosCitas() {
        StringBuilder contexto = new StringBuilder();
        contexto.append("=== ESTADOS DE CITAS ===\n");

        try {
            List<EstadoCita> estados = estadoCitaRepository.findAll();
            for (EstadoCita estado : estados) {
                contexto.append("• ").append(estado.getNombre());
                if (estado.getDescripcion() != null && !estado.getDescripcion().isEmpty()) {
                    contexto.append(": ").append(estado.getDescripcion());
                }
                contexto.append("\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener estados de citas: ", e);
        }

        contexto.append("\n");
        return contexto.toString();
    }

    /**
     * Construye el contexto de métodos de pago
     */
    private String construirContextoMetodosPago() {
        StringBuilder contexto = new StringBuilder();
        contexto.append("=== MÉTODOS DE PAGO DISPONIBLES ===\n");

        try {
            List<MetodoPago> metodos = metodoPagoRepository.findAll();
            for (MetodoPago metodo : metodos) {
                contexto.append("• ").append(metodo.getNombre());
                if (metodo.getDescripcion() != null && !metodo.getDescripcion().isEmpty()) {
                    contexto.append(": ").append(metodo.getDescripcion());
                }
                contexto.append("\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener métodos de pago: ", e);
        }

        contexto.append("\n");
        return contexto.toString();
    }

    /**
     * Construye la guía completa de funcionalidades del sistema
     */
    private String construirGuiaFuncionalidades() {
        return """
                === GUÍA DE FUNCIONALIDADES DEL SISTEMA ===

                **CÓMO REGISTRARSE EN EL SISTEMA:**
                1. Accede a la página de registro: /registro
                2. Ingresa tu email
                3. Recibirás un correo con un link de activación
                4. Haz clic en el link y completa el formulario con tus datos personales
                5. Tu cuenta quedará activa y podrás iniciar sesión

                **CÓMO INICIAR SESIÓN:**
                1. Ve a la página de login: /login
                2. Ingresa tu email y contraseña
                3. Serás redirigido a tu dashboard personal

                **CÓMO AGENDAR UNA CITA:**
                1. Ve al menú "Mis Citas" en tu portal de paciente: /paciente/citas
                2. Haz clic en "Agendar nueva cita"
                3. Selecciona el odontólogo, fecha, hora y procedimiento
                4. Confirma la cita
                5. Recibirás una confirmación por email

                **CÓMO CANCELAR UNA CITA:**
                1. Ve a "Mis Citas": /paciente/citas
                2. Busca la cita que deseas cancelar
                3. Haz clic en el botón de cancelar
                4. Confirma la cancelación

                **CÓMO CAMBIAR MI CONTRASEÑA:**
                1. Ve a "Cambiar Contraseña": /usuarios/cambiar-password
                2. Ingresa tu contraseña actual
                3. Ingresa tu nueva contraseña (debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas, números y caracteres especiales)
                4. Confirma la nueva contraseña
                5. Guarda los cambios

                **CÓMO RECUPERAR MI CONTRASEÑA:**
                1. En la página de login, haz clic en "¿Olvidaste tu contraseña?"
                2. Ingresa tu email
                3. Recibirás un correo con un link de recuperación
                4. Haz clic en el link y establece tu nueva contraseña

                **CÓMO EDITAR MI PERFIL:**
                1. Ve a "Mi Perfil": /paciente/perfil/editar
                2. Actualiza tus datos personales (teléfono, dirección, etc.)
                3. Actualiza tu información médica (alergias, antecedentes)
                4. Guarda los cambios

                **CÓMO VER MI HISTORIAL MÉDICO:**
                1. Ve a "Mi Historial": /paciente/perfil/historial
                2. Podrás ver todos tus tratamientos realizados
                3. Ver tus citas pasadas y futuras
                4. Descargar tu historial clínico

                **CÓMO VER MIS COMPROBANTES:**
                1. Ve a "Mi Perfil" o "Mis Comprobantes"
                2. Verás la lista de todos tus comprobantes
                3. Puedes ver el detalle de cada comprobante
                4. Ver el estado de pago (pendiente, pagado parcial, pagado total)
                5. Descargar el comprobante en PDF

                **CONTACTO:**
                Si necesitas ayuda adicional o tienes preguntas específicas, puedes contactar directamente con la clínica a través de los canales oficiales.

                """;
    }

    /**
     * Construye el prompt completo con todas las instrucciones
     */
    private String construirPrompt(String contextoCompleto, String preguntaUsuario) {
        return """
                Eres un asistente virtual del sistema OdontoApp, una aplicación de gestión odontológica.

                INSTRUCCIONES IMPORTANTES:

                1. ÁMBITO DE RESPUESTA:
                   - SOLO responde preguntas sobre odontología, salud bucal y el sistema OdontoApp
                   - Usa ÚNICAMENTE la información proporcionada en el contexto del sistema
                   - NO inventes información que no esté en el contexto
                   - Si no tienes la información exacta, sugiere contactar con la clínica

                2. RESPUESTAS PROHIBIDAS:
                   - Si te preguntan sobre temas NO relacionados (política, deportes, programación, entretenimiento, etc.):
                     "Lo siento, soy un asistente especializado en OdontoApp y solo puedo ayudarte con temas
                     relacionados con tu atención odontológica y el uso del sistema. ¿Hay algo sobre tus citas,
                     tratamientos o servicios dentales en lo que pueda ayudarte?"

                3. CÓMO RESPONDER:
                   - Sé amable, profesional y conciso
                   - Usa la información del paciente cuando sea relevante
                   - Da instrucciones paso a paso cuando pregunten "cómo hacer algo"
                   - Menciona URLs específicas cuando des instrucciones (ej: /paciente/citas)
                   - Si preguntan por servicios, lista los disponibles con precios
                   - Si preguntan por odontólogos, menciona sus nombres y horarios
                   - Si preguntan sobre citas, verifica si el paciente tiene citas próximas

                4. EJEMPLOS DE BUENAS RESPUESTAS:
                   - "¿Qué servicios ofrecen?" → Lista los procedimientos reales del sistema con precios
                   - "¿Cómo me registro?" → Da los pasos exactos con URLs
                   - "¿Quiénes son los odontólogos?" → Lista los odontólogos reales con horarios
                   - "¿Cuánto cuesta una limpieza?" → Busca en los procedimientos el precio exacto

                5. RESPONDE EN ESPAÑOL de forma clara y fácil de entender

                CONTEXTO COMPLETO DEL SISTEMA:
                """ + contextoCompleto + """

                PREGUNTA DEL PACIENTE:
                """ + preguntaUsuario + """

                RESPUESTA:
                """;
    }

    /**
     * Obtiene el paciente autenticado desde el contexto de seguridad
     */
    private Paciente obtenerPacienteAutenticado() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            return pacienteRepository.findByEmail(email)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error al obtener paciente autenticado: ", e);
            return null;
        }
    }

    /**
     * Obtiene el historial de chat del paciente autenticado
     */
    public List<ChatMensaje> obtenerHistorial() {
        Paciente paciente = obtenerPacienteAutenticado();
        if (paciente == null) {
            return List.of();
        }
        return chatMensajeRepository
                .findTop10ByPacienteAndEliminadoFalseOrderByFechaHoraDesc(paciente);
    }

    /**
     * Traduce el día de la semana al español
     */
    private String traducirDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
