package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.ClientBondWillFreezeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 邦德的意志末影龙渲染器冻结Mixin
 * 
 * <p>修改末影龙渲染器，在时停期间冻结末影龙的特殊飞行动画
 * 
 * @author luckgoose
 */
@Mixin(EnderDragonRenderer.class)
public abstract class BondWillEnderDragonRendererFreezeMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float goose_curios$freezeBondWillDragonPartialTick(float partialTick, EnderDragon dragon, float entityYaw, float originalPartialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        return ClientBondWillFreezeState.partial(dragon, partialTick);
    }
}

