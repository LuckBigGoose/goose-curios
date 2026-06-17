package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillCloakRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 人形盔甲层披风隐藏Mixin
 * 
 * <p>在时停期间隐藏盔甲层的披风渲染
 * 
 * @author luckgoose
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerCloakMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void goose_curios$startArmorCloak(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (BondWillCloakRenderState.shouldCloak(entity)) {
            ci.cancel();
        }
    }
}

