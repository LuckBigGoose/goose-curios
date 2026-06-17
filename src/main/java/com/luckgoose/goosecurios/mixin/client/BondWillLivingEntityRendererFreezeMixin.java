package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.ClientBondWillFreezeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 邦德的意志生物渲染器冻结Mixin
 * 
 * <p>修改生物渲染器，在时停期间冻结生物的肢体摆动和动画
 * 
 * @author luckgoose
 */
@Mixin(LivingEntityRenderer.class)
public abstract class BondWillLivingEntityRendererFreezeMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float goose_curios$freezeBondWillEntityYaw(float entityYaw, LivingEntity entity, float originalEntityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return ClientBondWillFreezeState.entityYaw(entity, entityYaw);
    }

    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float goose_curios$freezeBondWillPartialTick(float partialTick, LivingEntity entity, float entityYaw, float originalPartialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return ClientBondWillFreezeState.partial(entity, partialTick);
    }
}

