package com.luckgoose.goosecurios.compat.tacz.bondwill;

import com.luckgoose.goosecurios.mixin.SlimeAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 羁绊邦德的意志 - 被冻结实体的状态保存
 * 
 * <p>
 * 保存实体被时停冻结前的所有状态，以便在时停结束时恢复。
 * 包括位置、旋转、速度、AI状态、重力状态等。
 * </p>
 * 
 * <p><b>状态保存策略：</b></p>
 * <ul>
 *   <li>基础状态：位置、旋转、速度</li>
 *   <li>实体属性：AI状态、重力状态</li>
 *   <li>多部件实体：保存每个部件的状态（如末影龙）</li>
 *   <li>特殊实体：史莱姆的挤压动画</li>
 * </ul>
 * 
 * <p><b>弱引用设计：</b></p>
 * <p>
 * 使用WeakReference持有实体引用，如果实体已被卸载或死亡，
 * 弱引用会自动清除，避免内存泄漏。resolve()方法负责
 * 在需要时重新获取实体引用。
 * </p>
 * 
 * @author luckgoose
 * @see BondWillTimeStopManager 时停管理器
 */
public class BondWillFrozenEntityState {
    /** 实体UUID */
    private final UUID entityUuid;
    
    /** 实体所在维度 */
    private final ResourceKey<Level> dimension;
    
    /** 冻结前的速度 */
    private final Vec3 motion;
    
    /** 实体姿态状态 */
    private final EntityPoseState pose;
    
    /** 冻结前的AI状态（仅Mob） */
    private final boolean wasNoAi;
    
    /** 冻结前的重力状态 */
    private final boolean hadNoGravity;
    
    /** 多部件实体的部件姿态列表 */
    private final List<EntityPoseState> partPoses;
    
    /** 实体的弱引用（避免内存泄漏） */
    private WeakReference<Entity> entityRef;

    /**
     * 构造冻结状态
     * 
     * <p>
     * 捕获实体当前的所有状态。
     * </p>
     * 
     * @param entity 要冻结的实体
     */
    public BondWillFrozenEntityState(Entity entity) {
        this.entityUuid = entity.getUUID();
        this.dimension = entity.level().dimension();
        this.motion = entity.getDeltaMovement();
        this.pose = new EntityPoseState(entity);
        this.wasNoAi = entity instanceof Mob mob && mob.isNoAi();
        this.hadNoGravity = entity.isNoGravity();
        this.partPoses = captureParts(entity);
        this.entityRef = new WeakReference<>(entity);
    }

    public UUID entityUuid() {
        return entityUuid;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Vec3 motion() {
        return motion;
    }

    public EntityPoseState pose() {
        return pose;
    }

    public boolean wasNoAi() {
        return wasNoAi;
    }

    public boolean hadNoGravity() {
        return hadNoGravity;
    }

    public List<EntityPoseState> partPoses() {
        return partPoses;
    }

    /**
     * 解析实体引用
     * 
     * <p>
     * 尝试从弱引用获取实体，如果失败则通过UUID从世界查找。
     * 找到后更新弱引用以供下次使用。
     * </p>
     * 
     * @param level 服务器世界
     * @return 实体对象，如果不存在则为null
     */
    public Entity resolve(ServerLevel level) {
        Entity entity = entityRef.get();
        if (entity != null && entity.isAlive() && entity.level() == level) return entity;
        entity = level.getEntity(entityUuid);
        if (entity != null) entityRef = new WeakReference<>(entity);
        return entity;
    }

    /**
     * 捕获多部件实体的所有部件姿态
     * 
     * <p>
     * 用于末影龙等多部件实体。
     * </p>
     * 
     * @param entity 实体
     * @return 部件姿态列表
     */
    private static List<EntityPoseState> captureParts(Entity entity) {
        PartEntity<?>[] parts = entity.getParts();
        if (parts == null || parts.length == 0) return List.of();
        List<EntityPoseState> poses = new ArrayList<>();
        for (PartEntity<?> part : parts) {
            poses.add(new EntityPoseState(part));
        }
        return poses;
    }

    /**
     * 实体姿态状态
     * 
     * <p>
     * 保存实体的位置、旋转和特殊动画状态。
     * </p>
     */
    public static class EntityPoseState {
        private final Vec3 position;
        private final float xRot;
        private final float yRot;
        private final double xo;
        private final double yo;
        private final double zo;
        private final float xRotO;
        private final float yRotO;
        private final Float yBodyRot;
        private final Float yBodyRotO;
        private final Float yHeadRot;
        private final Float yHeadRotO;
        private final SlimePoseState slimePose;

        public EntityPoseState(Entity entity) {
            this.position = entity.position();
            this.xRot = entity.getXRot();
            this.yRot = entity.getYRot();
            this.xo = entity.xo;
            this.yo = entity.yo;
            this.zo = entity.zo;
            this.xRotO = entity.xRotO;
            this.yRotO = entity.yRotO;
            if (entity instanceof LivingEntity living) {
                this.yBodyRot = living.yBodyRot;
                this.yBodyRotO = living.yBodyRotO;
                this.yHeadRot = living.yHeadRot;
                this.yHeadRotO = living.yHeadRotO;
            } else {
                this.yBodyRot = null;
                this.yBodyRotO = null;
                this.yHeadRot = null;
                this.yHeadRotO = null;
            }
            this.slimePose = entity instanceof Slime slime ? new SlimePoseState(slime) : null;
        }

        public void lock(Entity entity) {
            entity.setPos(position.x, position.y, position.z);
            entity.setXRot(xRot);
            entity.setYRot(yRot);
            entity.xo = xo;
            entity.yo = yo;
            entity.zo = zo;
            entity.xRotO = xRotO;
            entity.yRotO = yRotO;
            if (entity instanceof LivingEntity living && yBodyRot != null) {
                living.yBodyRot = yBodyRot;
                living.yBodyRotO = yBodyRotO;
                living.yHeadRot = yHeadRot;
                living.yHeadRotO = yHeadRotO;
            }
            entity.xOld = position.x;
            entity.yOld = position.y;
            entity.zOld = position.z;
            entity.walkDistO = entity.walkDist;
            entity.moveDist = 0.0F;
            entity.flyDist = 0.0F;
            if (entity instanceof Slime slime && slimePose != null) {
                slimePose.lock(slime);
            }
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static class SlimePoseState {
        private final float squish;
        private final float oSquish;
        private final float targetSquish;

        private SlimePoseState(Slime slime) {
            SlimeAccessor accessor = (SlimeAccessor) slime;
            this.squish = accessor.goose_curios$getSquish();
            this.oSquish = accessor.goose_curios$getOSquish();
            this.targetSquish = accessor.goose_curios$getTargetSquish();
        }

        private void lock(Slime slime) {
            SlimeAccessor accessor = (SlimeAccessor) slime;
            accessor.goose_curios$setSquish(squish);
            accessor.goose_curios$setOSquish(oSquish);
            accessor.goose_curios$setTargetSquish(targetSquish);
        }
    }
}

