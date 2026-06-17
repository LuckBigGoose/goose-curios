package com.luckgoose.goosecurios.compat.tacz.bondwill;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 邦德的意志 - 时停实例
 * 
 * <p>
 * 表示单个玩家触发的时停效果实例。
 * 每个时停实例包含玩家UUID、维度、中心位置和半径。
 * </p>
 * 
 * <p><b>实例生命周期：</b></p>
 * <ol>
 *   <li>玩家加成达到100%时创建实例</li>
 *   <li>玩家移动时更新中心位置（跟随玩家）</li>
 *   <li>时停结束或玩家取消时移除实例</li>
 * </ol>
 * 
 * <p><b>不可变性：</b></p>
 * <ul>
 *   <li>owner和radius是final的，创建后不可变</li>
 *   <li>dimension和center可以通过update()方法更新</li>
 * </ul>
 * 
 * @author luckgoose
 * @see BondWillTimeStopManager 时停管理器
 */
public class BondWillTimeStopInstance {
    /** 时停的拥有者UUID（不可变） */
    private final UUID owner;
    
    /** 时停所在的维度（可更新） */
    private ResourceKey<Level> dimension;
    
    /** 时停的中心位置（可更新） */
    private Vec3 center;
    
    /** 时停半径（不可变） */
    private final double radius;

    /**
     * 构造时停实例
     * 
     * @param owner 拥有者UUID
     * @param dimension 维度
     * @param center 中心位置
     * @param radius 半径
     */
    public BondWillTimeStopInstance(UUID owner, ResourceKey<Level> dimension, Vec3 center, double radius) {
        this.owner = owner;
        this.dimension = dimension;
        this.center = center;
        this.radius = radius;
    }

    /**
     * 获取拥有者UUID
     * 
     * @return 拥有者UUID
     */
    public UUID owner() {
        return owner;
    }

    /**
     * 获取时停所在的维度
     * 
     * @return 维度ResourceKey
     */
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /**
     * 获取时停中心位置
     * 
     * @return 中心位置向量
     */
    public Vec3 center() {
        return center;
    }

    /**
     * 获取时停半径
     * 
     * @return 半径（方块）
     */
    public double radius() {
        return radius;
    }

    /**
     * 更新时停的维度和中心位置
     * 
     * <p>
     * 当玩家移动或切换维度时调用，使时停范围跟随玩家。
     * </p>
     * 
     * @param dimension 新的维度
     * @param center 新的中心位置
     */
    public void update(ResourceKey<Level> dimension, Vec3 center) {
        this.dimension = dimension;
        this.center = center;
    }

    /**
     * 判断实体是否在时停范围内
     * 
     * <p><b>判断条件：</b></p>
     * <ol>
     *   <li>实体与时停在同一维度</li>
     *   <li>实体到中心的距离 <= 半径</li>
     * </ol>
     * 
     * <p>
     * 使用距离平方比较避免开方运算，提高性能。
     * </p>
     * 
     * @param entity 要检查的实体
     * @return 是否在时停范围内
     */
    public boolean contains(Entity entity) {
        return entity.level().dimension() == dimension && entity.position().distanceToSqr(center) <= radius * radius;
    }
}

