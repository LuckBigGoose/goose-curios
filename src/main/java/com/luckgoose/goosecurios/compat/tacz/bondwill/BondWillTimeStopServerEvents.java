package com.luckgoose.goosecurios.compat.tacz.bondwill;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = GooseCuriosMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BondWillTimeStopServerEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        BondWillTimeStopManager.serverTick(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BondWillTimeStopManager.clear(event.getServer());
    }
}

