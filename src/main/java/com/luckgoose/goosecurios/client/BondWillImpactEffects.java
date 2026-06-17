package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.init.ModSounds;
import com.luckgoose.goosecurios.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 邦德的意志射击冲击特效管理器
 * 
 * <p>管理射击命中时的视觉和音效反馈
 * 
 * <p>核心功能：
 * <ul>
 *   <li>后处理特效：镜头冲击扭曲效果</li>
 *   <li>冲击波渲染：3D空间中的圆形冲击波</li>
 *   <li>音效播放：命中音效和去重控制</li>
 *   <li>多实例管理：同时追踪多个冲击波实例</li>
 * </ul>
 * 
 * <p>特效细节：
 * <ul>
 *   <li>持续时间：20 tick（1秒）</li>
 *   <li>去重冷却：2 tick 防止同一实体重复触发</li>
 *   <li>着色器参数：基于时间的动态衰减</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class BondWillImpactEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger(BondWillImpactEffects.class);

    /** 活跃的冲击波实例列表 */
    private static final List<PunchInstance> INSTANCES = new ArrayList<>();
    
    /** 本地玩家冲击特效剩余tick */
    private static int localTicks;
    
    /** 上次播放特效的实体ID */
    private static int lastPlayedEntityId = -1;
    
    /** 去重冷却tick（防止同一实体短时间内重复触发） */
    private static int duplicateCooldownTicks;

    /**
     * 播放射击冲击特效
     * 
     * @param entityId 命中的实体ID（-1表示本地玩家）
     * @param progress 蓄力进度（0.0-1.0）
     */
    public static void play(int entityId, float progress) {
        if (!BondWillClientDisplay.isShotEffectEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        Entity entity = entityId >= 0 ? minecraft.level.getEntity(entityId) : minecraft.player;
        if (entity == null) {
            entity = minecraft.player;
        }
        if (duplicateCooldownTicks > 0 && lastPlayedEntityId == entity.getId()) {
            return;
        }
        lastPlayedEntityId = entity.getId();
        duplicateCooldownTicks = 2;
        INSTANCES.add(new PunchInstance(entity.getId(), 20));
        if (entity.getId() == minecraft.player.getId()) {
            localTicks = 20;
            BondWillPostEffectCoordinator.request(minecraft, BondWillPostEffectCoordinator.Effect.PUNCH);
            updateShader(minecraft, 0.0F);
        }
        if (BondWillClientDisplay.isShotSoundEnabled()) {
            float volume = 1.0F;
            float pitch = 1.04F;
            minecraft.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), ModSounds.BOND_WILL_BOOM.get(), SoundSource.MASTER, volume, pitch, false);
        }
    }

    public static void clear() {
        INSTANCES.clear();
        localTicks = 0;
        lastPlayedEntityId = -1;
        duplicateCooldownTicks = 0;
        BondWillPostEffectCoordinator.release(Minecraft.getInstance(), BondWillPostEffectCoordinator.Effect.PUNCH);
    }

    public static void tick() {
        if (!BondWillClientDisplay.isShotEffectEnabled()) {
            clear();
            return;
        }
        if (localTicks > 0) {
            localTicks--;
        }
        if (duplicateCooldownTicks > 0) {
            duplicateCooldownTicks--;
        }
        Iterator<PunchInstance> iterator = INSTANCES.iterator();
        while (iterator.hasNext()) {
            PunchInstance instance = iterator.next();
            instance.ticks--;
            if (instance.ticks <= 0) {
                iterator.remove();
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (localTicks > 0) {
            BondWillPostEffectCoordinator.request(minecraft, BondWillPostEffectCoordinator.Effect.PUNCH);
            updateShader(minecraft, 0.0F);
        } else {
            BondWillPostEffectCoordinator.release(minecraft, BondWillPostEffectCoordinator.Effect.PUNCH);
        }
    }

    public static void updateFrame(float partialTick) {
        if (localTicks <= 0) {
            return;
        }
        updateShader(Minecraft.getInstance(), partialTick);
    }

    public static void renderWorld(RenderLevelStageEvent event) {
        if (!BondWillClientDisplay.isShotEffectEnabled() || INSTANCES.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        for (PunchInstance instance : INSTANCES) {
            Entity entity = minecraft.level.getEntity(instance.entityId);
            if (entity == null) {
                continue;
            }
            renderInstance(poseStack, cameraPos, entity, instance.ticks, partialTick);
        }
    }

    private static void renderInstance(PoseStack poseStack, Vec3 cameraPos, Entity entity, int ticks, float partialTick) {
        float remaining = ticks - partialTick;
        float progress = Mth.clamp((20.0F - remaining) / 20.0F, 0.0F, 1.0F);
        if (progress < 0.05F || progress > 0.65F) {
            return;
        }
        float ringProgress = (progress - 0.05F) / 0.4F;
        if (ringProgress > 1.0F) {
            return;
        }
        Vec3 interpolated = entity.getPosition(partialTick);
        double eyeHeight = entity instanceof LivingEntity livingEntity ? livingEntity.getEyeHeight() : entity.getBbHeight() * 0.85D;
        Vec3 playerPos = new Vec3(interpolated.x, interpolated.y + eyeHeight, interpolated.z).add(entity.getViewVector(partialTick).scale(1.5D));
        poseStack.pushPose();
        poseStack.translate(playerPos.x - cameraPos.x, playerPos.y - cameraPos.y, playerPos.z - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTick)));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        int alpha = Mth.clamp((int) (255.0F * (1.0F - ringProgress)), 0, 255);
        float size = Mth.lerp(ringProgress, 0.5F, 6.5F);
        float pixelCount = Math.max(1.0F, Mth.lerp(ringProgress, 4.0F, 32.0F));
        float pixelSize = size / pixelCount;
        float inner = Math.max(0.0F, size - 2.0F);
        int steps = (int) size + 1;
        for (int x = 0; x <= steps; x++) {
            for (int y = 0; y <= steps; y++) {
                float distance = (float) Math.sqrt(x * x + y * y);
                if (distance <= size + 0.3F && distance >= inner - 0.3F) {
                    drawPaintPixel(matrix, buffer, x, y, pixelSize, 240, 250, 255, alpha);
                    if (x != 0) {
                        drawPaintPixel(matrix, buffer, -x, y, pixelSize, 240, 250, 255, alpha);
                    }
                    if (y != 0) {
                        drawPaintPixel(matrix, buffer, x, -y, pixelSize, 240, 250, 255, alpha);
                    }
                    if (x != 0 && y != 0) {
                        drawPaintPixel(matrix, buffer, -x, -y, pixelSize, 240, 250, 255, alpha);
                    }
                }
            }
        }
        tesselator.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawPaintPixel(Matrix4f matrix, BufferBuilder buffer, int x, int y, float pixelSize, int r, int g, int b, int a) {
        float px = x * pixelSize;
        float py = y * pixelSize;
        float half = pixelSize * 0.5F;
        buffer.vertex(matrix, px - half, py - half, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, px + half, py - half, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, px + half, py + half, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, px - half, py + half, 0.0F).color(r, g, b, a).endVertex();
    }

    private static void updateShader(Minecraft minecraft, float partialTick) {
        PostChain current = minecraft.gameRenderer.currentEffect();
        if (!BondWillPostEffectCoordinator.isCurrent(minecraft, BondWillPostEffectCoordinator.Effect.PUNCH)) {
            return;
        }
        float remaining = localTicks - partialTick;
        float punch = 0.0F;
        float shake = 0.0F;
        if (remaining <= 19.0F && remaining > 4.0F) {
            float t = (remaining - 4.0F) / 15.0F;
            punch = t * t * t;
        }
        if (remaining <= 19.0F) {
            shake = remaining / 19.0F;
        }
        try {
            for (PostPass pass : getPasses(current)) {
                setUniform(pass, "DashIntensity", 0.0F);
                setUniform(pass, "PunchIntensity", Mth.clamp(punch, 0.0F, 1.0F));
                setUniform(pass, "ShakeIntensity", Mth.clamp(shake, 0.0F, 1.0F));
                setUniform(pass, "GrabIntensity", 0.0F);
                setUniform(pass, "LockIntensity", 0.0F);
                setUniform(pass, "Time", (System.currentTimeMillis() % 1000000L) / 1000.0F);
            }
        } catch (RuntimeException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to update shader uniform", e);
            }
            // 降级：禁用效果但不崩溃
            BondWillPostEffectCoordinator.release(Minecraft.getInstance(), BondWillPostEffectCoordinator.Effect.PUNCH);
        }
    }

    private static List<PostPass> getPasses(PostChain chain) {
        return ((PostChainAccessor) chain).goose_curios$getPasses();
    }

    private static void setUniform(PostPass pass, String name, float value) {
        Uniform uniform = pass.getEffect().getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static class PunchInstance {
        private final int entityId;
        private int ticks;

        private PunchInstance(int entityId, int ticks) {
            this.entityId = entityId;
            this.ticks = ticks;
        }
    }

}

