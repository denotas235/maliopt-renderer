package com.maliopt;

import com.maliopt.core.MaliOptLogger;
import com.maliopt.core.ModuleActivator;
import com.maliopt.gpu.CapabilityGate;
import com.maliopt.gpu.ExtensionScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliOptMod implements ClientModInitializer {
    public static final String MOD_ID = "malioptrender";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MaliOptLogger.initialize();
        MaliOptLogger.info("Core", "MaliOptRenderer a iniciar...");

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            MaliOptLogger.info("Core", "Cliente iniciado. Contexto OpenGL pronto.");
            CapabilityGate.initialize();
            ExtensionScanner scanner = new ExtensionScanner();
            scanner.scan();
            MaliOptLogger.info("Core", "Cobertura de extensões: " + scanner.getCoveragePercent() + "%");
            registerModules();
            ModuleActivator.activateAll();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            MaliOptLogger.info("Core", "A encerrar MaliOptRenderer.");
        });
    }

    private void registerModules() {
        ModuleActivator.register("ASTC Textures", new String[]{"GL_KHR_texture_compression_astc_ldr", "GL_KHR_texture_compression_astc_hdr"},
            () -> MaliOptLogger.info("Core", "ASTC Textures ativado."), () -> MaliOptLogger.warn("Core", "ASTC Textures desativado."));
        ModuleActivator.register("Framebuffer Fetch", new String[]{"GL_ARM_shader_framebuffer_fetch"},
            () -> MaliOptLogger.info("Core", "FB Fetch ativado."), () -> MaliOptLogger.warn("Core", "FB Fetch desativado."));
        ModuleActivator.register("Pixel Local Storage", new String[]{"GL_EXT_shader_pixel_local_storage"},
            () -> MaliOptLogger.info("Core", "PLS ativado."), () -> MaliOptLogger.warn("Core", "PLS desativado."));
        ModuleActivator.register("Discard Framebuffer", new String[]{"GL_EXT_discard_framebuffer"},
            () -> MaliOptLogger.info("Core", "Discard FB ativado."), () -> MaliOptLogger.warn("Core", "Discard FB desativado."));
    }
}
