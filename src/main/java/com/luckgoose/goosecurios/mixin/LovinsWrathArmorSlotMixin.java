package com.luckgoose.goosecurios.mixin;

import com.luckgoose.goosecurios.util.LovinsWrathRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * lovin的愤怒护甲槽拦截Mixin
 */
@Mixin(targets = "net.minecraft.world.inventory.InventoryMenu$1")
public abstract class LovinsWrathArmorSlotMixin extends Slot {
    private LovinsWrathArmorSlotMixin(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void goose_curios$blockArmorInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inventory
                && LovinsWrathRules.hasLovinsWrath(inventory.player)
                && !LovinsWrathRules.isAllowedArmor(stack)) {
            cir.setReturnValue(false);
        }
    }
}
