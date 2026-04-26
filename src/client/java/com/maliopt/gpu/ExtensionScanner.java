package com.maliopt.gpu;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExtensionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaliOptRenderer");

    private static final Set<String> MALI_G52_EXTENSIONS = new HashSet<>(Arrays.asList(
        "GL_EXT_debug_marker", "GL_ARM_mali_shader_binary", "GL_EXT_disjoint_timer_query",
        "GL_EXT_texture_filter_anisotropic", "GL_EXT_texture_format_BGRA8888",
        "GL_EXT_texture_type_2_10_10_10_REV", "GL_OES_compressed_ETC1_RGB8_texture",
        "GL_OES_depth_texture", "GL_OES_depth_texture_cube_map", "GL_OES_depth24",
        "GL_OES_EGL_image", "GL_OES_EGL_image_external", "GL_OES_EGL_image_external_essl3",
        "GL_OES_EGL_sync", "GL_OES_element_index_uint", "GL_OES_fbo_render_mipmap",
        "GL_OES_get_program_binary", "GL_OES_mapbuffer", "GL_OES_packed_depth_stencil",
        "GL_OES_rgb8_rgba8", "GL_OES_standard_derivatives", "GL_OES_surfaceless_context",
        "GL_OES_texture_3D", "GL_OES_texture_compression_astc", "GL_OES_texture_float",
        "GL_OES_texture_float_linear", "GL_OES_texture_half_float", "GL_OES_texture_half_float_linear",
        "GL_OES_texture_npot", "GL_OES_vertex_array_object", "GL_EXT_blend_minmax",
        "GL_EXT_read_format_bgra", "GL_EXT_occlusion_query_boolean", "GL_EXT_debug_label",
        "GL_EXT_texture_rg", "GL_EXT_discard_framebuffer", "GL_EXT_multisampled_render_to_texture",
        "GL_EXT_multisampled_render_to_texture2", "GL_EXT_robustness", "GL_EXT_texture_storage",
        "GL_EXT_sRGB", "GL_EXT_texture_sRGB_decode", "GL_EXT_texture_sRGB_R8",
        "GL_EXT_texture_sRGB_RG8", "GL_EXT_sRGB_write_control",
        "GL_EXT_texture_compression_astc_decode_mode", "GL_EXT_texture_compression_astc_decode_mode_rgb9e5",
        "GL_KHR_debug", "GL_KHR_texture_compression_astc_ldr", "GL_KHR_texture_compression_astc_hdr",
        "GL_KHR_no_error", "GL_KHR_parallel_shader_compile", "GL_OES_EGL_image_external_essl1",
        "GL_EXT_external_buffer", "GL_EXT_buffer_storage", "GL_EXT_shader_pixel_local_storage",
        "GL_ARM_shader_framebuffer_fetch", "GL_ARM_shader_framebuffer_fetch_depth_stencil",
        "GL_ARM_mali_program_binary", "GL_EXT_copy_image", "GL_EXT_color_buffer_half_float",
        "GL_EXT_color_buffer_float", "GL_EXT_YUV_target", "GL_OES_texture_stencil8",
        "GL_OES_shader_multisample_interpolation", "GL_OES_shader_image_atomic",
        "GL_OES_sample_variables", "GL_OES_sample_shading", "GL_OES_gpu_shader5",
        "GL_OES_shader_io_blocks", "GL_OES_texture_buffer", "GL_OES_texture_cube_map_array",
        "GL_OES_texture_view", "GL_OES_geometry_shader", "GL_OES_geometry_point_size",
        "GL_OES_tessellation_shader", "GL_OES_tessellation_point_size",
        "GL_EXT_texture_border_clamp", "GL_EXT_texture_buffer", "GL_EXT_texture_cube_map_array",
        "GL_EXT_texture_view", "GL_EXT_geometry_shader", "GL_EXT_geometry_point_size",
        "GL_EXT_tessellation_shader", "GL_EXT_tessellation_point_size", "GL_EXT_gpu_shader5",
        "GL_EXT_shader_io_blocks", "GL_EXT_shader_implicit_conversions", "GL_EXT_shader_integer_mix",
        "GL_EXT_shader_non_constant_global_initializers", "GL_EXT_protected_textures",
        "GL_KHR_blend_equation_advanced", "GL_KHR_blend_equation_advanced_coherent",
        "GL_OES_texture_storage_multisample_2d_array", "GL_OES_viewport_array",
        "GL_EXT_separate_shader_objects", "GL_OVR_multiview", "GL_OVR_multiview2",
        "GL_OVR_multiview_multisampled_render_to_texture", "GL_KHR_robustness",
        "GL_KHR_robust_buffer_access_behavior", "GL_EXT_draw_buffers_indexed"
    ));

    private Set<String> detectedExtensions = new HashSet<>();
    private Set<String> missingExtensions = new HashSet<>();
    private Set<String> commonExtensions = new HashSet<>();
    private boolean scanned = false;

    public void scan() {
        if (scanned) return;
        LOGGER.info("[MaliOptRenderer] A analisar extensões OpenGL ES...");

        detectedExtensions = getCurrentExtensions();

        commonExtensions = new HashSet<>(MALI_G52_EXTENSIONS);
        commonExtensions.retainAll(detectedExtensions);

        missingExtensions = new HashSet<>(MALI_G52_EXTENSIONS);
        missingExtensions.removeAll(detectedExtensions);

        scanned = true;
        logReport();
    }

    private Set<String> getCurrentExtensions() {
        Set<String> extensions = new HashSet<>();
        try {
            int numExtensions = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
            for (int i = 0; i < numExtensions; i++) {
                String ext = GL30.glGetStringi(GL11.GL_EXTENSIONS, i);
                if (ext != null) extensions.add(ext);
            }
        } catch (Exception e) {
            String extString = GL11.glGetString(GL11.GL_EXTENSIONS);
            if (extString != null) {
                extensions.addAll(Arrays.asList(extString.split(" ")));
            }
        }
        return extensions;
    }

    private void logReport() {
        LOGGER.info("[MaliOptRenderer] ========== RELATÓRIO DE EXTENSÕES ==========");
        LOGGER.info("[MaliOptRenderer] Detectadas: {} extensões", detectedExtensions.size());
        LOGGER.info("[MaliOptRenderer] Comuns (Mali-G52): {} extensões", commonExtensions.size());
        LOGGER.info("[MaliOptRenderer] Em falta: {} extensões", missingExtensions.size());
        LOGGER.info("[MaliOptRenderer] Cobertura: {}%", getCoveragePercent());
        LOGGER.info("[MaliOptRenderer] ");
        LOGGER.info("[MaliOptRenderer] ========== LISTA COMPLETA (102 extensões Mali-G52) ==========");

        List<String> sorted = new ArrayList<>(MALI_G52_EXTENSIONS);
        Collections.sort(sorted);

        int activeCount = 0;
        int inactiveCount = 0;

        for (String ext : sorted) {
            boolean present = detectedExtensions.contains(ext);
            if (present) {
                LOGGER.info("[MaliOptRenderer]   ✅ ATIVA   | {}", ext);
                activeCount++;
            } else {
                LOGGER.info("[MaliOptRenderer]   ❌ INATIVA | {}", ext);
                inactiveCount++;
            }
        }

        LOGGER.info("[MaliOptRenderer] ");
        LOGGER.info("[MaliOptRenderer] TOTAL: {} ativas, {} inativas (de 102)", activeCount, inactiveCount);
        LOGGER.info("[MaliOptRenderer] =================================================================");
    }

    public Set<String> getDetectedExtensions() { return Collections.unmodifiableSet(detectedExtensions); }
    public Set<String> getMissingExtensions() { return Collections.unmodifiableSet(missingExtensions); }
    public Set<String> getCommonExtensions() { return Collections.unmodifiableSet(commonExtensions); }
    public boolean isScanned() { return scanned; }

    public int getCoveragePercent() {
        if (MALI_G52_EXTENSIONS.isEmpty()) return 100;
        return (commonExtensions.size() * 100) / MALI_G52_EXTENSIONS.size();
    }
}
