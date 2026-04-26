package com.maliopt;

import com.maliopt.gpu.ExtensionScanner;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliOptMod implements ClientModInitializer, ModMenuApi {
    public static final String MOD_ID = "malioptrender";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return null;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MaliOptRenderer] Iniciando...");

        // Aguardar o cliente estar pronto (contexto OpenGL criado pelo ESCraft)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LOGGER.info("[MaliOptRenderer] Cliente iniciado — a analisar extensões...");
            ExtensionScanner scanner = new ExtensionScanner();
            scanner.scan();
            LOGGER.info("[MaliOptRenderer] Cobertura de extensões: {}%", scanner.getCoveragePercent());
        });
    }
}
