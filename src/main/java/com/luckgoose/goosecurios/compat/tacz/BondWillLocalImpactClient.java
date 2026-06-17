package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.client.BondWillClientDisplay;
import com.luckgoose.goosecurios.client.BondWillImpactEffects;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

final class BondWillLocalImpactClient {

    private static String lastGunKey;
    private static int lastAmmo = -1;
    private static int cooldownTicks;

    private BondWillLocalImpactClient() {
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator()) {
            reset();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun gun)) {
            reset();
            return;
        }

        String gunKey = gun.getGunId(stack).toString();
        int ammo = getEffectiveAmmo(gun, stack);
        if (!gunKey.equals(lastGunKey)) {
            lastGunKey = gunKey;
            lastAmmo = ammo;
            return;
        }

        if (lastAmmo >= 0 && ammo < lastAmmo && cooldownTicks <= 0 && BondWillClientDisplay.canPlayImpact()) {
            BondWillImpactEffects.play(player.getId(), 1.0F);
            cooldownTicks = 2;
        }
        lastAmmo = ammo;
    }

    private static int getEffectiveAmmo(IGun gun, ItemStack stack) {
        int ammo = gun.getCurrentAmmoCount(stack);
        Bolt boltType = TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                .map(index -> index.getGunData().getBolt())
                .orElse(Bolt.OPEN_BOLT);
        if (boltType != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(stack)) {
            ammo++;
        }
        return ammo;
    }

    private static void reset() {
        lastGunKey = null;
        lastAmmo = -1;
        cooldownTicks = 0;
    }
}

