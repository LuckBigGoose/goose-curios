package com.luckgoose.goosecurios.mixin;

import com.luckgoose.goosecurios.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 邦德的意志隐身效果Mixin
 * 
 * <p>修改 LivingEntity.canBeSeenByAnyone 方法，使拥有 BondVanishing 效果的实体对所有人不可见
 * 
 * @author luckgoose
 */
@Mixin(LivingEntity.class)
public abstract class BondVanishingVisibilityMixin {

    @Inject(method = "canBeSeenByAnyone", at = @At("HEAD"), cancellable = true)
    private void goose_curios$hideBondVanishing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasEffect(ModEffects.BOND_VANISHING.get())) {
            cir.setReturnValue(false);
        }
    }
}

