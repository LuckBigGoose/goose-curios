package com.luckgoose.goosecurios.mixin.client;

import com.luckgoose.goosecurios.client.BondWillSettingsScreen;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.network.BondWillSettingsUpdatePacket;
import com.luckgoose.goosecurios.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 邦德的意志容器界面右键Mixin
 * 
 * <p>在容器界面右键点击邦德的意志物品时打开设置界面
 * 
 * @author luckgoose
 */
@Mixin(AbstractContainerScreen.class)
public abstract class BondWillContainerScreenRightClickMixin<T extends AbstractContainerMenu> {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void goose_curios$openBondWillSettings(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1 || hoveredSlot == null || !hoveredSlot.hasItem() || !hoveredSlot.getItem().is(ModItems.BOND_WILL.get())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack slotStack = hoveredSlot.getItem();
        minecraft.setScreen(new BondWillSettingsScreen(minecraft.screen, slotStack, configuredStack -> {
            if (hoveredSlot != null && hoveredSlot.hasItem() && hoveredSlot.getItem().is(ModItems.BOND_WILL.get())) {
                BondWillSettings.applySettings(hoveredSlot.getItem(), BondWillSettings.copySettings(configuredStack));
            }
            ModNetwork.CHANNEL.sendToServer(new BondWillSettingsUpdatePacket(BondWillSettings.copySettings(configuredStack)));
        }));
        cir.setReturnValue(true);
    }
}
