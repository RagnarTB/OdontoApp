/**
 * horarios-citas.js
 * Manejo de selección de horarios disponibles para agendar citas
 */

(function () {
    'use strict';

    let horarioSeleccionado = null;
    let fechaSeleccionada = null;
    let odontologoSeleccionado = null;
    let duracionProcedimiento = 30; // Por defecto 30 minutos

    /**
     * Inicializa el sistema de horarios al abrir el modal
     */
    function inicializarSistemaHorarios() {
        // Verificar si ya hay una fecha pre-cargada (viene del calendario)
        const fechaActual = $('#fechaCitaAgendar').val();
        const fechaHoraActual = $('#fechaHoraInicioAgendar').val();
        // Limpiar estado anterior
        horarioSeleccionado = null;
        fechaSeleccionada = null;
        odontologoSeleccionado = null;
        $('#grilla-horarios-disponibles').html('');
        $('#horario-seleccionado-texto').text('Ninguno');
        // NOTA: NO inicializamos Select2 para pacienteIdAgendar porque ese campo
        // NO existe en el panel de paciente (solo en admin panel)

        // Inicializar Select2 solo para odontólogo
        if (!$('#odontologoIdAgendar').hasClass('select2-hidden-accessible')) {
            $('#odontologoIdAgendar').select2({
                placeholder: 'Seleccione un odontólogo',
                allowClear: true,
                dropdownParent: $('#modalAgendarCita')
            });
        }
        // Configurar fecha mínima (hoy)
        const hoy = new Date().toISOString().split('T')[0];
        $('#fechaCitaAgendar').attr('min', hoy);
        // Si NO hay fecha pre-cargada desde el calendario, usar hoy por defecto
        if (!fechaActual) {
            $('#fechaCitaAgendar').val(hoy);
        }
        // Si NO hay fecha/hora pre-cargada, limpiar el campo
        if (!fechaHoraActual) {
            $('#fechaHoraInicioAgendar').val('');
        }
    }

    /**
     * Carga los horarios disponibles desde el servidor
     */
    function cargarHorariosDisponibles() {
        const fecha = $('#fechaCitaAgendar').val();
        const odontologoId = $('#odontologoIdAgendar').val();
        const procedimientoId = $('#procedimientoIdAgendar').val();

        // Validar que tengamos odontólogo y fecha
        if (!odontologoId || !fecha) {
            $('#grilla-horarios-disponibles').html(
                '<div class="alert alert-warning">' +
                '<i class="fas fa-info-circle mr-2"></i>' +
                'Por favor seleccione un odontólogo y una fecha' +
                '</div>'
            );
            return;
        }

        // Obtener duración del procedimiento si hay uno seleccionado
        if (procedimientoId) {
            const procedimientoOption = $(`#procedimientoIdAgendar option[value="${procedimientoId}"]`);
            const duracionData = procedimientoOption.data('duracion');
            if (duracionData) {
                duracionProcedimiento = parseInt(duracionData);
            }
        }

        // Mostrar loading
        $('#grilla-horarios-disponibles').html(
            '<div class="text-center py-4">' +
            '<i class="fas fa-spinner fa-spin fa-2x text-primary"></i>' +
            '<p class="mt-2">Cargando horarios disponibles...</p>' +
            '</div>'
        );

        // Hacer petición al servidor
        $.ajax({
            url: '/citas/api/horarios-disponibles',
            method: 'GET',
            data: {
                odontologoId: odontologoId,
                fecha: fecha,
                duracion: duracionProcedimiento
            },
            success: function (response) {
                if (response.error) {
                    mostrarError(response.mensaje);
                    return;
                }

                mostrarGrillaHorarios(response);
            },
            error: function (xhr, status, error) {
                $('#grilla-horarios-disponibles').html(
                    '<div class="alert alert-danger">' +
                    '<i class="fas fa-exclamation-triangle mr-2"></i>' +
                    'Error al cargar horarios: ' + error +
                    '</div>'
                );
            }
        });
    }

    /**
     * Muestra la grilla de horarios disponibles
     */
    function mostrarGrillaHorarios(response) {
        fechaSeleccionada = response.fecha;
        odontologoSeleccionado = response.odontologoId;

        const contenedor = $('#grilla-horarios-disponibles');
        contenedor.empty();

        // Si no hay disponibilidad
        if (!response.disponible) {
            contenedor.html(
                '<div class="alert alert-warning">' +
                '<i class="fas fa-info-circle mr-2"></i>' +
                '<strong>No disponible:</strong> ' + response.motivo +
                '</div>'
            );
            return;
        }

        // Si no hay horarios disponibles
        if (!response.horariosDisponibles || response.horariosDisponibles.length === 0) {
            contenedor.html(
                '<div class="alert alert-info">' +
                '<i class="fas fa-info-circle mr-2"></i>' +
                'No hay horarios disponibles para esta fecha. Por favor seleccione otra fecha.' +
                '</div>'
            );
            return;
        }

        // Título con información
        let titulo = '<div class="mb-3">';
        titulo += '<h6 class="text-primary"><i class="fas fa-calendar-day mr-2"></i>Horarios Disponibles</h6>';
        titulo += '<small class="text-muted">';
        titulo += `Seleccione un horario para ${response.odontologoNombre}`;
        if (response.esExcepcion) {
            titulo += ' <span class="badge badge-info ml-2">Horario especial</span>';
        }
        titulo += '</small><br>';
        titulo += '<small class="text-info">';
        titulo += '<i class="fas fa-info-circle mr-1"></i>';
        titulo += `<strong>Duración:</strong> ${duracionProcedimiento} min | `;
        titulo += '<strong>Buffer:</strong> 15 min entre citas';
        titulo += '</small>';
        titulo += '</div>';
        contenedor.append(titulo);

        // Grilla de horarios
        const grilla = $('<div class="horarios-grid"></div>');

        response.horariosDisponibles.forEach(function (slot) {
            const boton = $('<button type="button" class="horario-slot"></button>');
            boton.addClass(slot.disponible ? 'disponible' : 'ocupado');
            boton.text(slot.inicio);
            boton.attr('data-inicio', slot.inicio);
            boton.attr('data-fin', slot.fin);

            if (slot.disponible) {
                boton.on('click', function () {
                    seleccionarHorario(slot.inicio, slot.fin);
                });
                boton.append('<span class="duracion-texto">' + duracionProcedimiento + 'min</span>');
            } else {
                boton.attr('disabled', true);
                boton.attr('title', 'Horario ocupado');
            }

            grilla.append(boton);
        });

        contenedor.append(grilla);

        // Leyenda
        const leyenda = $('<div class="leyenda-horarios mt-3"></div>');
        leyenda.html(
            '<small class="mr-3"><span class="badge badge-success">Verde</span> Disponible</small>' +
            '<small class="mr-3"><span class="badge badge-secondary">Gris</span> Ocupado</small>' +
            '<small><i class="fas fa-info-circle text-primary"></i> Mínimo 1h de anticipación</small>'
        );
        contenedor.append(leyenda);
    }

    /**
     * Maneja la selección de un horario
     */
    function seleccionarHorario(inicio, fin) {
        horarioSeleccionado = inicio;

        // Construir fecha y hora completa
        const fechaHoraCompleta = fechaSeleccionada + 'T' + inicio;
        $('#fechaHoraInicioAgendar').val(fechaHoraCompleta);

        // Actualizar UI
        $('.horario-slot').removeClass('seleccionado');
        $(`.horario-slot[data-inicio="${inicio}"]`).addClass('seleccionado');

        $('#horario-seleccionado-texto').html(
            `<strong>${inicio}</strong> - ${fin} <i class="fas fa-check text-success ml-2"></i>`
        );

        // Mostrar mensaje de confirmación
        mostrarInfo(`Horario seleccionado: ${inicio} - ${fin}`);
    }

    /**
     * Validar antes de enviar el formulario
     */
    function validarFormularioCita() {
        if (!horarioSeleccionado) {
            mostrarAdvertencia('Por favor seleccione un horario disponible');
            return false;
        }
        if (!$('#odontologoIdAgendar').val()) {
            mostrarAdvertencia('Por favor seleccione un odontólogo');
            return false;
        }
        // NOTA: NO validamos pacienteIdAgendar porque en el panel de paciente
        // el paciente se establece automáticamente en el backend por seguridad
        // Solo el admin panel requiere seleccionar paciente manualmente
        return true;
    }

    // =====================================================
    // WIZARD DE NAVEGACIÓN
    // =====================================================
    let pasoActual = 1;
    const totalPasos = 3;

    function cambiarPaso(nuevoPaso) {
        if (nuevoPaso < 1 || nuevoPaso > totalPasos) return;

        // Ocultar paso actual
        $('#wizard-step-' + pasoActual).removeClass('active');
        $('.wizard-step[data-step="' + pasoActual + '"]').removeClass('active').addClass('completed');

        // Mostrar nuevo paso
        pasoActual = nuevoPaso;
        $('#wizard-step-' + pasoActual).addClass('active');
        $('.wizard-step[data-step="' + pasoActual + '"]').addClass('active').removeClass('completed');

        // Actualizar botones
        if (pasoActual === 1) {
            $('#btnWizardAnterior').hide();
            $('#btnWizardSiguiente').show();
            $('#btnWizardFinalizar').hide();
        } else if (pasoActual === totalPasos) {
            $('#btnWizardAnterior').show();
            $('#btnWizardSiguiente').hide();
            $('#btnWizardFinalizar').show();
        } else {
            $('#btnWizardAnterior').show();
            $('#btnWizardSiguiente').show();
            $('#btnWizardFinalizar').hide();
        }

        // Scroll al inicio del modal
        $('.modal-body').scrollTop(0);
    }

    function validarPasoActual() {
        if (pasoActual === 1) {
            // VALIDAR PACIENTE: Solo en admin panel (si el campo existe)
            const campoPaciente = $('#pacienteIdAgendar');
            if (campoPaciente.length > 0 && !campoPaciente.val()) {
                mostrarAdvertencia('Por favor seleccione un paciente');
                return false;
            }
            // Validar odontólogo
            if (!$('#odontologoIdAgendar').val()) {
                mostrarAdvertencia('Por favor seleccione un odontólogo');
                return false;
            }

            // Validar fecha
            if (!$('#fechaCitaAgendar').val()) {
                mostrarAdvertencia('Por favor seleccione una fecha');
                return false;
            }
        } else if (pasoActual === 2) {
            // Validar horario seleccionado
            if (!$('#fechaHoraInicioAgendar').val()) {
                mostrarAdvertencia('Por favor seleccione un horario disponible');
                return false;
            }
        }
        return true;
    }

    // Inicializar cuando el DOM esté listo
    $(document).ready(function () {
        // Inicializar al abrir el modal
        $('#modalAgendarCita').on('show.bs.modal', function () {
            inicializarSistemaHorarios();
            // Resetear wizard
            pasoActual = 1;
            $('.wizard-content-step').removeClass('active');
            $('#wizard-step-1').addClass('active');
            $('.wizard-step').removeClass('active completed');
            $('.wizard-step[data-step="1"]').addClass('active');
            $('#btnWizardAnterior').hide();
            $('#btnWizardSiguiente').show();
            $('#btnWizardFinalizar').hide();
        });

        // Navegación del wizard
        $('#btnWizardSiguiente').on('click', function () {
            if (validarPasoActual()) {
                cambiarPaso(pasoActual + 1);
            }
        });

        $('#btnWizardAnterior').on('click', function () {
            cambiarPaso(pasoActual - 1);
        });

        // Cargar horarios cuando cambie la fecha u odontólogo
        $('#fechaCitaAgendar, #odontologoIdAgendar, #procedimientoIdAgendar').on('change', function () {
            cargarHorariosDisponibles();
        });

        // Botón manual de refrescar horarios (útil cuando se edita el horario del odontólogo)
        $('#btnRefrescarHorarios').on('click', function () {
            // Animar el ícono de refrescar
            $(this).find('i').addClass('fa-spin');
            setTimeout(() => {
                $(this).find('i').removeClass('fa-spin');
            }, 1000);

            // Recargar horarios disponibles
            cargarHorariosDisponibles();
        });

        // Validar antes de enviar
        $('#modalAgendarCita form').on('submit', function (e) {
            if (!validarFormularioCita()) {
                e.preventDefault();
                return false;
            }
        });
    });

    // =====================================================
    // MANEJO DE REPROGRAMACIÓN DE CITAS
    // =====================================================

    let horarioSeleccionadoReprogramar = null;
    let odontologoIdReprogramar = null;
    let duracionReprogramar = 30;
    let fechaHoraOriginalReprogramar = null; // Guardar la fecha/hora original de la cita

    /**
     * Inicializa el modal de reprogramación cuando se abre
     */
    $('#btnReprogramar').on('click', function () {
        // Obtener datos de la cita del modal de detalle
        const citaId = $('#detalleCitaId').val();
        const paciente = $('#detallePaciente').text();
        const odontologo = $('#detalleOdontologo').text();
        const odontologoId = $('#detalleOdontologoId').val();
        const fechaHoraInicio = $('#detalleFechaHoraInicio').text();

        // Llenar campos del modal de reprogramación
        $('#reprogramarCitaId').val(citaId);
        $('#reprogramarPaciente').text(paciente);
        $('#reprogramarOdontologo').text(odontologo);
        $('#reprogramarFechaActual').text(fechaHoraInicio);
        $('#reprogramarOdontologoId').val(odontologoId);

        // Guardar odontólogo ID y fecha/hora original para validación
        odontologoIdReprogramar = odontologoId;
        fechaHoraOriginalReprogramar = fechaHoraInicio; // Formato: "DD/MM/YYYY HH:mm"

        // Limpiar estado anterior
        horarioSeleccionadoReprogramar = null;
        $('#grilla-horarios-disponibles-reprogramar').html(
            '<div class="alert alert-secondary text-center">' +
            '<i class="fas fa-info-circle mr-2"></i>' +
            'Seleccione una fecha para ver los horarios disponibles del odontólogo' +
            '</div>'
        );
        $('#horario-seleccionado-texto-reprogramar').text('Ninguno');
        $('#horario-seleccionado-info-reprogramar').hide();
        $('#nuevaFechaHoraInicioReprogramar').val('');

        // Configurar fecha mínima (hoy)
        const hoy = new Date().toISOString().split('T')[0];
        $('#fechaCitaReprogramar').attr('min', hoy);
        $('#fechaCitaReprogramar').val('');
    });

    /**
     * Cargar horarios cuando cambia la fecha en reprogramación
     */
    $('#fechaCitaReprogramar').on('change', function () {
        const fecha = $(this).val();
        if (!fecha || !odontologoIdReprogramar) {
            return;
        }

        // Mostrar loading
        $('#grilla-horarios-disponibles-reprogramar').html(
            '<div class="text-center py-4">' +
            '<i class="fas fa-spinner fa-spin fa-2x text-primary"></i>' +
            '<p class="mt-2">Cargando horarios disponibles...</p>' +
            '</div>'
        );

        // Hacer petición al servidor (excluir cita actual)
        const citaIdActual = $('#reprogramarCitaId').val();
        $.ajax({
            url: '/citas/api/horarios-disponibles',
            method: 'GET',
            data: {
                odontologoId: odontologoIdReprogramar,
                fecha: fecha,
                duracion: duracionReprogramar,
                citaIdExcluir: citaIdActual  // Excluir cita actual para que su horario aparezca disponible
            },
            success: function (response) {
                if (response.error) {
                    $('#grilla-horarios-disponibles-reprogramar').html(
                        '<div class="alert alert-danger">' +
                        '<i class="fas fa-exclamation-triangle mr-2"></i>' +
                        response.mensaje +
                        '</div>'
                    );
                    return;
                }

                mostrarGrillaHorariosReprogramar(response);
            },
            error: function (xhr, status, error) {
                $('#grilla-horarios-disponibles-reprogramar').html(
                    '<div class="alert alert-danger">' +
                    '<i class="fas fa-exclamation-triangle mr-2"></i>' +
                    'Error al cargar horarios' +
                    '</div>'
                );
            }
        });
    });

    /**
     * Muestra la grilla de horarios para reprogramación
     */
    function mostrarGrillaHorariosReprogramar(response) {
        const container = $('#grilla-horarios-disponibles-reprogramar');
        container.empty();

        if (!response.disponible) {
            container.html(
                '<div class="alert alert-warning">' +
                '<i class="fas fa-exclamation-triangle mr-2"></i>' +
                '<strong>No disponible:</strong> ' + (response.motivo || 'El odontólogo no trabaja en esta fecha') +
                '</div>'
            );
            return;
        }

        const horarios = response.horariosDisponibles || [];
        if (horarios.length === 0) {
            container.html(
                '<div class="alert alert-warning">' +
                '<i class="fas fa-info-circle mr-2"></i>' +
                'No hay horarios disponibles para esta fecha' +
                '</div>'
            );
            return;
        }

        // Crear grilla
        let html = '<div class="horarios-grid">';
        horarios.forEach(function (slot) {
            html += '<button type="button" class="btn btn-outline-primary btn-horario-slot-reprogramar" ' +
                'data-hora="' + slot.inicio + '">' +
                '<i class="far fa-clock mr-1"></i>' + slot.inicio +
                '</button>';
        });
        html += '</div>';

        container.html(html);

        // Agregar event listeners
        $('.btn-horario-slot-reprogramar').on('click', function () {
            const hora = $(this).data('hora');
            seleccionarHorarioReprogramar(hora);

            // Resaltar selección
            $('.btn-horario-slot-reprogramar').removeClass('active btn-primary').addClass('btn-outline-primary');
            $(this).removeClass('btn-outline-primary').addClass('btn-primary active');
        });
    }

    /**
     * Selecciona un horario para reprogramación
     */
    function seleccionarHorarioReprogramar(hora) {
        const fecha = $('#fechaCitaReprogramar').val();

        // Validar que no sea la misma fecha y hora original
        if (fechaHoraOriginalReprogramar) {
            // Parsear la fecha/hora original (formato: "DD/MM/YYYY HH:mm")
            const partes = fechaHoraOriginalReprogramar.split(' ');
            if (partes.length === 2) {
                const [dia, mes, anio] = partes[0].split('/');
                const fechaOriginal = `${anio}-${mes}-${dia}`; // Formato ISO
                const horaOriginal = partes[1]; // HH:mm

                // Comparar con la fecha y hora seleccionada
                if (fecha === fechaOriginal && hora === horaOriginal) {
                    mostrarAdvertencia('No puede reprogramar la cita en el mismo horario actual. Por favor seleccione un horario diferente.');
                    return;
                }
            }
        }

        horarioSeleccionadoReprogramar = hora;

        // Actualizar campos hidden y visuales
        const fechaHoraCompleta = fecha + 'T' + hora;
        $('#nuevaFechaHoraInicioReprogramar').val(fechaHoraCompleta);
        $('#horario-seleccionado-texto-reprogramar').text(hora + ' - ' + fecha);
        $('#horario-seleccionado-info-reprogramar').show();
    }

    /**
     * Validar antes de enviar reprogramación
     */
    $('#modalReprogramarCita form').on('submit', function (e) {
        if (!horarioSeleccionadoReprogramar) {
            e.preventDefault();
            mostrarError('Por favor seleccione un horario para la reprogramación');
            return false;
        }

        // Bloquear el botón para evitar doble clic
        const btnReprogramar = $('#btnConfirmarReprogramar');
        if (btnReprogramar.prop('disabled')) {
            e.preventDefault();
            return false;
        }

        btnReprogramar.prop('disabled', true);
        btnReprogramar.html('<i class="fas fa-spinner fa-spin mr-1"></i>Reprogramando...');

        // Si hay algún error de validación del navegador, desbloquear el botón
        if (!this.checkValidity()) {
            btnReprogramar.prop('disabled', false);
            btnReprogramar.html('<i class="fas fa-check mr-1"></i>Reprogramar');
            return true; // Dejar que el navegador maneje la validación
        }
    });

})();
