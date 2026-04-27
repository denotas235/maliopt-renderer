package com.maliopt.mixin;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.platform.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {}

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {}
}
