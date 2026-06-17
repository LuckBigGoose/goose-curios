package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.ClientBondWillFreezeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 邦德的意志实体渲染调度器冻结Mixin
 * 
 * <p>修改实体渲染调度器，在时停期间冻结所有实体的渲染动画
 * 
 * @author luckgoose
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class BondWillEntityRenderDispatcherFreezeMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void goose_curios$lockBondWillFrozenEntityBeforeRender(E entity, double x, double y, double z, float rotationYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ClientBondWillFreezeState.FrozenSnapshot snapshot = ClientBondWillFreezeState.snapshot(entity);
        if (snapshot != null) {
            snapshot.lockEntity(entity);
        }
    }
}

