package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Warden客户端访问器
 * 
 * <p>访问监守者的客户端动画状态，用于时停期间冻结特殊动画
 * 
 * @author luckgoose
 */
@Mixin(Warden.class)
public interface WardenClientAccessor {
    @Accessor("tendrilAnimation")
    int goose_curios$getTendrilAnimation();

    @Accessor("tendrilAnimationO")
    int goose_curios$getTendrilAnimationO();

    @Accessor("heartAnimation")
    int goose_curios$getHeartAnimation();

    @Accessor("heartAnimationO")
    int goose_curios$getHeartAnimationO();

    @Accessor("tendrilAnimation")
    void goose_curios$setTendrilAnimation(int tendrilAnimation);

    @Accessor("tendrilAnimationO")
    void goose_curios$setTendrilAnimationO(int tendrilAnimationO);

    @Accessor("heartAnimation")
    void goose_curios$setHeartAnimation(int heartAnimation);

    @Accessor("heartAnimationO")
    void goose_curios$setHeartAnimationO(int heartAnimationO);
}

