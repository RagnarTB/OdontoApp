/**
 * Sistema global de loading para botones
 * Previene clicks múltiples y muestra estado de carga
 */

// Clase para manejar estado de loading en botones
class ButtonLoader {
    constructor(button, options = {}) {
        this.$button = $(button);
        this.options = {
            loadingText: options.loadingText || 'Procesando...',
            loadingIcon: options.loadingIcon || 'fa-spinner fa-spin',
            disabled: options.disabled !== false, // Por defecto true
            ...options
        };

        this.originalHtml = this.$button.html();
        this.originalDisabled = this.$button.prop('disabled');
    }

    start() {
        // Guardar HTML original si no está guardado
        if (!this.$button.data('original-html')) {
            this.$button.data('original-html', this.originalHtml);
            this.$button.data('original-disabled', this.originalDisabled);
        }

        // Ocultar contenido original y mostrar loading
        const icon = `<i class="fas ${this.options.loadingIcon} mr-1"></i>`;
        this.$button.html(`${icon}${this.options.loadingText}`);

        if (this.options.disabled) {
            this.$button.prop('disabled', true);
        }

        // Agregar clase de loading
        this.$button.addClass('btn-loading');
    }

    stop() {
        // Restaurar contenido original
        const originalHtml = this.$button.data('original-html') || this.originalHtml;
        const originalDisabled = this.$button.data('original-disabled') || this.originalDisabled;

        this.$button.html(originalHtml);
        this.$button.prop('disabled', originalDisabled);

        // Remover clase de loading
        this.$button.removeClass('btn-loading');
    }

    reset() {
        this.stop();
        this.$button.removeData('original-html');
        this.$button.removeData('original-disabled');
    }
}

// Función global para agregar loading a botón
window.setButtonLoading = function(buttonSelector, loading, options = {}) {
    const $button = $(buttonSelector);

    if (loading) {
        const loader = new ButtonLoader($button, options);
        $button.data('button-loader', loader);
        loader.start();
    } else {
        const loader = $button.data('button-loader');
        if (loader) {
            loader.stop();
        }
    }
};

// Auto-loading para formularios
$(document).ready(function() {
    // Auto-loading en formularios con submit
    $('form[data-auto-loading="true"]').on('submit', function(e) {
        const $form = $(this);
        const $submitButton = $form.find('button[type="submit"], input[type="submit"]');

        // Solo aplicar si el formulario es válido
        if (this.checkValidity && !this.checkValidity()) {
            return;
        }

        // Aplicar loading
        setButtonLoading($submitButton, true, {
            loadingText: $submitButton.data('loading-text') || 'Guardando...'
        });
    });

    // Auto-loading en botones AJAX con data attribute
    $(document).on('click', '[data-ajax-action]', function(e) {
        const $btn = $(this);

        // Si ya está en loading, prevenir click
        if ($btn.hasClass('btn-loading')) {
            e.preventDefault();
            return false;
        }

        const loadingText = $btn.data('loading-text') || 'Procesando...';
        setButtonLoading($btn, true, { loadingText });
    });

    // Restaurar botones cuando se recarga la página
    $(window).on('beforeunload', function() {
        $('.btn-loading').each(function() {
            const loader = $(this).data('button-loader');
            if (loader) {
                loader.stop();
            }
        });
    });
});

// Helper para crear botón con loading integrado
window.createLoadingButton = function(config) {
    const {
        text,
        icon = 'fa-save',
        btnClass = 'btn-primary',
        loadingText = 'Procesando...',
        type = 'submit',
        id = ''
    } = config;

    return `
        <button type="${type}"
                class="btn ${btnClass}"
                ${id ? `id="${id}"` : ''}
                data-loading-text="${loadingText}">
            <i class="fas ${icon} mr-1"></i>
            <span>${text}</span>
        </button>
    `;
};

// Exportar para uso global
window.ButtonLoader = ButtonLoader;
