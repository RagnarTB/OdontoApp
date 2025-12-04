# 🔐 Configuración de OdontoApp

## 📋 Configuración Inicial para Desarrolladores

Si eres un nuevo desarrollador en este proyecto, sigue estos pasos para configurar tu entorno local:

### 1. Configurar application.properties

El archivo `application.properties` contiene credenciales sensibles y **NO está en Git** por seguridad.

**Pasos:**

1. **Copia el archivo de ejemplo**:
   ```bash
   cd src/main/resources
   copy application.properties.example application.properties
   ```

2. **Edita `application.properties`** y reemplaza los placeholders con tus credenciales reales:

   ```properties
   # Base de datos
   spring.datasource.password=TU_PASSWORD_MYSQL_AQUI
   
   # API de Apidecolecta
   api.decolecta.token=TU_TOKEN_APIDECOLECTA_AQUI
   
   # Email (Gmail)
   spring.mail.username=TU_EMAIL@gmail.com
   spring.mail.password=TU_APP_PASSWORD_GMAIL_AQUI
   
   # Gemini API
   gemini.api.key=TU_API_KEY_GEMINI_AQUI
   ```

### 2. Obtener Credenciales

#### 🔑 API Key de Google Gemini
1. Ve a: https://aistudio.google.com/app/apikey
2. Crea una nueva API key
3. Cópiala y pégala en `gemini.api.key`

#### 📧 App Password de Gmail
1. Ve a: https://myaccount.google.com/apppasswords
2. Genera una contraseña de aplicación para "Mail"
3. Cópiala (sin espacios) y pégala en `spring.mail.password`

#### 🗄️ Base de Datos MySQL
- Si usas Docker Compose, la contraseña está en `docker-compose.yml`
- Por defecto es: `leonardo`

### 3. Verificar Configuración

```bash
# Verifica que application.properties NO esté en Git
git status

# Deberías ver SOLO application.properties.example
# Si ves application.properties, ¡NO LO AGREGUES!
```

## ⚠️ IMPORTANTE: Seguridad

### ❌ NUNCA hagas esto:
```bash
git add src/main/resources/application.properties  # ¡PELIGRO!
git commit -m "Added config"
```

### ✅ SIEMPRE haz esto:
- Solo modifica tu `application.properties` local
- Si necesitas cambiar la estructura, edita `application.properties.example`
- Verifica con `git status` antes de hacer commit

## 🐳 Despliegue con Docker

Para producción, las credenciales se pasan como variables de entorno en `docker-compose.yml`:

```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
  GEMINI_API_KEY: ${GEMINI_API_KEY}
```

Y creas un archivo `.env` (que también está en `.gitignore`):

```env
DB_PASSWORD=tu_password_produccion
GEMINI_API_KEY=tu_api_key_produccion
```

## 📝 Checklist de Configuración

- [ ] Copiar `application.properties.example` a `application.properties`
- [ ] Configurar password de MySQL
- [ ] Obtener y configurar API key de Gemini
- [ ] Configurar credenciales de Gmail (si usas email)
- [ ] Configurar token de Apidecolecta (si usas facturación)
- [ ] Verificar que `application.properties` NO esté en `git status`
- [ ] Probar la aplicación: `docker-compose up`

## 🆘 Ayuda

Si tienes problemas con la configuración, revisa:
- Los logs de Docker: `docker-compose logs -f`
- Que todas las credenciales estén correctamente configuradas
- Que no haya espacios extra en las API keys
