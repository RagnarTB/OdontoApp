package com.odontoapp.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;
    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void testBuscarPorId() {
        System.out.println("==============================================");
        System.out.println("➡️ INICIANDO TEST UNITARIO: Buscar Usuario por ID");

        // 1. ARRANGE
        Long idPrueba = 1L;
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(idPrueba);
        usuarioMock.setEmail("test@odontoapp.com");
        usuarioMock.setNombreCompleto("Juan Perez");

        System.out.println("   [Mock] Configurando repositorio simulado...");
        when(usuarioRepository.findById(idPrueba)).thenReturn(Optional.of(usuarioMock));

        // 2. ACT
        System.out.println("   [Accion] Llamando al servicio buscarPorId(" + idPrueba + ")...");
        Optional<Usuario> resultado = usuarioService.buscarPorId(idPrueba);

        // 3. ASSERT
        System.out.println("   [Verificacion] Validando resultados...");

        assertTrue(resultado.isPresent(), "El usuario debería ser encontrado");
        System.out.println("   ✅ Usuario encontrado: OK");

        assertEquals("Juan Perez", resultado.get().getNombreCompleto());
        System.out.println("   ✅ Nombre coincide (Juan Perez): OK");

        assertEquals("test@odontoapp.com", resultado.get().getEmail());
        System.out.println("   ✅ Email coincide: OK");

        verify(usuarioRepository).findById(idPrueba);

        System.out.println("✅ TEST FINALIZADO CON ÉXITO");
        System.out.println("==============================================");
    }
}
