// Validación de fechas en formulario de reportes
document.addEventListener('DOMContentLoaded', function() {
    const formReportes = document.querySelector('form[action*="/reportes"]');
    const fechaInicio = document.getElementById('fechaInicio');
    const fechaFin = document.getElementById('fechaFin');
    
    if (!formReportes || !fechaInicio || !fechaFin) return;
    
    // Establecer fecha máxima como hoy
    const hoy = new Date().toISOString().split('T')[0];
    fechaInicio.setAttribute('max', hoy);
    fechaFin.setAttribute('max', hoy);
    
    // Validar al enviar el formulario
    formReportes.addEventListener('submit', function(e) {
        const inicio = fechaInicio.value;
        const fin = fechaFin.value;
        
        // Validar que ambas fechas estén presentes si una está presente
        if ((inicio && !fin) || (!inicio && fin)) {
            e.preventDefault();
            if (typeof Swal !== 'undefined') {
                Swal.fire('Advertencia', 'Debe seleccionar ambas fechas (Desde y Hasta) o ninguna.', 'warning');
            } else {
                alert('Debe seleccionar ambas fechas (Desde y Hasta) o ninguna.');
            }
            return false;
        }
        
        // Validar que fechaInicio <= fechaFin
        if (inicio && fin && inicio > fin) {
            e.preventDefault();
            if (typeof Swal !== 'undefined') {
                Swal.fire('Error', 'La fecha "Desde" no puede ser posterior a la fecha "Hasta".', 'error');
            } else {
                alert('La fecha "Desde" no puede ser posterior a la fecha "Hasta".');
            }
            return false;
        }
        
        // Validar que no sean fechas futuras
        if (inicio && inicio > hoy) {
            e.preventDefault();
            if (typeof Swal !== 'undefined') {
                Swal.fire('Error', 'No se pueden generar reportes de fechas futuras.', 'error');
            } else {
                alert('No se pueden generar reportes de fechas futuras.');
            }
            return false;
        }
        
        if (fin && fin > hoy) {
            e.preventDefault();
            if (typeof Swal !== 'undefined') {
                Swal.fire('Error', 'No se pueden generar reportes de fechas futuras.', 'error');
            } else {
                alert('No se pueden generar reportes de fechas futuras.');
            }
            return false;
        }
        
        return true;
    });
});
