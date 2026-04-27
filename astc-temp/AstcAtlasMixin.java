package com.hyengra.engine.mixin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * AstcAtlasMixin — carrega texturas ASTC pré-compiladas em vez de PNG
 * quando rodando no Android com suporte GL_KHR_texture_compression_astc_ldr.
 *
 * NOTA: este é o stub para construção futura.
 * Actualmente apenas detecta o ambiente e loga —
 * a substituição real de textura deve ser implementada
 * após a pipeline build-time estar funcional.
 *
 * Pré-requisitos para activar:
 *   1. png_to_astc.py executado → astc_manifest.json gerado no JAR
 *   2. GL_KHR_texture_compression_astc_ldr confirmado no device
 *   3. IS_ANDROID = true detectado via AndroidConfig
 */
@Mixin(SpriteLoader.class)
public abstract class AstcAtlasMixin {

    // GL enum para ASTC 4x4 RGBA — igual ao definido em GL_KHR_texture_compression_astc_ldr
    private static final int GL_COMPRESSED_RGBA_ASTC_4x4_KHR = 0x93B0;

    // Caminho do manifesto dentro do JAR/resources
    private static final String MANIFEST_PATH = "/assets/hyengra/astc_manifest.json";

    // Manifesto carregado uma vez (null = não disponível)
    private static Map<String, AstcEntry> astcManifest = null;
    private static boolean manifestLoaded = false;

    // ── Estrutura do manifesto ──────────────────────────────────────────────

    private static class AstcEntry {
        String astc;         // path relativo: assets/hyengra/textures_astc/...
        int    width;
        int    height;
        String block;        // "4x4x1"
        double size_kb;
        double original_kb;
    }

    // ── Inicialização ───────────────────────────────────────────────────────

    private static synchronized Map<String, AstcEntry> getManifest() {
        if (manifestLoaded) return astcManifest;
        manifestLoaded = true;
        try (InputStream is = AstcAtlasMixin.class.getResourceAsStream(MANIFEST_PATH)) {
            if (is == null) {
                System.err.println("[HYENGRA-ASTC] Manifesto não encontrado: " + MANIFEST_PATH);
                return null;
            }
            Type type = new TypeToken<Map<String, AstcEntry>>(){}.getType();
            astcManifest = new Gson().fromJson(new InputStreamReader(is), type);
            System.err.println("[HYENGRA-ASTC] Manifesto carregado: " + astcManifest.size() + " entradas");
        } catch (Exception e) {
            System.err.println("[HYENGRA-ASTC] Erro ao carregar manifesto: " + e.getMessage());
        }
        return astcManifest;
    }

    private static boolean isAstcSupported() {
        // Verifica extensão ASTC LDR via OpenGL
        // glGetString(GL_EXTENSIONS) retorna string com todas as extensões
        String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
        return extensions != null && extensions.contains("GL_KHR_texture_compression_astc_ldr");
    }

    private static boolean isAndroid() {
        // Mesma lógica do AndroidConfig.java no VulkanMod
        String osVersion = System.getProperty("os.version", "");
        String pojavPath = System.getProperty("pojav.path.minecraft", "");
        String renderer  = System.getenv("POJAV_RENDERER");
        return osVersion.contains("Android")
            || !pojavPath.isEmpty()
            || "vulkan_zink".equals(renderer)
            || "opengles3_virgl".equals(renderer);
    }

    // ── Mixin hook ──────────────────────────────────────────────────────────

    /**
     * TODO: injectar em SpriteLoader.loadSprite() ou equivalente
     * para substituir o NativeImage carregado por dados ASTC.
     *
     * O ponto de injecção correcto depende da versão do Minecraft.
     * Para 1.21.x verificar: SpriteLoader#load(ResourceManager, Identifier, int, Executor)
     *
     * Por agora este método serve de documentação da intenção:
     * quando isAndroid() && isAstcSupported() && manifesto contém a textura,
     * carregar o .astc e fazer glCompressedTexImage2D em vez de glTexImage2D.
     */
    @Inject(
        // Ajustar target ao método correcto após análise do bytecode 1.21.x
        method = "<init>",  // placeholder — substituir pelo método real
        at = @At("TAIL"),
        cancellable = true
    )
    private void onInit(CallbackInfoReturnable<?> cir) {
        // Stub: apenas loga estado na primeira execução
        if (!manifestLoaded) {
            System.err.println("[HYENGRA-ASTC] Android=" + isAndroid()
                + "  ASTC_EXT=" + isAstcSupported());
            getManifest();
        }
    }

    // ── Método utilitário para uso futuro ───────────────────────────────────

    /**
     * Carrega dados ASTC crus de um ficheiro .astc dentro do JAR.
     * O header ASTC tem 16 bytes; os dados comprimidos começam no byte 16.
     *
     * @param astcResourcePath caminho dentro do classpath (começa com /)
     * @return ByteBuffer com os dados ASTC sem o header, ou null em falha
     */
    public static ByteBuffer loadAstcData(String astcResourcePath) {
        try (InputStream is = AstcAtlasMixin.class.getResourceAsStream("/" + astcResourcePath)) {
            if (is == null) return null;
            byte[] all = is.readAllBytes();
            // Validar magic 0x5CA1AB13 (little-endian)
            int magic = (all[0] & 0xFF)
                      | ((all[1] & 0xFF) << 8)
                      | ((all[2] & 0xFF) << 16)
                      | ((all[3] & 0xFF) << 24);
            if (magic != 0x5CA1AB13) {
                System.err.println("[HYENGRA-ASTC] Magic inválido em: " + astcResourcePath);
                return null;
            }
            // Dados comprimidos começam após header de 16 bytes
            ByteBuffer buf = ByteBuffer.allocateDirect(all.length - 16);
            buf.put(all, 16, all.length - 16);
            buf.flip();
            return buf;
        } catch (Exception e) {
            System.err.println("[HYENGRA-ASTC] Erro ao carregar ASTC: " + e.getMessage());
            return null;
        }
    }
}
