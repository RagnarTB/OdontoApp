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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal del chatbot.
 * Gestiona la lógica de negocio, construcción de contexto y generación de
 * respuestas.
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

            // Construir el contexto del sistema con datos del paciente
            String contextoSistema = construirContextoSistema(paciente);

            // Construir el prompt completo con instrucciones
            String promptCompleto = construirPrompt(contextoSistema, request.getMensaje());

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
     * Construye el contexto del sistema con los datos reales del paciente
     */
    private String construirContextoSistema(Paciente paciente) {
        StringBuilder contexto = new StringBuilder();

        // Información básica del paciente
        contexto.append("INFORMACIÓN DEL PACIENTE:\n");
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

        // Citas próximas (usando el método que existe en el repositorio)
        try {
            List<Cita> citasProximas = citaRepository
                    .findByFechaHoraInicioAfterAndPacienteId(
                            LocalDateTime.now(),
                            paciente.getId(),
                            PageRequest.of(0, 5))
                    .getContent();

            if (!citasProximas.isEmpty()) {
                contexto.append("\nCITAS PRÓXIMAS:\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (Cita cita : citasProximas) {
                    contexto.append("- ")
                            .append(cita.getFechaHoraInicio().format(formatter))
                            .append(" - ")
                            .append(cita.getMotivoConsulta() != null ? cita.getMotivoConsulta() : "Consulta general")
                            .append(" (Estado: ").append(cita.getEstadoCita().getNombre()).append(")\n");
                }
            } else {
                contexto.append("\nNo tienes citas próximas programadas.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener citas próximas: ", e);
            contexto.append("\nNo se pudieron cargar las citas próximas.\n");
        }

        // Tratamientos recientes
        try {
            List<TratamientoRealizado> tratamientos = tratamientoRealizadoRepository
                    .findByPacienteId(paciente.getId(), PageRequest.of(0, 5))
                    .getContent();

            if (!tratamientos.isEmpty()) {
                contexto.append("\nTRATAMIENTOS RECIENTES:\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (TratamientoRealizado tr : tratamientos) {
                    contexto.append("- ")
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
            contexto.append("\nNo se pudieron cargar los tratamientos.\n");
        }

        // Comprobantes pendientes de pago
        try {
            List<Comprobante> comprobantesPendientes = comprobanteRepository
                    .findByPacienteIdAndMontoPendienteGreaterThan(
                            paciente.getId(),
                            BigDecimal.ZERO);

            if (!comprobantesPendientes.isEmpty()) {
                contexto.append("\nCOMPROBANTES PENDIENTES DE PAGO:\n");
                for (Comprobante comp : comprobantesPendientes) {
                    contexto.append("- Comprobante #")
                            .append(comp.getNumeroComprobante())
                            .append(": S/ ")
                            .append(comp.getMontoPendiente())
                            .append(" (Total: S/ ")
                            .append(comp.getMontoTotal())
                            .append(")\n");
                }
            } else {
                contexto.append("\nNo tienes comprobantes pendientes de pago.\n");
            }
        } catch (Exception e) {
            log.error("Error al obtener comprobantes: ", e);
            contexto.append("\nNo se pudieron cargar los comprobantes.\n");
        }

        return contexto.toString();
    }

    /**
     * Construye el prompt completo con instrucciones del sistema
     */
    private String construirPrompt(String contextoSistema, String preguntaUsuario) {
        return """
                Eres un asistente virtual del sistema OdontoApp, una aplicación de gestión odontológica.

                INSTRUCCIONES IMPORTANTES:
                1. SOLO puedes responder preguntas relacionadas con:
                   - Información del paciente (citas, tratamientos, comprobantes)
                   - Servicios odontológicos generales
                   - Funcionamiento del sistema OdontoApp
                   - Procedimientos dentales comunes
                   - Cuidados dentales y salud bucal

                2. Si te preguntan sobre temas NO relacionados con odontología o el sistema (política, deportes,
                   programación, entretenimiento, etc.), debes responder EXACTAMENTE:
                   "Lo siento, soy un asistente especializado en OdontoApp y solo puedo ayudarte con temas
                   relacionados con tu atención odontológica y el uso del sistema. ¿Hay algo sobre tus citas,
                   tratamientos o servicios dentales en lo que pueda ayudarte?"

                3. Sé amable, profesional y conciso en tus respuestas.

                4. Si no tienes información suficiente para responder sobre algo específico del paciente,
                   sugiere contactar directamente a la clínica.

                5. NO inventes información. Solo usa los datos proporcionados en el contexto del paciente.

                6. Si el paciente pregunta sobre agendar citas, explícale que puede hacerlo desde el menú
                   "Mis Citas" en el portal del paciente.

                7. Responde en español de forma clara y fácil de entender.

                CONTEXTO DEL PACIENTE:
                """ + contextoSistema + """

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
}
