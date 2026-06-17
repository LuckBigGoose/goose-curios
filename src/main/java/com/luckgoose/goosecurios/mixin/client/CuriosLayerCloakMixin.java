package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillCloakRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.client.render.CuriosLayer;

/**
 * Curios层披风隐藏Mixin
 * 
 * <p>在时停期间隐藏Curios饰品的披风层渲染
 * 
 * @author luckgoose
 */
@Mixin(value = CuriosLayer.class, remap = false)
public abstract class CuriosLayerCloakMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void goose_curios$skipCloakedCurios(PoseStack poseStack, MultiBufferSource bufferSource, int light, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (BondWillCloakRenderState.shouldCloak(livingEntity)) {
            ci.cancel();
        }
    }
}

