package com.maliopt.gpu;

import org.lwjgl.opengl.GL13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

public class CapabilityGate {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaliOptRenderer/Cap");
    private static final Set<String> availableExtensions = new HashSet<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        String extString = GL13.glGetString(GL13.GL_EXTENSIONS);
        if (extString != null) {
            for (String ext : extString.split(" ")) {
                availableExtensions.add(ext.trim());
            }
        }
        initialized = true;
        LOGGER.info("[CapabilityGate] Inicializado com {} extensões.", availableExtensions.size());
    }

    public static boolean hasExtension(String extensionName) {
        if (!initialized) {
            LOGGER.warn("[CapabilityGate] Chamado antes de initialize(). Assumindo false para {}", extensionName);
            return false;
        }
        return availableExtensions.contains(extensionName);
    }

    public static boolean hasAllExtensions(String... extensionNames) {
        for (String ext : extensionNames) {
            if (!hasExtension(ext)) return false;
        }
        return true;
    }

    public static boolean hasAnyExtension(String... extensionNames) {
        for (String ext : extensionNames) {
            if (hasExtension(ext)) return true;
        }
        return false;
    }

    public static boolean isInitialized() { return initialized; }
}
