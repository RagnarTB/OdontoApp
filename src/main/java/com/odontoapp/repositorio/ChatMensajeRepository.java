package com.odontoapp.repositorio;

import com.odontoapp.entidad.ChatMensaje;
import com.odontoapp.entidad.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio para gestionar el historial de mensajes del chatbot
 */
@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    /**
     * Obtiene los últimos 10 mensajes de un paciente
     */
    List<ChatMensaje> findTop10ByPacienteAndEliminadoFalseOrderByFechaHoraDesc(Paciente paciente);

    /**
     * Cuenta el total de mensajes de un paciente
     */
    Long countByPacienteAndEliminadoFalse(Paciente paciente);
}
