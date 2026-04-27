package com.maliopt.core;

import com.maliopt.gpu.CapabilityGate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModuleActivator {
    private static final Map<String, Module> modules = new LinkedHashMap<>();

    public static void register(String name, String[] extensions, Runnable activator, Runnable deactivator) {
        modules.put(name, new Module(name, extensions, activator, deactivator));
    }

    public static void activateAll() {
        if (!CapabilityGate.isInitialized()) {
            MaliOptLogger.warn("ModuleActivator", "CapabilityGate não inicializado. Nenhum módulo ativado.");
            return;
        }
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String name = entry.getKey();
            Module mod = entry.getValue();
            MaliOptLogger.info("ModuleActivator", "Verificando: " + name);
            boolean allPresent = true;
            for (String ext : mod.extensions) {
                boolean present = CapabilityGate.hasExtension(ext);
                if (!present) allPresent = false;
            }
            if (allPresent) {
                try { mod.activator.run(); mod.active = true; MaliOptLogger.info("ModuleActivator", "  Ativado: " + name); }
                catch (Exception e) { MaliOptLogger.error("ModuleActivator", "Erro ao ativar " + name + ": " + e.getMessage()); try { mod.deactivator.run(); } catch (Exception ignored) {} }
            } else {
                try { mod.deactivator.run(); } catch (Exception e) { MaliOptLogger.error("ModuleActivator", "Erro ao desativar " + name + ": " + e.getMessage()); }
                mod.active = false;
                MaliOptLogger.warn("ModuleActivator", "  Desativado: " + name);
            }
        }
    }

    public static boolean isModuleActive(String name) { Module mod = modules.get(name); return mod != null && mod.active; }

    private static class Module {
        final String name; final String[] extensions; final Runnable activator; final Runnable deactivator; boolean active = false;
        Module(String name, String[] extensions, Runnable activator, Runnable deactivator) {
            this.name = name; this.extensions = extensions; this.activator = activator; this.deactivator = deactivator;
        }
    }
}
