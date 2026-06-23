package com.luckgoose.goosecurios.mixin;

import com.luckgoose.goosecurios.util.LovinsWrathRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * lovin的愤怒右键穿甲拦截Mixin
 */
@Mixin(ArmorItem.class)
public class LovinsWrathArmorUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void goose_curios$blockArmorUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (LovinsWrathRules.hasLovinsWrath(player)
                && !LovinsWrathRules.isAllowedArmor(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
