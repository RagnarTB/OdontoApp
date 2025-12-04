package com.odontoapp.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);


    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:leonardo}")
    private String dbPassword;


    private String dbHost = "mysql-db"; 
    private String dbName = "odontoapp_db";
    private String backupDir = "/app/backups";

    /**
     * Backup Real a las 12:00 AM
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void realizarBackupDiario() {
        log.info(">>> INICIANDO BACKUP REAL DE BASE DE DATOS...");

        String fecha = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        String nombreArchivo = "backup_" + fecha + ".sql";
        File archivoDestino = new File(backupDir, nombreArchivo);

        String[] comando = {
            "mysqldump",
            "-h", dbHost,
            "-u", dbUser,
            "-p" + dbPassword,
            "--single-transaction",
            "--routines",
            dbName
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(comando);
            
            pb.redirectOutput(ProcessBuilder.Redirect.to(archivoDestino));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process proceso = pb.start();

            boolean termino = proceso.waitFor(5, TimeUnit.MINUTES);

            if (termino && proceso.exitValue() == 0) {
                log.info("✅ BACKUP EXITOSO GENERADO: {}", archivoDestino.getAbsolutePath());
                log.info("Tamaño del archivo: {} bytes", archivoDestino.length());
            } else {
                log.error("❌ ERROR EN BACKUP. Código de salida: {}", proceso.exitValue());
            }

        } catch (IOException | InterruptedException e) {
            log.error("❌ EXCEPCIÓN CRÍTICA DURANTE EL BACKUP", e);
            Thread.currentThread().interrupt();
        }
    }
}
