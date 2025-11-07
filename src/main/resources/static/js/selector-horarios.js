/**
 * selector-horarios.js
 * Componente visual para seleccionar horarios de trabajo
 * Permite seleccionar bloques de 30 minutos arrastrando el mouse
 */

(function() {
    'use strict';

    const HORA_INICIO = 6;  // 6:00 AM
    const HORA_FIN = 22;    // 10:00 PM
    const INTERVALO_MINUTOS = 30;

    // Horarios predefinidos comunes en clínicas dentales
    const HORARIOS_PREDEFINIDOS = {
        'manana': { label: 'Mañana (8:00-13:00)', bloques: ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30'] },
        'tarde': { label: 'Tarde (15:00-20:00)', bloques: ['15:00', '15:30', '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30'] },
        'completo': { label: 'Jornada Completa (8:00-13:00, 15:00-20:00)', bloques: ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30'] },
        'intensivo': { label: 'Intensivo (8:00-16:00)', bloques: ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30'] }
    };

    const DIAS_SEMANA = [
        { key: 'MONDAY', nombre: 'Lunes' },
        { key: 'TUESDAY', nombre: 'Martes' },
        { key: 'WEDNESDAY', nombre: 'Miércoles' },
        { key: 'THURSDAY', nombre: 'Jueves' },
        { key: 'FRIDAY', nombre: 'Viernes' },
        { key: 'SATURDAY', nombre: 'Sábado' },
        { key: 'SUNDAY', nombre: 'Domingo' }
    ];

    /**
     * Genera todos los bloques de tiempo disponibles
     */
    function generarBloques() {
        const bloques = [];
        for (let hora = HORA_INICIO; hora < HORA_FIN; hora++) {
            for (let minuto = 0; minuto < 60; minuto += INTERVALO_MINUTOS) {
                const horaStr = String(hora).padStart(2, '0');
                const minutoStr = String(minuto).padStart(2, '0');
                bloques.push(`${horaStr}:${minutoStr}`);
            }
        }
        return bloques;
    }

    /**
     * Convierte bloques seleccionados a formato string "HH:mm-HH:mm,HH:mm-HH:mm"
     */
    function bloquesAString(bloques) {
        if (bloques.length === 0) return '';

        // Ordenar bloques
        bloques.sort();

        const rangos = [];
        let rangoActual = { inicio: bloques[0], fin: null };

        for (let i = 1; i < bloques.length; i++) {
            const bloqueActual = bloques[i];
            const bloqueAnterior = bloques[i - 1];

            // Calcular si es consecutivo (diferencia de 30 min)
            const tiempoActual = bloqueAMinutos(bloqueActual);
            const tiempoAnterior = bloqueAMinutos(bloqueAnterior);

            if (tiempoActual - tiempoAnterior === INTERVALO_MINUTOS) {
                // Es consecutivo, continuar rango
                rangoActual.fin = bloqueActual;
            } else {
                // No es consecutivo, cerrar rango y empezar uno nuevo
                if (!rangoActual.fin) {
                    rangoActual.fin = sumarMinutos(bloqueAnterior, INTERVALO_MINUTOS);
                } else {
                    rangoActual.fin = sumarMinutos(rangoActual.fin, INTERVALO_MINUTOS);
                }
                rangos.push(`${rangoActual.inicio}-${rangoActual.fin}`);
                rangoActual = { inicio: bloqueActual, fin: null };
            }
        }

        // Cerrar último rango
        if (!rangoActual.fin) {
            rangoActual.fin = sumarMinutos(bloques[bloques.length - 1], INTERVALO_MINUTOS);
        } else {
            rangoActual.fin = sumarMinutos(rangoActual.fin, INTERVALO_MINUTOS);
        }
        rangos.push(`${rangoActual.inicio}-${rangoActual.fin}`);

        return rangos.join(',');
    }

    /**
     * Convierte string formato "HH:mm-HH:mm,HH:mm-HH:mm" a array de bloques
     */
    function stringABloques(str) {
        if (!str || str.trim() === '') return [];

        const bloques = [];
        const rangos = str.split(',');

        rangos.forEach(rango => {
            const partes = rango.trim().split('-');
            if (partes.length !== 2) return;

            const inicio = partes[0].trim();
            const fin = partes[1].trim();

            let bloqueActual = inicio;
            const finMinutos = bloqueAMinutos(fin);

            while (bloqueAMinutos(bloqueActual) < finMinutos) {
                bloques.push(bloqueActual);
                bloqueActual = sumarMinutos(bloqueActual, INTERVALO_MINUTOS);
            }
        });

        return bloques;
    }

    /**
     * Convierte "HH:mm" a minutos desde medianoche
     */
    function bloqueAMinutos(bloque) {
        const partes = bloque.split(':');
        return parseInt(partes[0]) * 60 + parseInt(partes[1]);
    }

    /**
     * Suma minutos a un bloque "HH:mm"
     */
    function sumarMinutos(bloque, minutos) {
        const totalMinutos = bloqueAMinutos(bloque) + minutos;
        const horas = Math.floor(totalMinutos / 60);
        const mins = totalMinutos % 60;
        return `${String(horas).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
    }

    /**
     * Inicializa el selector de horarios para un día específico
     */
    function inicializarSelectorDia(dia) {
        const contenedor = $(`#selector-${dia.key}`);
        if (contenedor.length === 0) return;

        contenedor.empty();

        // Botones de horarios predefinidos
        const botonesPredefinidos = $('<div class="mb-2"></div>');
        Object.keys(HORARIOS_PREDEFINIDOS).forEach(key => {
            const config = HORARIOS_PREDEFINIDOS[key];
            const btn = $(`<button type="button" class="btn btn-sm btn-outline-primary mr-1 mb-1">${config.label}</button>`);
            btn.on('click', function() {
                aplicarHorarioPredefinido(dia.key, config.bloques);
            });
            botonesPredefinidos.append(btn);
        });

        const btnLimpiar = $('<button type="button" class="btn btn-sm btn-outline-danger mb-1">Limpiar</button>');
        btnLimpiar.on('click', function() {
            limpiarSeleccion(dia.key);
        });
        botonesPredefinidos.append(btnLimpiar);

        contenedor.append(botonesPredefinidos);

        // Grilla de bloques
        const grilla = $('<div class="bloques-horario-grid"></div>');
        const todosLosBloques = generarBloques();

        todosLosBloques.forEach(bloque => {
            const bloqueDiv = $(`<div class="bloque-horario" data-bloque="${bloque}">${bloque}</div>`);

            bloqueDiv.on('click', function() {
                toggleBloque(dia.key, bloque);
            });

            // Arrastre para selección múltiple
            bloqueDiv.on('mousedown', function() {
                window.seleccionandoBloques = true;
                window.estadoInicialBloque = !$(this).hasClass('seleccionado');
                toggleBloque(dia.key, bloque, window.estadoInicialBloque);
            });

            bloqueDiv.on('mouseenter', function() {
                if (window.seleccionandoBloques) {
                    toggleBloque(dia.key, bloque, window.estadoInicialBloque);
                }
            });

            grilla.append(bloqueDiv);
        });

        contenedor.append(grilla);

        // Cargar bloques existentes si hay
        const inputHorario = $(`input[name="horarioRegular[${dia.key}]"]`);
        const valorActual = inputHorario.val();
        if (valorActual && valorActual.trim() !== '') {
            const bloquesExistentes = stringABloques(valorActual);
            bloquesExistentes.forEach(bloque => {
                $(`#selector-${dia.key} .bloque-horario[data-bloque="${bloque}"]`).addClass('seleccionado');
            });
        }
    }

    /**
     * Toggle de un bloque específico
     */
    function toggleBloque(diaKey, bloque, forzarEstado) {
        const bloqueDiv = $(`#selector-${diaKey} .bloque-horario[data-bloque="${bloque}"]`);

        if (forzarEstado !== undefined) {
            bloqueDiv.toggleClass('seleccionado', forzarEstado);
        } else {
            bloqueDiv.toggleClass('seleccionado');
        }

        actualizarInput(diaKey);
    }

    /**
     * Aplica un horario predefinido
     */
    function aplicarHorarioPredefinido(diaKey, bloques) {
        // Limpiar selección actual
        $(`#selector-${diaKey} .bloque-horario`).removeClass('seleccionado');

        // Seleccionar bloques predefinidos
        bloques.forEach(bloque => {
            $(`#selector-${diaKey} .bloque-horario[data-bloque="${bloque}"]`).addClass('seleccionado');
        });

        actualizarInput(diaKey);
    }

    /**
     * Limpia la selección de un día
     */
    function limpiarSeleccion(diaKey) {
        $(`#selector-${diaKey} .bloque-horario`).removeClass('seleccionado');
        actualizarInput(diaKey);
    }

    /**
     * Actualiza el input hidden con el valor convertido
     */
    function actualizarInput(diaKey) {
        const bloquesSeleccionados = [];
        $(`#selector-${diaKey} .bloque-horario.seleccionado`).each(function() {
            bloquesSeleccionados.push($(this).data('bloque'));
        });

        const valorString = bloquesAString(bloquesSeleccionados);
        $(`input[name="horarioRegular[${diaKey}]"]`).val(valorString);

        // Mostrar vista previa
        const preview = valorString || '<em class="text-muted">Sin horario configurado</em>';
        $(`#preview-${diaKey}`).html(preview);
    }

    /**
     * Aplicar el mismo horario a todos los días de la semana
     */
    function aplicarATodosLosDias() {
        // Tomar el horario del lunes como referencia
        const horarioLunes = $('input[name="horarioRegular[MONDAY]"]').val();

        if (!horarioLunes || horarioLunes.trim() === '') {
            mostrarAdvertencia('Configure primero el horario del lunes');
            return;
        }

        DIAS_SEMANA.forEach(dia => {
            if (dia.key !== 'MONDAY') {
                $(`input[name="horarioRegular[${dia.key}]"]`).val(horarioLunes);

                // Actualizar visualmente
                $(`#selector-${dia.key} .bloque-horario`).removeClass('seleccionado');
                const bloques = stringABloques(horarioLunes);
                bloques.forEach(bloque => {
                    $(`#selector-${dia.key} .bloque-horario[data-bloque="${bloque}"]`).addClass('seleccionado');
                });

                $(`#preview-${dia.key}`).html(horarioLunes);
            }
        });

        mostrarExito('Horario del lunes aplicado a todos los días');
    }

    /**
     * Aplicar horario solo a días laborables (L-V)
     */
    function aplicarADiasLaborables() {
        const horarioLunes = $('input[name="horarioRegular[MONDAY]"]').val();

        if (!horarioLunes || horarioLunes.trim() === '') {
            mostrarAdvertencia('Configure primero el horario del lunes');
            return;
        }

        ['TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'].forEach(diaKey => {
            $(`input[name="horarioRegular[${diaKey}]"]`).val(horarioLunes);

            // Actualizar visualmente
            $(`#selector-${diaKey} .bloque-horario`).removeClass('seleccionado');
            const bloques = stringABloques(horarioLunes);
            bloques.forEach(bloque => {
                $(`#selector-${diaKey} .bloque-horario[data-bloque="${bloque}"]`).addClass('seleccionado');
            });

            $(`#preview-${diaKey}`).html(horarioLunes);
        });

        mostrarExito('Horario del lunes aplicado a días laborables (Ma-Vi)');
    }

    // Inicializar cuando el documento esté listo
    $(document).ready(function() {
        // Detener arrastre al soltar el mouse
        $(document).on('mouseup', function() {
            window.seleccionandoBloques = false;
        });

        // Inicializar selectores para cada día
        DIAS_SEMANA.forEach(dia => {
            inicializarSelectorDia(dia);
        });

        // Botones de acciones rápidas
        $('#btnAplicarTodos').on('click', aplicarATodosLosDias);
        $('#btnAplicarLaborables').on('click', aplicarADiasLaborables);
    });

    // Exponer funciones globalmente si es necesario
    window.SelectorHorarios = {
        inicializarSelectorDia,
        aplicarATodosLosDias,
        aplicarADiasLaborables
    };

})();
