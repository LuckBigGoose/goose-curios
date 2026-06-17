package com.luckgoose.goosecurios.mixin;

import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Slime访问器
 * 
 * <p>访问史莱姆的私有字段，用于时停期间保存和恢复蹲伏动画状态
 * 
 * @author luckgoose
 */
@Mixin(Slime.class)
public interface SlimeAccessor {
    @Accessor("squish")
    float goose_curios$getSquish();

    @Accessor("squish")
    void goose_curios$setSquish(float squish);

    @Accessor("oSquish")
    float goose_curios$getOSquish();

    @Accessor("oSquish")
    void goose_curios$setOSquish(float oSquish);

    @Accessor("targetSquish")
    float goose_curios$getTargetSquish();

    @Accessor("targetSquish")
    void goose_curios$setTargetSquish(float targetSquish);
}

