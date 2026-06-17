package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Slime客户端访问器
 * 
 * <p>访问史莱姆客户端的蹲伏值，用于时停期间冻结蹲伏动画
 * 
 * @author luckgoose
 */
@Mixin(Slime.class)
public interface SlimeClientAccessor {
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

