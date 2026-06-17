package com.luckgoose.goosecurios.mixin;

import com.luckgoose.goosecurios.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 邦德的意志容器右键Mixin
 * 
 * <p>拦截对邦德的意志物品的右键点击，防止在容器界面误操作触发设置界面
 * 
 * @author luckgoose
 */
@Mixin(AbstractContainerMenu.class)
public abstract class BondWillContainerRightClickMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void goose_curios$handleBondWillRightClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        // button 1 = 右键, ClickType.PICKUP = 普通点击
        if (button != 1 || clickType != ClickType.PICKUP || slotId < 0) return;
        
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId >= menu.slots.size()) return;
        
        Slot slot = menu.slots.get(slotId);
        ItemStack stack = slot.getItem();
        
        if (stack.is(ModItems.BOND_WILL.get())) {
            ci.cancel();
        }
    }
}

