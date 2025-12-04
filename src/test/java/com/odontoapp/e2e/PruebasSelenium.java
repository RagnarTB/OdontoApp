package com.odontoapp.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de Sistema (End-to-End) automatizadas con Selenium WebDriver.
 * Valida los flujos críticos de autenticación y acceso desde la perspectiva del
 * usuario.
 * * Requisito: Tener Google Chrome instalado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PruebasSelenium {

    private WebDriver driver;

    private final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    @Test
    void caso1_ValidarCargaPaginaLogin() {
        System.out.println(">>> Iniciando Caso 1: Carga de Login");
        driver.get(BASE_URL + "/login");

        String titulo = driver.getTitle();
        System.out.println("Título obtenido: " + titulo);

        boolean existeBotonLogin = driver.findElements(By.cssSelector("button[type='submit']")).size() > 0;
        assertTrue(existeBotonLogin, "El formulario de login debería desplegarse correctamente");
    }

    @Test
    void caso2_LoginFallido_CredencialesIncorrectas() {
        System.out.println(">>> Iniciando Caso 2: Login Fallido");
        driver.get(BASE_URL + "/login");

        WebElement usuarioInput = driver.findElement(By.name("username"));
        WebElement passwordInput = driver.findElement(By.name("password"));
        WebElement botonIngresar = driver.findElement(By.cssSelector("button[type='submit']"));

        usuarioInput.sendKeys("ola@odontoapp.com");
        passwordInput.sendKeys("clave_erronea_123");
        botonIngresar.click();

        boolean existeError = driver.getCurrentUrl().contains("error") ||
                driver.getPageSource().contains("Bad credentials") ||
                driver.getPageSource().contains("incorrectos");

        assertTrue(existeError, "El sistema debe indicar error o redirigir con ?error");
    }

    @Test
    void caso3_LoginExitoso_RedireccionDashboard() {
        System.out.println(">>> Iniciando Caso 3: Login Exitoso");
        driver.get(BASE_URL + "/login");

        driver.findElement(By.name("username")).sendKeys("admin@odontoapp.com");
        driver.findElement(By.name("password")).sendKeys("admin123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        String urlActual = driver.getCurrentUrl();
        System.out.println("URL tras login: " + urlActual);

        boolean accesoCorrecto = urlActual.contains("dashboard") || urlActual.contains("home")
                || urlActual.equals(BASE_URL + "/");
        assertTrue(accesoCorrecto, "Debería redirigir al área privada tras login exitoso");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
