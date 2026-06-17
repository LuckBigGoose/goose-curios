package com.luckgoose.goosecurios.compat.tacz;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

final class TaczQuickReload {

    private static final int RELOAD_RETRY_COOLDOWN_TICKS = 4;
    private static int reloadRetryCooldown;

    private TaczQuickReload() {
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (reloadRetryCooldown > 0) {
            reloadRetryCooldown--;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (!ShootKey.SHOOT_KEY.isDown()) {
            return;
        }
        if (reloadRetryCooldown > 0) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun gun)) {
            return;
        }

        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        if (gunOperator.getSynReloadState().getStateType().isReloading()) {
            return;
        }
        if (gunOperator.getSynDrawCoolDown() != 0) {
            return;
        }

        int ammoCount = gun.getCurrentAmmoCount(mainHandItem);
        Bolt boltType = TimelessAPI.getCommonGunIndex(gun.getGunId(mainHandItem))
                .map(index -> index.getGunData().getBolt())
                .orElse(Bolt.OPEN_BOLT);
        if (boltType != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(mainHandItem)) {
            ammoCount++;
        }

        if (ammoCount <= 0) {
            IClientPlayerGunOperator.fromLocalPlayer(player).reload();
            reloadRetryCooldown = RELOAD_RETRY_COOLDOWN_TICKS;
        }
    }
}
