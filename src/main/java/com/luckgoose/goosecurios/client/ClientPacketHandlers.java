package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.event.NineCalamitiesEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * 客户端数据包处理器
 * 
 * <p>负责处理所有来自服务端的网络数据包
 * 
 * <p>处理的数据包：
 * <ul>
 *   <li>ShortStatusMessagePacket：短消息通知</li>
 *   <li>NineCalamitiesCastBonusSyncPacket：九魔·九厄施法加成同步</li>
 *   <li>BondWillSyncPacket：邦德的意志状态同步</li>
 *   <li>BondWillImpactPacket：邦德的意志冲击特效</li>
 *   <li>BondWillFreezeSyncPacket：邦德的意志冻结同步</li>
 * </ul>
 * 
 * @author luckgoose
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    /**
     * 处理短消息通知
     */
    public static void handleShortStatusMessage(Component message, int durationTicks) {
        ShortStatusMessageClient.show(message, durationTicks);
    }

    /**
     * 处理九魔·九厄施法加成同步
     */
    public static void handleNineCalamitiesCastBonus(long until, List<String> types, long version) {
        if (Minecraft.getInstance().player != null) {
            NineCalamitiesEventHandler.syncCastBonus(Minecraft.getInstance().player, until, types, version);
        }
    }

    /**
     * 处理邦德的意志状态同步
     */
    public static void handleBondWill(float progress, float bonus, float maxBonus, boolean active, boolean equipped, int cooldownTicks, boolean timeStopActive, float timeStopCountdownProgress, CompoundTag settings) {
        BondWillClientDisplay.set(progress, bonus, maxBonus, active, equipped, cooldownTicks, timeStopActive, timeStopCountdownProgress, settings);
    }

    /**
     * 处理邦德的意志冲击特效
     */
    public static void handleBondWillImpact(int entityId, float progress) {
        BondWillImpactEffects.play(entityId, progress);
    }

    /**
     * 处理邦德的意志冻结同步
     */
    public static void handleBondWillFreeze(int entityId, UUID entityUuid, boolean frozen, double x, double y, double z, float xRot, float yRot) {
        ClientBondWillFreezeState.sync(entityId, entityUuid, frozen, x, y, z, xRot, yRot);
    }
}
