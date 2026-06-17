package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.client.BondWillClientDisplay;
import com.luckgoose.goosecurios.config.BondWillConfig;
import com.luckgoose.goosecurios.config.GooseClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.entity.PartEntity;

import java.util.Optional;

/**
 * 邦德的意志爆头准星渲染器
 * 
 * <p>在瞄准生物时渲染爆头判定框，帮助玩家瞄准头部
 * 
 * <p>渲染内容：
 * <ul>
 *   <li>实体碰撞箱（白色）：整体边界</li>
 *   <li>爆头判定框（绿色）：TACZ爆头判定区域</li>
 *   <li>眼睛位置（红色十字）：精确爆头点</li>
 * </ul>
 * 
 * <p>性能优化：
 * <ul>
 *   <li>目标缓存：3 tick内复用瞄准目标</li>
 *   <li>距离限制：最大渲染距离可配置</li>
 *   <li>配置开关：可通过客户端配置关闭</li>
 * </ul>
 * 
 * @author luckgoose
 */
public final class BondWillHeadshotRenderer {

    private static final float AABB_LINE_RED = 1.0F;
    private static final float AABB_LINE_GREEN = 1.0F;
    private static final float AABB_LINE_BLUE = 1.0F;
    private static final float AABB_LINE_ALPHA = 1.0F;
    private static final float PART_LINE_RED = 0.0F;
    private static final float PART_LINE_GREEN = 1.0F;
    private static final float PART_LINE_BLUE = 0.0F;
    private static final float PART_LINE_ALPHA = 1.0F;
    private static final float EYE_LINE_RED = 1.0F;
    private static final float EYE_LINE_GREEN = 0.0F;
    private static final float EYE_LINE_BLUE = 0.0F;
    private static final float EYE_LINE_ALPHA = 1.0F;
    private static final double EYE_LINE_HALF_THICKNESS = 0.01D;
    private static final int TARGET_CACHE_TICKS = 3;

    private static LivingEntity cachedTarget;
    private static long cacheTickExpire;

    private BondWillHeadshotRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!GooseClientConfig.BOND_WILL_HEADSHOT_GUIDE.get() || !BondWillClientDisplay.isHitboxDisplayEnabled()) {
            clearTarget();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.screen != null || minecraft.options.hideGui) {
            clearTarget();
            return;
        }
        if (!IGun.mainhandHoldGun(player)) {
            clearTarget();
            return;
        }
        if (IGunOperator.fromLivingEntity(player).getSynAimingProgress() < BondWillConfig.AIMING_PROGRESS_THRESHOLD.get()) {
            clearTarget();
            return;
        }

        float partialTick = event.getPartialTick();
        double maxDistance = resolveMaxDistance();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(maxDistance));
        LivingEntity target = resolveTarget(minecraft, player, eye, end, maxDistance, partialTick);
        if (target == null) {
            return;
        }

        renderVanillaEntityAabb(event.getPoseStack(), minecraft, target, getInterpolatedEntityBox(target, partialTick), partialTick);
    }

    private static void clearTarget() {
        cachedTarget = null;
        cacheTickExpire = 0L;
    }

    private static double resolveMaxDistance() {
        double maxDistance = GooseClientConfig.BOND_WILL_HEADSHOT_GUIDE_MAX_DISTANCE.get();
        return maxDistance > 0.0D ? maxDistance : 80.0D;
    }

    /**
     * 只缓存当前瞄准目标，避免每一帧都扫描视线路径上的实体。
     */
    private static LivingEntity resolveTarget(Minecraft minecraft, LocalPlayer player, Vec3 eye, Vec3 end, double maxDistance, float partialTick) {
        long currentTick = minecraft.level.getGameTime();
        LivingEntity target = cachedTarget;
        if (target == null || !target.isAlive() || target.isRemoved() || currentTick >= cacheTickExpire) {
            target = findLookTarget(minecraft, player, eye, end, maxDistance, partialTick);
            cachedTarget = target;
            cacheTickExpire = currentTick + TARGET_CACHE_TICKS;
        }
        return target;
    }

    /**
     * 使用原版实体 AABB 做射线命中，只选择准星方向上最近的 LivingEntity。
     */
    private static LivingEntity findLookTarget(Minecraft minecraft, LocalPlayer player, Vec3 eye, Vec3 end, double maxDistance, float partialTick) {
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        LivingEntity best = null;
        double bestDistance = maxDistance * maxDistance;
        for (LivingEntity entity : minecraft.level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            AABB bodyBox = getInterpolatedEntityBox(entity, partialTick);
            if (!isRenderableBox(bodyBox)) {
                continue;
            }
            Optional<Vec3> hit = clipEntityOrParts(entity, bodyBox, eye, end, partialTick);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = eye.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    /**
     * AABB 使用原版 getBoundingBox，用 partialTick 平移到当前渲染帧，避免移动实体线框滞后。
     */
    private static AABB getInterpolatedEntityBox(LivingEntity target, float partialTick) {
        double x = target.xOld + (target.getX() - target.xOld) * partialTick;
        double y = target.yOld + (target.getY() - target.yOld) * partialTick;
        double z = target.zOld + (target.getZ() - target.zOld) * partialTick;
        return target.getBoundingBox().move(-target.getX(), -target.getY(), -target.getZ()).move(x, y, z);
    }

    /**
     * PartEntity 直接使用当前位置的 AABB，避免插值导致的抖动
     * 
     * <p>修复：PartEntity 的位置由父实体控制，xOld/yOld/zOld 可能不准确，
     * 直接使用 getBoundingBox() 可以避免抖动
     */
    private static AABB getInterpolatedPartBox(PartEntity<?> part, float partialTick) {
        return part.getBoundingBox();
    }

    /**
     * 多部件实体优先按各 PartEntity AABB 命中。
     */
    private static Optional<Vec3> clipEntityOrParts(LivingEntity entity, AABB bodyBox, Vec3 eye, Vec3 end, float partialTick) {
        Optional<Vec3> bodyHit = bodyBox.clip(eye, end);
        Optional<Vec3> bestHit = bodyHit;
        double bestDistance = bodyHit.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
        PartEntity<?>[] parts = entity.getParts();
        if (parts == null || parts.length == 0) {
            return bestHit;
        }
        for (PartEntity<?> part : parts) {
            AABB partBox = getInterpolatedPartBox(part, partialTick);
            if (!isRenderablePartBox(partBox)) {
                continue;
            }
            Optional<Vec3> partHit = partBox.clip(eye, end);
            if (partHit.isEmpty()) {
                continue;
            }
            double distance = eye.distanceToSqr(partHit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestHit = partHit;
            }
        }
        return bestHit;
    }

    /**
     * 复用原版公开 LevelRenderer.renderLineBox 绘制 AABB，并在同一线框批次补充红色眼睛位置线。
     */
    private static void renderVanillaEntityAabb(PoseStack poseStack, Minecraft minecraft, LivingEntity target, AABB box, float partialTick) {
        if (!isRenderableBox(box)) {
            return;
        }
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        AABB localBox = box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, buffer, localBox, AABB_LINE_RED, AABB_LINE_GREEN, AABB_LINE_BLUE, AABB_LINE_ALPHA);
        renderPartBoxes(poseStack, buffer, cameraPos, target, partialTick);
        renderEyeHeightLine(poseStack, buffer, cameraPos, target, box, partialTick);
        bufferSource.endBatch(RenderType.lines());
    }

    /**
     * 多部件实体额外绘制每个 PartEntity 的 AABB。
     */
    private static void renderPartBoxes(PoseStack poseStack, VertexConsumer buffer, Vec3 cameraPos, LivingEntity target, float partialTick) {
        PartEntity<?>[] parts = target.getParts();
        if (parts == null || parts.length == 0) {
            return;
        }
        for (PartEntity<?> part : parts) {
            AABB partBox = getInterpolatedPartBox(part, partialTick);
            if (!isRenderablePartBox(partBox)) {
                continue;
            }
            AABB localPartBox = partBox.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            LevelRenderer.renderLineBox(poseStack, buffer, localPartBox, PART_LINE_RED, PART_LINE_GREEN, PART_LINE_BLUE, PART_LINE_ALPHA);
        }
    }

    /**
     * 红色眼睛位置线标记实体眼高平面。
     */
    private static void renderEyeHeightLine(PoseStack poseStack, VertexConsumer buffer, Vec3 cameraPos, LivingEntity target, AABB box, float partialTick) {
        double eyeY = target.getEyePosition(partialTick).y;
        if (eyeY < box.minY || eyeY > box.maxY) {
            return;
        }
        AABB eyePlane = new AABB(box.minX, eyeY - EYE_LINE_HALF_THICKNESS, box.minZ, box.maxX, eyeY + EYE_LINE_HALF_THICKNESS, box.maxZ)
                .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        LevelRenderer.renderLineBox(poseStack, buffer, eyePlane, EYE_LINE_RED, EYE_LINE_GREEN, EYE_LINE_BLUE, EYE_LINE_ALPHA);
    }

    private static boolean isRenderableBox(AABB box) {
        return isFiniteBox(box)
                && box.getXsize() > 0.0D
                && box.getYsize() > 0.0D
                && box.getZsize() > 0.0D
                && box.getXsize() <= 16.0D
                && box.getYsize() <= 16.0D
                && box.getZsize() <= 16.0D;
    }

    private static boolean isRenderablePartBox(AABB box) {
        return isFiniteBox(box)
                && box.getXsize() > 0.0D
                && box.getYsize() > 0.0D
                && box.getZsize() > 0.0D
                && box.getXsize() <= 64.0D
                && box.getYsize() <= 64.0D
                && box.getZsize() <= 64.0D;
    }

    private static boolean isFiniteBox(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ) && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }
}
