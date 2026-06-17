package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillCloakRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 生物渲染器披风隐藏Mixin
 * 
 * <p>在时停期间隐藏玩家披风渲染，避免披风动画破坏冻结效果
 * 
 * @author luckgoose
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCloakMixin<T extends LivingEntity, M extends EntityModel<T>> implements RenderLayerParent<T, M> {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void goose_curios$hideCloakedEntity(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (BondWillCloakRenderState.shouldCloak(entity)) {
            ci.cancel();
        }
    }
}

