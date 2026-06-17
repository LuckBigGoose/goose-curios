package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillTimeStopClientEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 邦德的意志世界渲染器天气冻结Mixin
 * 
 * <p>修改世界渲染器,在时停期间冻结雨雪等天气粒子效果
 * 
 * @author luckgoose
 */
@Mixin(LevelRenderer.class)
public abstract class BondWillLevelRendererWeatherFreezeMixin {
    @Shadow
    private int ticks;

    private int goose_curios$bondWillFrozenWeatherTicks = Integer.MIN_VALUE;
    private int goose_curios$bondWillPreviousWeatherTicks;
    private boolean goose_curios$bondWillWeatherTicksOverridden;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void goose_curios$freezeBondWillWeatherRenderTicks(LightTexture lightTexture, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (BondWillTimeStopClientEffects.areLocalVisualEffectsEnabled()) {
            if (this.goose_curios$bondWillFrozenWeatherTicks == Integer.MIN_VALUE) {
                this.goose_curios$bondWillFrozenWeatherTicks = this.ticks;
            }
            this.goose_curios$bondWillPreviousWeatherTicks = this.ticks;
            this.ticks = this.goose_curios$bondWillFrozenWeatherTicks;
            this.goose_curios$bondWillWeatherTicksOverridden = true;
        } else {
            this.goose_curios$bondWillFrozenWeatherTicks = Integer.MIN_VALUE;
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("RETURN"))
    private void goose_curios$restoreBondWillWeatherRenderTicks(LightTexture lightTexture, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (this.goose_curios$bondWillWeatherTicksOverridden) {
            this.ticks = this.goose_curios$bondWillPreviousWeatherTicks;
            this.goose_curios$bondWillWeatherTicksOverridden = false;
        }
    }

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void goose_curios$cancelBondWillFrozenRainTick(Camera camera, CallbackInfo ci) {
        if (BondWillTimeStopClientEffects.areLocalVisualEffectsEnabled()) {
            ci.cancel();
        }
    }
}

