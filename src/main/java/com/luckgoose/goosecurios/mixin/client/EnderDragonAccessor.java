package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * EnderDragon访问器
 * 
 * <p>访问末影龙的客户端动画字段，用于时停期间冻结飞行动画
 * 
 * @author luckgoose
 */
@Mixin(EnderDragon.class)
public interface EnderDragonAccessor {
    @Accessor("positions")
    double[][] goose_curios$getPositions();

    @Accessor("positions")
    void goose_curios$setPositions(double[][] positions);

    @Accessor("posPointer")
    int goose_curios$getPosPointer();

    @Accessor("posPointer")
    void goose_curios$setPosPointer(int posPointer);
}

