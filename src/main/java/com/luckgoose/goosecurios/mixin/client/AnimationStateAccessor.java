package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.world.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * AnimationState访问器
 * 
 * <p>访问动画状态的时间字段，用于时停期间冻结实体动画
 * 
 * @author luckgoose
 */
@Mixin(AnimationState.class)
public interface AnimationStateAccessor {
    @Accessor("lastTime")
    long goose_curios$getLastTime();

    @Accessor("accumulatedTime")
    long goose_curios$getAccumulatedTime();

    @Accessor("lastTime")
    void goose_curios$setLastTime(long lastTime);

    @Accessor("accumulatedTime")
    void goose_curios$setAccumulatedTime(long accumulatedTime);
}

