package com.maliopt;

import com.maliopt.gpu.ExtensionScanner;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliOptMod implements ClientModInitializer {
    public static final String MOD_ID = "malioptrender";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MaliOptRenderer] Iniciando...");
        
        // Primeira função: detetar extensões OpenGL ES
        ExtensionScanner scanner = new ExtensionScanner();
        scanner.scan();
        
        LOGGER.info("[MaliOptRenderer] Cobertura de extensões: {}%", scanner.getCoveragePercent());
    }
}
