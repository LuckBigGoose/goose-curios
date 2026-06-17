package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillCloakRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家渲染器手部披风隐藏Mixin
 * 
 * <p>在时停期间隐藏第一人称视角的披风渲染
 * 
 * @author luckgoose
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHandCloakMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    
    public PlayerRendererHandCloakMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }
    @Unique
    private AbstractClientPlayer goose_curios$handPlayer;

    @Unique
    private MultiBufferSource goose_curios$handBufferSource;

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void goose_curios$startHandCloak(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        this.goose_curios$handPlayer = player;
        this.goose_curios$handBufferSource = bufferSource;
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void goose_curios$endHandCloak(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        this.goose_curios$handPlayer = null;
        this.goose_curios$handBufferSource = null;
    }

    @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 0))
    private void goose_curios$renderCloakedArm(ModelPart arm, PoseStack poseStack, VertexConsumer originalConsumer, int packedLight, int packedOverlay) {
        goose_curios$renderHandPart(arm, poseStack, originalConsumer, packedLight, packedOverlay);
    }

    @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 1))
    private void goose_curios$renderCloakedSleeve(ModelPart sleeve, PoseStack poseStack, VertexConsumer originalConsumer, int packedLight, int packedOverlay) {
        goose_curios$renderHandPart(sleeve, poseStack, originalConsumer, packedLight, packedOverlay);
    }

    @Unique
    private void goose_curios$renderHandPart(ModelPart part, PoseStack poseStack, VertexConsumer originalConsumer, int packedLight, int packedOverlay) {
        if (!BondWillCloakRenderState.shouldCloak(this.goose_curios$handPlayer) || this.goose_curios$handBufferSource == null) {
            part.render(poseStack, originalConsumer, packedLight, packedOverlay);
            return;
        }
        VertexConsumer cloakConsumer = this.goose_curios$handBufferSource.getBuffer(RenderType.entityTranslucent(BondWillCloakRenderState.cloakTexture()));
        part.render(poseStack, cloakConsumer, BondWillCloakRenderState.light(), OverlayTexture.NO_OVERLAY, BondWillCloakRenderState.red(), BondWillCloakRenderState.green(), BondWillCloakRenderState.blue(), BondWillCloakRenderState.handAlpha());
    }
}

