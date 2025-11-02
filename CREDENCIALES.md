# Credenciales de Acceso - OdontoApp

## Usuario Administrador (Creado Automáticamente)

El sistema crea automáticamente un usuario administrador al iniciar la aplicación por primera vez.

### Credenciales

```
Email:     admin@odontoapp.com
Password:  admin123
```

### Instrucciones de Acceso

1. Acceder a: `http://localhost:8080/login`
2. Hacer clic en la pestaña **"Personal Clínica"**
3. Ingresar las credenciales anteriores
4. Hacer clic en "Iniciar Sesión"

### Importante

- El usuario admin tiene rol **ADMIN** con todos los permisos
- Debe usar el login de "Personal Clínica", NO el de "Pacientes"
- La contraseña está hasheada con BCrypt en la base de datos
- El usuario se crea automáticamente en `DataInitializer.java` (línea 198-207)

## Otros Roles Creados

El sistema crea automáticamente los siguientes roles:

- **ADMIN**: Acceso total al sistema
- **ODONTOLOGO**: Gestión de citas, tratamientos, pacientes
- **RECEPCIONISTA**: Gestión de citas, pacientes, facturación
- **ALMACEN**: Gestión de inventario
- **PACIENTE**: Portal de paciente (solo lectura)

## Crear Nuevos Usuarios

Una vez logueado como administrador, puedes crear nuevos usuarios desde:

```
Dashboard → Usuarios → Nuevo Usuario
```

## Solución de Problemas

### Login muestra error

1. Verificar que estás usando la pestaña correcta:
   - Personal clínico: usa "Personal Clínica"
   - Pacientes: usa "Pacientes"

2. Verificar credenciales:
   - Email: exactamente `admin@odontoapp.com`
   - Password: exactamente `admin123`
   - Son case-sensitive

3. Revisar logs de la aplicación:
   ```bash
   docker-compose logs -f odontoapp-backend
   ```

4. Verificar que el usuario existe en la base de datos:
   ```sql
   USE odontoapp_db;
   SELECT email, nombre_completo, esta_activo FROM usuarios;
   ```

### Base de datos vacía

Si la base de datos está vacía o corrupta, reiniciar con:

```bash
docker-compose down -v  # Elimina volúmenes
docker-compose up --build  # Recrea todo
```

Esto volverá a crear el usuario administrador automáticamente.

## Cambiar Contraseña del Admin

Para cambiar la contraseña del administrador, puedes:

1. Desde la aplicación (una vez logueado):
   - Dashboard → Mi Perfil → Cambiar Contraseña

2. Desde la base de datos:
   ```sql
   UPDATE usuarios
   SET password = '$2a$10$TU_NUEVO_HASH_AQUI'
   WHERE email = 'admin@odontoapp.com';
   ```

   Para generar un nuevo hash BCrypt, puedes usar https://bcrypt-generator.com/
   o crear un usuario desde la aplicación y copiar su hash.

## Seguridad

**IMPORTANTE**: En producción, cambiar inmediatamente:

1. La contraseña del administrador
2. El email del administrador
3. Deshabilitar la creación automática de usuarios en `DataInitializer.java`

---

Para más información, consultar el README.md del proyecto.
