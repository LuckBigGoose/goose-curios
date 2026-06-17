package com.luckgoose.goosecurios.mixin.bondwill;

import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillTimeStopManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 邦德的意志服务端世界实体tick Mixin
 * 
 * <p>修改服务端世界的实体tick方法，在时停期间阻止被冻结实体的服务端更新
 * 
 * @author luckgoose
 */
@Mixin(ServerLevel.class)
public abstract class BondWillServerLevelEntityTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void goose_curios$cancelBondWillFrozenEntityTick(Entity entity, CallbackInfo ci) {
        if (BondWillTimeStopManager.shouldCancelEntityTick(entity)) {
            ci.cancel();
        }
    }
}

