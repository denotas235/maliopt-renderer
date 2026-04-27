package com.maliopt.core;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MaliOptLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaliOptRenderer");
    private static Path logFile;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path logDir = gameDir.resolve("maliopt-renderer").resolve("logs");
            Files.createDirectories(logDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            logFile = logDir.resolve("maliopt_" + timestamp + ".log");
            Files.createFile(logFile);
            initialized = true;
            info("Core", "Sistema de logging inicializado: " + logFile.toString());
        } catch (IOException e) {
            LOGGER.error("[MaliOptRenderer] Erro ao criar ficheiro de log: {}", e.getMessage());
        }
    }

    public static void info(String module, String message) { log("INFO", module, message); }
    public static void warn(String module, String message) { log("WARN", module, message); }
    public static void error(String module, String message) { log("ERROR", module, message); }
    public static void debug(String module, String message) { log("DEBUG", module, message); }

    private static void log(String level, String module, String message) {
        String line = String.format("[%s] [%s] [%s] %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            level, module, message);
        switch (level) {
            case "ERROR": LOGGER.error("[{}] {}", module, message); break;
            case "WARN":  LOGGER.warn("[{}] {}", module, message); break;
            case "DEBUG": LOGGER.debug("[{}] {}", module, message); break;
            default:      LOGGER.info("[{}] {}", module, message); break;
        }
        if (initialized && logFile != null) {
            try { Files.writeString(logFile, line + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE); }
            catch (IOException ignored) {}
        }
    }

    public static boolean isInitialized() { return initialized; }
    public static Path getLogFile() { return logFile; }
}
