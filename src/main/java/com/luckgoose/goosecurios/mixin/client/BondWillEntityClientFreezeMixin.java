package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.ClientBondWillFreezeState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 邦德的意志实体客户端冻结Mixin
 * 
 * <p>修改Entity的客户端tick方法，在时停期间阻止实体的客户端更新
 * 
 * @author luckgoose
 */
@Mixin(Entity.class)
public abstract class BondWillEntityClientFreezeMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void goose_curios$cancelBondWillFrozenClientTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        ClientBondWillFreezeState.FrozenSnapshot snapshot = ClientBondWillFreezeState.snapshot(entity);
        if (snapshot != null) {
            snapshot.lockEntity(entity);
            ci.cancel();
        }
    }

    @Inject(method = "lerpTo", at = @At("HEAD"), cancellable = true)
    private void goose_curios$cancelBondWillFrozenLerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport, CallbackInfo ci) {
        if (ClientBondWillFreezeState.isFrozen((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "lerpMotion", at = @At("HEAD"), cancellable = true)
    private void goose_curios$cancelBondWillFrozenLerpMotion(double x, double y, double z, CallbackInfo ci) {
        if (ClientBondWillFreezeState.isFrozen((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}

