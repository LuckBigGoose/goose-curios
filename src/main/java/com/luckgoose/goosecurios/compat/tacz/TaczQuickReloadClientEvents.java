package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GooseCuriosMod.MOD_ID, value = Dist.CLIENT)
public final class TaczQuickReloadClientEvents {

    private TaczQuickReloadClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TaczQuickReload.onClientTick(event);
        BondWillLocalImpactClient.onClientTick(event);
    }
}
