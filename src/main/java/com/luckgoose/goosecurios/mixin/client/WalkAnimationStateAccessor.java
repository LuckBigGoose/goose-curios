package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * WalkAnimationState访问器
 * 
 * <p>访问行走动画状态字段，用于时停期间冻结行走动画
 * 
 * @author luckgoose
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld")
    float goose_curios$getSpeedOld();

    @Accessor("speed")
    float goose_curios$getSpeed();

    @Accessor("position")
    float goose_curios$getPosition();

    @Accessor("speedOld")
    void goose_curios$setSpeedOld(float speedOld);

    @Accessor("speed")
    void goose_curios$setSpeed(float speed);

    @Accessor("position")
    void goose_curios$setPosition(float position);
}

