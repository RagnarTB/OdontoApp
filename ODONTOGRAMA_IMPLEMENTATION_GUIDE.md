# 🦷 GUÍA DE IMPLEMENTACIÓN DEL ODONTOGRAMA

## 📋 ÍNDICE
1. [Análisis de la Situación Actual](#análisis)
2. [Propuesta de Arquitectura](#arquitectura)
3. [Implementación Backend](#backend)
4. [Implementación Frontend](#frontend)
5. [Integraciones](#integraciones)
6. [Roadmap de Implementación](#roadmap)

---

## 🔍 ANÁLISIS DE LA SITUACIÓN ACTUAL {#análisis}

### ✅ Lo que YA tienes:

**Entidades:**
```java
// OdontogramaDiente - Estado actual de cada diente
- ID, paciente, numeroDiente (FDI: "11"-"48")
- estado (SANO, CARIES, RESTAURADO, ENDODONCIA, CORONA, etc.)
- superficiesAfectadas, notas
- fechaUltimaModificacion

// OdontogramaHistorial - Auditoría de cambios
- ID, paciente, numeroDiente
- estadoAnterior, estadoNuevo
- notas, fechaCambio, usuario, tratamientoRealizado
```

**Repositorios completos:**
```java
OdontogramaDienteRepository
  - findByPaciente(Usuario)
  - findByPacienteAndNumeroDiente(Usuario, String)
  - findByPacienteAndEstado(Usuario, String)

OdontogramaHistorialRepository
  - findByPacienteOrderByFechaCambioDesc(Usuario)
  - findByPacienteAndNumeroDienteOrderByFechaCambioDesc(...)
```

**API REST:**
```
GET    /api/odontograma/{pacienteId}
POST   /api/odontograma/{pacienteId}
POST   /api/odontograma/{pacienteId}/inicializar
```

### ❌ Lo que FALTA:
- Servicio completo usando tablas OdontogramaDiente
- Vista interactiva del odontograma
- Integración con tratamientos realizados
- Auto-actualización desde TratamientoRealizado

---

## 🏗️ PROPUESTA DE ARQUITECTURA {#arquitectura}

### Enfoque Recomendado: **Migrar a tablas normalizadas**

#### ¿Por qué NO usar JSON?
- ❌ Difícil de consultar (no puedes hacer: "busca todos los pacientes con caries")
- ❌ Sin validación a nivel de BD
- ❌ Sin historial automático
- ❌ Difícil de reportar

#### ¿Por qué SÍ usar OdontogramaDiente + OdontogramaHistorial?
- ✅ Consultas SQL eficientes
- ✅ Historial completo de cambios
- ✅ Validación con constraints
- ✅ Fácil de reportar y analizar
- ✅ Integración natural con TratamientoRealizado

---

## 💻 IMPLEMENTACIÓN BACKEND {#backend}

### 1. Crear Nuevo Servicio Completo

**Archivo:** `OdontogramaDienteService.java`

```java
package com.odontoapp.servicio;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.entidad.OdontogramaDiente;
import java.util.List;
import java.util.Map;

public interface OdontogramaDienteService {

    /**
     * Obtiene el odontograma completo de un paciente
     * Si no existe, lo inicializa con todos los dientes en estado SANO
     */
    List<OdontogramaDienteDTO> obtenerOdontogramaCompleto(Long pacienteUsuarioId);

    /**
     * Actualiza el estado de un diente específico
     * Registra el cambio en OdontogramaHistorial automáticamente
     */
    OdontogramaDienteDTO actualizarEstadoDiente(
        Long pacienteUsuarioId,
        String numeroDiente,
        String nuevoEstado,
        String superficiesAfectadas,
        String notas,
        Long tratamientoRealizadoId  // Opcional, para vincular
    );

    /**
     * Actualiza múltiples dientes a la vez (desde el frontend)
     */
    List<OdontogramaDienteDTO> actualizarMultiplesDientes(
        Long pacienteUsuarioId,
        List<OdontogramaDienteDTO> cambios
    );

    /**
     * Inicializa odontograma con los 32 dientes permanentes
     */
    List<OdontogramaDienteDTO> inicializarOdontograma(Long pacienteUsuarioId);

    /**
     * Obtiene historial de cambios de un diente
     */
    List<OdontogramaHistorial> obtenerHistorialDiente(
        Long pacienteUsuarioId,
        String numeroDiente
    );

    /**
     * Obtiene estadísticas del odontograma del paciente
     */
    Map<String, Integer> obtenerEstadisticas(Long pacienteUsuarioId);

    /**
     * Actualiza odontograma automáticamente desde TratamientoRealizado
     */
    void actualizarDesdeTratamiento(Long tratamientoRealizadoId);
}
```

**DTO para transferir datos:**

```java
package com.odontoapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OdontogramaDienteDTO {
    private Long id;
    private String numeroDiente;       // "11", "12", ..., "48"
    private String estado;             // "SANO", "CARIES", etc.
    private String superficiesAfectadas; // "Oclusal,Vestibular"
    private String notas;
    private LocalDateTime fechaUltimaModificacion;

    // Info adicional para el frontend
    private String cuadrante;          // "1", "2", "3", "4"
    private String posicion;           // "1", "2", ..., "8"
    private String nombreDiente;       // "Incisivo Central", "Molar", etc.
}
```

### 2. Implementación del Servicio

**Archivo:** `OdontogramaDienteServiceImpl.java`

```java
package com.odontoapp.servicio.impl;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OdontogramaDienteServiceImpl implements OdontogramaDienteService {

    private final OdontogramaDienteRepository odontogramaDienteRepository;
    private final OdontogramaHistorialRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final TratamientoRealizadoRepository tratamientoRepository;

    // Mapa de nombres de dientes según FDI
    private static final Map<String, String> NOMBRES_DIENTES = Map.ofEntries(
        Map.entry("1", "Incisivo Central"), Map.entry("2", "Incisivo Lateral"),
        Map.entry("3", "Canino"), Map.entry("4", "Primer Premolar"),
        Map.entry("5", "Segundo Premolar"), Map.entry("6", "Primer Molar"),
        Map.entry("7", "Segundo Molar"), Map.entry("8", "Tercer Molar (Muela del Juicio)")
    );

    public OdontogramaDienteServiceImpl(
            OdontogramaDienteRepository odontogramaDienteRepository,
            OdontogramaHistorialRepository historialRepository,
            UsuarioRepository usuarioRepository,
            TratamientoRealizadoRepository tratamientoRepository) {
        this.odontogramaDienteRepository = odontogramaDienteRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.tratamientoRepository = tratamientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdontogramaDienteDTO> obtenerOdontogramaCompleto(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(paciente);

        // Si no tiene odontograma, inicializarlo
        if (dientes.isEmpty()) {
            return inicializarOdontograma(pacienteUsuarioId);
        }

        return dientes.stream()
            .map(this::convertirADTO)
            .sorted(Comparator.comparing(OdontogramaDienteDTO::getNumeroDiente))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OdontogramaDienteDTO actualizarEstadoDiente(
            Long pacienteUsuarioId,
            String numeroDiente,
            String nuevoEstado,
            String superficiesAfectadas,
            String notas,
            Long tratamientoRealizadoId) {

        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        // Buscar o crear el diente
        OdontogramaDiente diente = odontogramaDienteRepository
            .findByPacienteAndNumeroDiente(paciente, numeroDiente)
            .orElseGet(() -> {
                OdontogramaDiente nuevo = new OdontogramaDiente();
                nuevo.setPaciente(paciente);
                nuevo.setNumeroDiente(numeroDiente);
                nuevo.setEstado("SANO");
                return nuevo;
            });

        // Guardar estado anterior para historial
        String estadoAnterior = diente.getEstado();

        // Actualizar estado
        diente.setEstado(nuevoEstado);
        diente.setSuperficiesAfectadas(superficiesAfectadas);
        diente.setNotas(notas);
        diente.setFechaUltimaModificacion(LocalDateTime.now());

        OdontogramaDiente dienteGuardado = odontogramaDienteRepository.save(diente);

        // Registrar en historial
        registrarEnHistorial(paciente, numeroDiente, estadoAnterior, nuevoEstado,
                           notas, tratamientoRealizadoId);

        return convertirADTO(dienteGuardado);
    }

    @Override
    @Transactional
    public List<OdontogramaDienteDTO> actualizarMultiplesDientes(
            Long pacienteUsuarioId,
            List<OdontogramaDienteDTO> cambios) {

        List<OdontogramaDienteDTO> resultados = new ArrayList<>();

        for (OdontogramaDienteDTO cambio : cambios) {
            OdontogramaDienteDTO resultado = actualizarEstadoDiente(
                pacienteUsuarioId,
                cambio.getNumeroDiente(),
                cambio.getEstado(),
                cambio.getSuperficiesAfectadas(),
                cambio.getNotas(),
                null
            );
            resultados.add(resultado);
        }

        return resultados;
    }

    @Override
    @Transactional
    public List<OdontogramaDienteDTO> inicializarOdontograma(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        List<OdontogramaDiente> dientes = new ArrayList<>();

        // Crear los 32 dientes permanentes (cuadrantes 1-4, dientes 1-8)
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int posicion = 1; posicion <= 8; posicion++) {
                String numeroDiente = String.valueOf(cuadrante) + posicion;

                // Verificar que no exista ya
                if (!odontogramaDienteRepository
                        .existsByPacienteAndNumeroDiente(paciente, numeroDiente)) {

                    OdontogramaDiente diente = new OdontogramaDiente();
                    diente.setPaciente(paciente);
                    diente.setNumeroDiente(numeroDiente);
                    diente.setEstado("SANO");
                    diente.setFechaUltimaModificacion(LocalDateTime.now());
                    dientes.add(diente);
                }
            }
        }

        List<OdontogramaDiente> guardados = odontogramaDienteRepository.saveAll(dientes);

        return guardados.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdontogramaHistorial> obtenerHistorialDiente(
            Long pacienteUsuarioId, String numeroDiente) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);
        return historialRepository
            .findByPacienteAndNumeroDienteOrderByFechaCambioDesc(paciente, numeroDiente);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> obtenerEstadisticas(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);
        List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(paciente);

        Map<String, Integer> estadisticas = new HashMap<>();
        estadisticas.put("total", dientes.size());
        estadisticas.put("sanos", (int) dientes.stream()
            .filter(d -> "SANO".equals(d.getEstado())).count());
        estadisticas.put("caries", (int) dientes.stream()
            .filter(d -> "CARIES".equals(d.getEstado())).count());
        estadisticas.put("restaurados", (int) dientes.stream()
            .filter(d -> "RESTAURADO".equals(d.getEstado())).count());
        estadisticas.put("extraidos", (int) dientes.stream()
            .filter(d -> "EXTRACCION".equals(d.getEstado())).count());

        return estadisticas;
    }

    @Override
    @Transactional
    public void actualizarDesdeT ratamiento(Long tratamientoRealizadoId) {
        TratamientoRealizado tratamiento = tratamientoRepository.findById(tratamientoRealizadoId)
            .orElseThrow(() -> new RuntimeException("Tratamiento no encontrado"));

        String piezaDental = tratamiento.getPiezaDental();
        if (piezaDental == null || piezaDental.isEmpty()) {
            return; // No hay pieza dental específica
        }

        // Mapear procedimiento a estado del diente
        String procedimientoNombre = tratamiento.getProcedimiento().getNombre().toUpperCase();
        String nuevoEstado = mapearProcedimientoAEstado(procedimientoNombre);

        if (nuevoEstado != null) {
            actualizarEstadoDiente(
                tratamiento.getCita().getPaciente().getId(),
                piezaDental,
                nuevoEstado,
                null,
                "Actualizado automáticamente desde tratamiento: " + procedimientoNombre,
                tratamientoRealizadoId
            );
        }
    }

    // =============== MÉTODOS PRIVADOS ===============

    private Usuario buscarPaciente(Long pacienteUsuarioId) {
        return usuarioRepository.findById(pacienteUsuarioId)
            .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteUsuarioId));
    }

    private void registrarEnHistorial(Usuario paciente, String numeroDiente,
                                     String estadoAnterior, String estadoNuevo,
                                     String notas, Long tratamientoRealizadoId) {

        OdontogramaHistorial historial = new OdontogramaHistorial();
        historial.setPaciente(paciente);
        historial.setNumeroDiente(numeroDiente);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setNotas(notas);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setUsuario(paciente); // TODO: Obtener usuario actual del SecurityContext

        if (tratamientoRealizadoId != null) {
            TratamientoRealizado tratamiento = tratamientoRepository
                .findById(tratamientoRealizadoId).orElse(null);
            historial.setTratamientoRealizado(tratamiento);
        }

        historialRepository.save(historial);
    }

    private OdontogramaDienteDTO convertirADTO(OdontogramaDiente diente) {
        OdontogramaDienteDTO dto = new OdontogramaDienteDTO();
        dto.setId(diente.getId());
        dto.setNumeroDiente(diente.getNumeroDiente());
        dto.setEstado(diente.getEstado());
        dto.setSuperficiesAfectadas(diente.getSuperficiesAfectadas());
        dto.setNotas(diente.getNotas());
        dto.setFechaUltimaModificacion(diente.getFechaUltimaModificacion());

        // Extraer cuadrante y posición
        String numero = diente.getNumeroDiente();
        dto.setCuadrante(numero.substring(0, 1));
        dto.setPosicion(numero.substring(1));
        dto.setNombreDiente(NOMBRES_DIENTES.getOrDefault(dto.getPosicion(), "Diente"));

        return dto;
    }

    private String mapearProcedimientoAEstado(String procedimiento) {
        if (procedimiento.contains("OBTURACIÓN") || procedimiento.contains("RESTAURACIÓN")) {
            return "RESTAURADO";
        }
        if (procedimiento.contains("ENDODONCIA") || procedimiento.contains("CONDUCTO")) {
            return "ENDODONCIA";
        }
        if (procedimiento.contains("CORONA")) {
            return "CORONA";
        }
        if (procedimiento.contains("EXTRACCIÓN") || procedimiento.contains("EXODONCIA")) {
            return "EXTRACCION";
        }
        if (procedimiento.contains("IMPLANTE")) {
            return "IMPLANTE";
        }
        return null; // No se puede mapear automáticamente
    }
}
```

### 3. Crear Controlador MVC para la Vista

**Archivo:** `OdontogramaViewController.java`

```java
package com.odontoapp.controlador;

import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/odontograma")
public class OdontogramaViewController {

    private final OdontogramaDienteService odontogramaService;

    public OdontogramaViewController(OdontogramaDienteService odontogramaService) {
        this.odontogramaService = odontogramaService;
    }

    /**
     * Vista principal del odontograma de un paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    public String verOdontograma(@PathVariable Long pacienteId, Model model) {
        var odontograma = odontogramaService.obtenerOdontogramaCompleto(pacienteId);
        var estadisticas = odontogramaService.obtenerEstadisticas(pacienteId);

        model.addAttribute("pacienteId", pacienteId);
        model.addAttribute("dientes", odontograma);
        model.addAttribute("estadisticas", estadisticas);

        return "modulos/odontograma/vista";
    }
}
```

### 4. API REST Mejorada

```java
package com.odontoapp.controlador;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odontograma")
public class OdontogramaRestController {

    private final OdontogramaDienteService odontogramaService;

    public OdontogramaRestController(OdontogramaDienteService odontogramaService) {
        this.odontogramaService = odontogramaService;
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<List<OdontogramaDienteDTO>> obtenerOdontograma(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontogramaService.obtenerOdontogramaCompleto(pacienteId));
    }

    @PostMapping("/paciente/{pacienteId}/diente")
    @PreAuthorize("hasRole('ODONTOLOGO')")
    public ResponseEntity<OdontogramaDienteDTO> actualizarDiente(
            @PathVariable Long pacienteId,
            @RequestBody OdontogramaDienteDTO dienteDTO) {

        var resultado = odontogramaService.actualizarEstadoDiente(
            pacienteId,
            dienteDTO.getNumeroDiente(),
            dienteDTO.getEstado(),
            dienteDTO.getSuperficiesAfectadas(),
            dienteDTO.getNotas(),
            null
        );
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/paciente/{pacienteId}/actualizar-multiple")
    @PreAuthorize("hasRole('ODONTOLOGO')")
    public ResponseEntity<List<OdontogramaDienteDTO>> actualizarMultiple(
            @PathVariable Long pacienteId,
            @RequestBody List<OdontogramaDienteDTO> cambios) {

        return ResponseEntity.ok(
            odontogramaService.actualizarMultiplesDientes(pacienteId, cambios)
        );
    }

    @GetMapping("/paciente/{pacienteId}/estadisticas")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<Map<String, Integer>> obtenerEstadisticas(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontogramaService.obtenerEstadisticas(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/diente/{numeroDiente}/historial")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> obtenerHistorialDiente(
            @PathVariable Long pacienteId,
            @PathVariable String numeroDiente) {
        return ResponseEntity.ok(
            odontogramaService.obtenerHistorialDiente(pacienteId, numeroDiente)
        );
    }
}
```

---

## 🎨 IMPLEMENTACIÓN FRONTEND {#frontend}

### 1. HTML - Vista del Odontograma

**Archivo:** `src/main/resources/templates/modulos/odontograma/vista.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/layout}">

<head>
    <title>Odontograma - Paciente</title>
    <style>
        /* Estilos para el odontograma */
        .odontograma-container {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 0 20px rgba(0,0,0,0.1);
        }

        .cuadrante {
            display: grid;
            grid-template-columns: repeat(8, 60px);
            gap: 10px;
            margin: 20px 0;
        }

        .diente {
            width: 60px;
            height: 80px;
            border: 2px solid #ddd;
            border-radius: 8px;
            cursor: pointer;
            position: relative;
            transition: all 0.3s;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            font-size: 12px;
        }

        .diente:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }

        .diente .numero {
            position: absolute;
            top: 5px;
            font-weight: bold;
            font-size: 10px;
        }

        .diente .icon {
            font-size: 28px;
            margin: 10px 0;
        }

        .diente .nombre {
            font-size: 8px;
            text-align: center;
            padding: 0 2px;
        }

        /* Estados de los dientes con colores */
        .diente[data-estado="SANO"] {
            background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
            color: white;
            border-color: #45a049;
        }

        .diente[data-estado="CARIES"] {
            background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%);
            color: white;
            border-color: #d32f2f;
        }

        .diente[data-estado="RESTAURADO"] {
            background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
            color: white;
            border-color: #1976D2;
        }

        .diente[data-estado="ENDODONCIA"] {
            background: linear-gradient(135deg, #9C27B0 0%, #7B1FA2 100%);
            color: white;
            border-color: #7B1FA2;
        }

        .diente[data-estado="CORONA"] {
            background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%);
            color: white;
            border-color: #F57C00;
        }

        .diente[data-estado="EXTRACCION"] {
            background: linear-gradient(135deg, #9E9E9E 0%, #616161 100%);
            color: white;
            border-color: #616161;
        }

        .diente[data-estado="IMPLANTE"] {
            background: linear-gradient(135deg, #00BCD4 0%, #0097A7 100%);
            color: white;
            border-color: #0097A7;
        }

        .diente[data-estado="AUSENTE"] {
            background: linear-gradient(135deg, #212121 0%, #000000 100%);
            color: white;
            border-color: #000000;
        }

        .diente[data-estado="FRACTURADO"] {
            background: linear-gradient(135deg, #FF5722 0%, #E64A19 100%);
            color: white;
            border-color: #E64A19;
        }

        .cuadrante-label {
            text-align: center;
            font-weight: bold;
            color: #666;
            margin: 10px 0;
        }

        .leyenda {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 10px;
            margin-top: 20px;
        }

        .leyenda-item {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .leyenda-color {
            width: 30px;
            height: 30px;
            border-radius: 5px;
            border: 2px solid #ddd;
        }
    </style>
</head>

<body>
<div layout:fragment="content">

    <section class="content-header">
        <div class="container-fluid">
            <div class="row mb-2">
                <div class="col-sm-6">
                    <h1><i class="fas fa-tooth mr-2"></i>Odontograma</h1>
                </div>
                <div class="col-sm-6">
                    <button class="btn btn-success float-right" onclick="guardarCambios()">
                        <i class="fas fa-save mr-2"></i>Guardar Cambios
                    </button>
                </div>
            </div>
        </div>
    </section>

    <section class="content">
        <div class="container-fluid">

            <!-- Estadísticas -->
            <div class="row">
                <div class="col-md-3">
                    <div class="small-box bg-success">
                        <div class="inner">
                            <h3 th:text="${estadisticas.sanos}">0</h3>
                            <p>Dientes Sanos</p>
                        </div>
                        <div class="icon">
                            <i class="fas fa-tooth"></i>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="small-box bg-danger">
                        <div class="inner">
                            <h3 th:text="${estadisticas.caries}">0</h3>
                            <p>Con Caries</p>
                        </div>
                        <div class="icon">
                            <i class="fas fa-exclamation-triangle"></i>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="small-box bg-info">
                        <div class="inner">
                            <h3 th:text="${estadisticas.restaurados}">0</h3>
                            <p>Restaurados</p>
                        </div>
                        <div class="icon">
                            <i class="fas fa-tools"></i>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="small-box bg-secondary">
                        <div class="inner">
                            <h3 th:text="${estadisticas.extraidos}">0</h3>
                            <p>Extraídos</p>
                        </div>
                        <div class="icon">
                            <i class="fas fa-times-circle"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Odontograma -->
            <div class="card">
                <div class="card-body odontograma-container">

                    <!-- Cuadrante Superior -->
                    <div class="row">
                        <!-- Cuadrante 1: Superior Derecho -->
                        <div class="col-md-6">
                            <div class="cuadrante-label">Cuadrante 1: Superior Derecho</div>
                            <div class="cuadrante" id="cuadrante-1">
                                <!-- Los dientes se generarán con JavaScript -->
                            </div>
                        </div>

                        <!-- Cuadrante 2: Superior Izquierdo -->
                        <div class="col-md-6">
                            <div class="cuadrante-label">Cuadrante 2: Superior Izquierdo</div>
                            <div class="cuadrante" id="cuadrante-2">
                                <!-- Los dientes se generarán con JavaScript -->
                            </div>
                        </div>
                    </div>

                    <hr class="my-4">

                    <!-- Cuadrante Inferior -->
                    <div class="row">
                        <!-- Cuadrante 4: Inferior Derecho -->
                        <div class="col-md-6">
                            <div class="cuadrante-label">Cuadrante 4: Inferior Derecho</div>
                            <div class="cuadrante" id="cuadrante-4">
                                <!-- Los dientes se generarán con JavaScript -->
                            </div>
                        </div>

                        <!-- Cuadrante 3: Inferior Izquierdo -->
                        <div class="col-md-6">
                            <div class="cuadrante-label">Cuadrante 3: Inferior Izquierdo</div>
                            <div class="cuadrante" id="cuadrante-3">
                                <!-- Los dientes se generarán con JavaScript -->
                            </div>
                        </div>
                    </div>

                    <!-- Leyenda -->
                    <div class="mt-4">
                        <h5>Leyenda de Estados</h5>
                        <div class="leyenda">
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #4CAF50;"></div>
                                <span>Sano</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #f44336;"></div>
                                <span>Caries</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #2196F3;"></div>
                                <span>Restaurado</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #9C27B0;"></div>
                                <span>Endodoncia</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #FF9800;"></div>
                                <span>Corona</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #9E9E9E;"></div>
                                <span>Extraído</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #00BCD4;"></div>
                                <span>Implante</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #212121;"></div>
                                <span>Ausente</span>
                            </div>
                            <div class="leyenda-item">
                                <div class="leyenda-color" style="background: #FF5722;"></div>
                                <span>Fracturado</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Modal para editar diente -->
    <div class="modal fade" id="modalEditarDiente" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title">
                        <i class="fas fa-tooth mr-2"></i>Editar Diente <span id="dienteNumero"></span>
                    </h5>
                    <button type="button" class="close text-white" data-dismiss="modal">
                        <span>&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="dienteId">
                    <input type="hidden" id="dienteNumeroInput">

                    <div class="form-group">
                        <label>Estado del Diente</label>
                        <select class="form-control" id="dienteEstado">
                            <option value="SANO">🦷 Sano</option>
                            <option value="CARIES">🔴 Caries</option>
                            <option value="RESTAURADO">🔵 Restaurado/Obturado</option>
                            <option value="ENDODONCIA">🟣 Endodoncia</option>
                            <option value="CORONA">🟠 Corona</option>
                            <option value="EXTRACCION">⚫ Extraído</option>
                            <option value="IMPLANTE">🔷 Implante</option>
                            <option value="AUSENTE">⬛ Ausente</option>
                            <option value="FRACTURADO">🟧 Fracturado</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Superficies Afectadas</label>
                        <div class="btn-group-toggle" data-toggle="buttons">
                            <label class="btn btn-outline-primary btn-sm">
                                <input type="checkbox" value="Oclusal"> Oclusal
                            </label>
                            <label class="btn btn-outline-primary btn-sm">
                                <input type="checkbox" value="Vestibular"> Vestibular
                            </label>
                            <label class="btn btn-outline-primary btn-sm">
                                <input type="checkbox" value="Lingual"> Lingual
                            </label>
                            <label class="btn btn-outline-primary btn-sm">
                                <input type="checkbox" value="Mesial"> Mesial
                            </label>
                            <label class="btn btn-outline-primary btn-sm">
                                <input type="checkbox" value="Distal"> Distal
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Notas</label>
                        <textarea class="form-control" id="dienteNotas" rows="3"
                                  placeholder="Observaciones adicionales..."></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-primary" onclick="guardarDiente()">
                        <i class="fas fa-save mr-2"></i>Guardar
                    </button>
                </div>
            </div>
        </div>
    </div>

</div>

<th:block layout:fragment="scripts">
    <script th:inline="javascript">
        /*<![CDATA[*/
        const pacienteId = /*[[${pacienteId}]]*/ 0;
        const dientesData = /*[[${dientes}]]*/ [];

        const NOMBRES_DIENTES = {
            '1': 'Incisivo Central',
            '2': 'Incisivo Lateral',
            '3': 'Canino',
            '4': '1er Premolar',
            '5': '2do Premolar',
            '6': '1er Molar',
            '7': '2do Molar',
            '8': '3er Molar'
        };

        // Renderizar odontograma al cargar la página
        $(document).ready(function() {
            renderizarOdontograma();
        });

        function renderizarOdontograma() {
            // Renderizar cada cuadrante
            for (let cuadrante = 1; cuadrante <= 4; cuadrante++) {
                const container = document.getElementById(`cuadrante-${cuadrante}`);
                container.innerHTML = '';

                for (let posicion = 1; posicion <= 8; posicion++) {
                    const numeroDiente = `${cuadrante}${posicion}`;
                    const diente = dientesData.find(d => d.numeroDiente === numeroDiente) || {
                        numeroDiente: numeroDiente,
                        estado: 'SANO',
                        notas: '',
                        superficiesAfectadas: ''
                    };

                    const dienteElement = crearElementoDiente(diente);
                    container.appendChild(dienteElement);
                }
            }
        }

        function crearElementoDiente(diente) {
            const div = document.createElement('div');
            div.className = 'diente';
            div.setAttribute('data-estado', diente.estado);
            div.setAttribute('data-numero', diente.numeroDiente);
            div.onclick = () => abrirModalEdicion(diente);

            const numero = document.createElement('div');
            numero.className = 'numero';
            numero.textContent = diente.numeroDiente;

            const icon = document.createElement('div');
            icon.className = 'icon';
            icon.innerHTML = '<i class="fas fa-tooth"></i>';

            const nombre = document.createElement('div');
            nombre.className = 'nombre';
            nombre.textContent = NOMBRES_DIENTES[diente.numeroDiente.charAt(1)] || '';

            div.appendChild(numero);
            div.appendChild(icon);
            div.appendChild(nombre);

            return div;
        }

        function abrirModalEdicion(diente) {
            $('#dienteNumero').text(diente.numeroDiente);
            $('#dienteId').val(diente.id || '');
            $('#dienteNumeroInput').val(diente.numeroDiente);
            $('#dienteEstado').val(diente.estado);
            $('#dienteNotas').val(diente.notas || '');

            // Marcar superficies afectadas
            $('.btn-group-toggle input').prop('checked', false);
            if (diente.superficiesAfectadas) {
                diente.superficiesAfectadas.split(',').forEach(sup => {
                    $(`.btn-group-toggle input[value="${sup.trim()}"]`).prop('checked', true)
                        .parent().addClass('active');
                });
            }

            $('#modalEditarDiente').modal('show');
        }

        function guardarDiente() {
            const dienteDTO = {
                numeroDiente: $('#dienteNumeroInput').val(),
                estado: $('#dienteEstado').val(),
                notas: $('#dienteNotas').val(),
                superficiesAfectadas: $('.btn-group-toggle input:checked').map(function() {
                    return $(this).val();
                }).get().join(',')
            };

            $.ajax({
                url: `/api/odontograma/paciente/${pacienteId}/diente`,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(dienteDTO),
                success: function(response) {
                    Swal.fire('Éxito', 'Diente actualizado correctamente', 'success');
                    $('#modalEditarDiente').modal('hide');

                    // Actualizar el diente en el array
                    const index = dientesData.findIndex(d => d.numeroDiente === dienteDTO.numeroDiente);
                    if (index >= 0) {
                        dientesData[index] = response;
                    } else {
                        dientesData.push(response);
                    }

                    // Re-renderizar
                    renderizarOdontograma();
                },
                error: function(xhr) {
                    Swal.fire('Error', 'No se pudo actualizar el diente', 'error');
                }
            });
        }

        function guardarCambios() {
            Swal.fire({
                title: '¿Guardar todos los cambios?',
                text: 'Se actualizará el odontograma del paciente',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sí, guardar',
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    Swal.fire('Guardado', 'Cambios guardados exitosamente', 'success');
                }
            });
        }
        /*]]>*/
    </script>
</th:block>

</body>
</html>
```

### 2. Integrar en Historial del Paciente

**Actualizar:** `src/main/resources/templates/modulos/pacientes/historial.html`

Reemplazar líneas 344-356 con:

```html
<!-- Tab Odontograma -->
<div class="tab-pane fade" id="odontograma" role="tabpanel" aria-labelledby="odontograma-tab">
    <div class="text-center">
        <a th:href="@{/odontograma/paciente/{id}(id=${paciente.id})}"
           class="btn btn-primary btn-lg">
            <i class="fas fa-tooth mr-2"></i>Ver Odontograma Completo
        </a>
    </div>

    <!-- O puedes incluir un iframe -->
    <iframe th:src="@{/odontograma/paciente/{id}(id=${paciente.id})}"
            style="width: 100%; height: 800px; border: none;"
            class="mt-3"></iframe>
</div>
```

---

## 🔗 INTEGRACIONES {#integraciones}

### 1. Integración con TratamientoRealizado

Modifica `TratamientoController.java` para actualizar el odontograma automáticamente:

```java
@Autowired
private OdontogramaDienteService odontogramaService;

// En el método realizar-inmediato, después de guardar:
tratamientoRealizadoRepository.save(tratamiento);

// ACTUALIZAR ODONTOGRAMA AUTOMÁTICAMENTE
try {
    odontogramaService.actualizarDesderatamiento(tratamiento.getId());
} catch (Exception e) {
    // Log error pero no fallar el tratamiento
    System.err.println("Error al actualizar odontograma: " + e.getMessage());
}
```

### 2. Vincular con Comprobantes

Al generar un comprobante de tratamiento, mostrar enlace al odontograma:

```html
<a th:href="@{/odontograma/paciente/{id}(id=${paciente.id})}"
   class="btn btn-sm btn-info">
    <i class="fas fa-tooth mr-1"></i>Ver Odontograma
</a>
```

---

## 📅 ROADMAP DE IMPLEMENTACIÓN {#roadmap}

### FASE 1: Backend Core (1-2 días)
- [ ] Crear `OdontogramaDienteDTO`
- [ ] Crear `OdontogramaDienteService` e implementación
- [ ] Crear `OdontogramaRestController`
- [ ] Probar endpoints con Postman

### FASE 2: Frontend Básico (2-3 días)
- [ ] Crear vista HTML del odontograma
- [ ] Implementar renderizado con JavaScript
- [ ] Implementar modal de edición
- [ ] Integrar con API REST

### FASE 3: Integraciones (1-2 días)
- [ ] Integrar con `TratamientoRealizado`
- [ ] Agregar enlace en historial del paciente
- [ ] Agregar historial de cambios

### FASE 4: Mejoras (Opcional)
- [ ] Exportar odontograma a PDF
- [ ] Comparar odontogramas entre fechas
- [ ] Reportes de salud dental por paciente
- [ ] Alertas de caries detectadas

---

## 📝 NOTAS IMPORTANTES

1. **Sistema de numeración FDI** es el estándar internacional
2. **Colores** deben ser intuitivos (verde=sano, rojo=caries)
3. **Historial** es crítico para auditoría
4. **Seguridad**: Solo odontólogos pueden editar
5. **Performance**: Cargar solo el odontograma del paciente actual

---

## 🎯 RESULTADO ESPERADO

```
Usuario: Odontólogo
Acción: Ver historial de paciente Juan Pérez
1. Hace clic en tab "Odontograma"
2. Ve gráfico visual con 32 dientes
3. Dientes con caries están en rojo
4. Hace clic en diente "16" (primer molar superior derecho)
5. Abre modal para cambiar estado a "RESTAURADO"
6. Guarda cambio
7. Sistema registra en historial automáticamente
```

---

¿Quieres que implemente alguna parte específica primero? Te recomiendo empezar con la FASE 1 (Backend Core).
