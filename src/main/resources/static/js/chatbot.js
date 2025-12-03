/**
 * CHATBOT WIDGET - OdontoApp
 * Asistente virtual con Google Gemini
 */

class ChatbotWidget {
    constructor() {
        this.isOpen = false;
        this.isTyping = false;
        this.messagesContainer = null;
        this.inputField = null;
        this.sendButton = null;

        this.init();
    }

    init() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.setup());
        } else {
            this.setup();
        }
    }

    setup() {
        this.createWidget();
        this.attachEventListeners();
        this.showWelcomeMessage();
    }

    createWidget() {
        const widgetHTML = `
            <div class="chatbot-widget">
                <button class="chatbot-button" id="chatbot-toggle" title="Abrir asistente virtual">
                    <i class="fas fa-comments"></i>
                </button>
                <div class="chatbot-window" id="chatbot-window">
                    <div class="chatbot-header">
                        <div>
                            <h3>Asistente Virtual</h3>
                            <small>OdontoApp</small>
                        </div>
                        <button class="chatbot-close" id="chatbot-close" title="Cerrar">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                    <div class="chatbot-messages" id="chatbot-messages"></div>
                    <div class="chatbot-input-area">
                        <input type="text" class="chatbot-input" id="chatbot-input" placeholder="Escribe tu mensaje..." maxlength="500" autocomplete="off">
                        <button class="chatbot-send" id="chatbot-send" title="Enviar mensaje">
                            <i class="fas fa-paper-plane"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', widgetHTML);
        this.messagesContainer = document.getElementById('chatbot-messages');
        this.inputField = document.getElementById('chatbot-input');
        this.sendButton = document.getElementById('chatbot-send');
    }

    attachEventListeners() {
        document.getElementById('chatbot-toggle').addEventListener('click', () => this.toggleChat());
        document.getElementById('chatbot-close').addEventListener('click', () => this.closeChat());
        this.sendButton.addEventListener('click', () => this.sendMessage());
        this.inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
    }

    toggleChat() {
        this.isOpen = !this.isOpen;
        const window = document.getElementById('chatbot-window');
        if (this.isOpen) {
            window.classList.add('active');
            this.inputField.focus();
        } else {
            window.classList.remove('active');
        }
    }

    closeChat() {
        this.isOpen = false;
        document.getElementById('chatbot-window').classList.remove('active');
    }

    showWelcomeMessage() {
        const welcomeMsg = `¡Hola! 👋 Soy tu asistente virtual de OdontoApp.\n\nPuedo ayudarte con:\n• Información sobre tus citas\n• Consultar tus tratamientos\n• Ver tus comprobantes\n\n¿En qué puedo ayudarte hoy?`;
        this.addMessage(welcomeMsg, 'bot');
    }

    async sendMessage() {
        const message = this.inputField.value.trim();
        if (!message || this.isTyping) return;

        this.addMessage(message, 'user');
        this.inputField.value = '';
        this.showTypingIndicator();

        // --- CORRECCIÓN CSRF AQUÍ ---
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

        const headers = {
            'Content-Type': 'application/json'
        };

        // Si los meta tags existen, agregamos el token al header
        if (csrfTokenMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.content] = csrfTokenMeta.content;
        }
        // -----------------------------

        try {
            const response = await fetch('/api/chatbot/mensaje', {
                method: 'POST',
                headers: headers, // Usamos los headers con el token
                body: JSON.stringify({ mensaje: message })
            });

            // Si la sesión expiró, el servidor redirige al login (HTML)
            // Detectamos si la respuesta fue redireccionada
            if (response.redirected) {
                window.location.href = '/login'; // Forzar recarga al login
                return;
            }

            // Si la respuesta no es OK (ej. 403 Forbidden o 500)
            if (!response.ok) {
                throw new Error(`Error HTTP: ${response.status}`);
            }

            const data = await response.json();
            this.hideTypingIndicator();

            if (data.exito) {
                this.addMessage(data.respuesta, 'bot');
            } else {
                this.addMessage('Lo siento, no pude procesar tu mensaje.', 'bot');
            }

        } catch (error) {
            console.error('Error al enviar mensaje:', error);
            this.hideTypingIndicator();
            this.addMessage('Error de conexión o sesión expirada. Por favor recarga la página.', 'bot');
        }
    }

    addMessage(text, sender) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${sender}`;
        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        bubble.style.whiteSpace = 'pre-wrap';
        bubble.textContent = text;
        const time = document.createElement('div');
        time.className = 'message-time';
        time.textContent = this.getCurrentTime();
        messageDiv.appendChild(bubble);
        messageDiv.appendChild(time);
        this.messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();
    }

    showTypingIndicator() {
        this.isTyping = true;
        this.sendButton.disabled = true;
        const indicator = document.createElement('div');
        indicator.className = 'chat-message bot';
        indicator.id = 'typing-indicator';
        indicator.innerHTML = `<div class="typing-indicator"><div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div></div>`;
        this.messagesContainer.appendChild(indicator);
        this.scrollToBottom();
    }

    hideTypingIndicator() {
        this.isTyping = false;
        this.sendButton.disabled = false;
        const indicator = document.getElementById('typing-indicator');
        if (indicator) indicator.remove();
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    getCurrentTime() {
        return new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    }
}

const chatbot = new ChatbotWidget();