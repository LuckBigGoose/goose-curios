package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillTimeStopManager;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillTimeStopState;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.network.BondWillImpactPacket;
import com.luckgoose.goosecurios.network.BondWillSyncPacket;
import com.luckgoose.goosecurios.network.ModNetwork;
import com.luckgoose.goosecurios.config.BondWillConfig;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.UUID;

/**
 * 邦德的意志物品的TACZ枪械模组兼容事件处理器
 *
 * <p>实现核心游戏机制：
 * <ul>
 *   <li>隐身蓄力系统：脱战状态下累积伤害加成</li>
 *   <li>伤害加成应用：首次伤害触发，立即清空加成值</li>
 *   <li>时停效果触发：满蓄力时可触发范围时停</li>
 * </ul>
 *
 * @author luckgoose
 * @see BondWillState 状态管理器
 * @see BondWillTimeStopManager 时停管理器
 */
public class BondWillTaczEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(BondWillTaczEvents.class);

    /** 状态同步间隔（tick）：每 5 tick 向客户端同步一次，平衡实时性与带宽 */
    private static final int SYNC_INTERVAL_TICKS = 5;

    /** 过期数据清理间隔（tick）：每 30 分钟清理一次（36000 = 20tick/秒 × 60 × 30） */
    private static final int CLEANUP_INTERVAL_TICKS = 36000;

    /** 过期数据阈值（tick）：1 小时未活动则清理（72000 = 20tick/秒 × 60 × 60） */
    private static final int STALE_DATA_THRESHOLD_TICKS = 72000;

    /**
     * 处理玩家每游戏刻的状态更新
     *
     * <p>执行流程：
     * <ol>
     *   <li>验证玩家是否装备邦德的意志</li>
     *   <li>判断是否满足隐身条件</li>
     *   <li>更新伤害加成累积值</li>
     *   <li>处理时停效果逻辑</li>
     *   <li>同步状态到客户端</li>
     * </ol>
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        if (!BondWillState.isEquipped(uuid)) {
            cancel(player, true);
            return;
        }

        int currentTick = player.server.getTickCount();

        if (!canStealth(player, uuid, currentTick)) {
            if (BondWillTimeStopState.isTimeStopActive(uuid)) {
                BondWillState.markRevealed(uuid, currentTick);
            }
            cancel(player, true);
            if (currentTick % SYNC_INTERVAL_TICKS == 0) {
                sync(player, false);
            }
            return;
        }

        BondWillState.activateStealth(player);
        BondWillState.tickBonus(uuid, currentTick);
        tickTimeStop(player, uuid);
        if (currentTick % SYNC_INTERVAL_TICKS == 0) {
            sync(player, true);
        }
    }

    /**
     * 处理枪械伤害事件，应用伤害加成或冷却惩罚
     *
     * <p>逻辑说明：
     * <ul>
     *   <li>有加成：伤害乘以(1 + 加成)，立即清空加成</li>
     *   <li>无加成且战斗中：伤害减少(冷却惩罚)</li>
     * </ul>
     *
     * @param event 枪械伤害事件
     */
    @SubscribeEvent
    public void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (!(event.getAttacker() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        if (!BondWillState.isEquipped(uuid)) {
            return;
        }

        int currentTick = player.server.getTickCount();
        double bonus = BondWillState.getBonus(uuid);
        boolean outOfCombat = BondWillState.isOutOfCombat(uuid, currentTick);

        BondWillState.markDamageDealt(uuid, currentTick);

        if (bonus <= 0.0D) {
            if (!outOfCombat) {
                double multiplier = 1.0D - BondWillConfig.COOLDOWN_DAMAGE_REDUCTION.get();
                event.setBaseAmount((float) (event.getBaseAmount() * multiplier));
            }
            cancel(player, true);
            return;
        }

        event.setBaseAmount((float) (event.getBaseAmount() * (1.0D + bonus)));

        float progress = (float) BondWillState.getProgress(uuid);
        if (progress >= 0.999F) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BondWillImpactPacket(player.getId(), progress));
        }

        cancel(player, true);
    }

    /**
     * 处理生物受伤事件
     *
     * <p>玩家受到伤害或造成近战伤害时，打破隐身状态
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BondWillState.markDamageTaken(player.getUUID(), player.server.getTickCount());
            cancel(player, true);
        }

        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player && BondWillState.isEquipped(player.getUUID())) {
            UUID uuid = player.getUUID();
            int currentTick = player.server.getTickCount();
            BondWillState.markDamageDealt(uuid, currentTick);
            cancel(player, true);
        }
    }

    /**
     * 处理饰品更换事件，更新装备计数
     */
    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean fromBondWill = event.getFrom().is(ModItems.BOND_WILL.get());
        boolean toBondWill = event.getTo().is(ModItems.BOND_WILL.get());
        if (!fromBondWill && !toBondWill) {
            return;
        }

        BondWillState.setEquippedCount(player.getUUID(), countEquipped(player));
        if (!BondWillState.isEquipped(player.getUUID())) {
            cancel(player, true);
        }
    }

    /**
     * 处理玩家切换维度事件，重新统计装备数量
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BondWillState.setEquippedCount(player.getUUID(), countEquipped(player));
            if (!BondWillState.isEquipped(player.getUUID())) {
                cancel(player, true);
            }
        }
    }

    /**
     * 处理玩家登录事件，初始化状态并同步到客户端
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BondWillState.setEquippedCount(player.getUUID(), countEquipped(player));
            sync(player, false);
        }
    }

    /**
     * 处理玩家登出事件，清理所有状态数据
     */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearTimeStop(player);
            BondWillState.clear(player);
        }
    }

    /**
     * 处理服务器Tick事件，定期清理离线玩家的过期数据
     *
     * <p>清理策略：每30分钟清理一次，移除超过1小时未活动的数据
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int currentTick = event.getServer().getTickCount();

        if (currentTick % CLEANUP_INTERVAL_TICKS == 0) {
            BondWillState.cleanupStaleData(currentTick, STALE_DATA_THRESHOLD_TICKS);
            CyberPsychosisState.cleanupStaleData(currentTick, STALE_DATA_THRESHOLD_TICKS);
        }
    }

    /** 判断玩家是否满足隐身条件：持枪、瞄准、脱战 */
    private static boolean canStealth(ServerPlayer player, UUID uuid, int currentTick) {
        return IGun.mainhandHoldGun(player)
                && IGunOperator.fromLivingEntity(player).getSynAimingProgress() >= BondWillConfig.AIMING_PROGRESS_THRESHOLD.get()
                && BondWillState.isOutOfCombat(uuid, currentTick);
    }

    /** 取消隐身、清空加成、清除时停，并同步到客户端 */
    private static void cancel(ServerPlayer player, boolean sync) {
        UUID uuid = player.getUUID();
        boolean shouldSync = BondWillState.isStealthActive(player)
                          || BondWillState.getBonus(uuid) > 0.0D
                          || BondWillTimeStopState.isTimeStopActive(uuid);
        BondWillState.cancelStealth(player);
        BondWillState.clearBonus(uuid);
        clearTimeStop(player);
        if (sync && shouldSync) {
            sync(player, false);
        }
    }

    /** 处理时停效果的每Tick更新逻辑 */
    private static void tickTimeStop(ServerPlayer player, UUID uuid) {
        if (BondWillState.getProgress(uuid) < 1.0D) {
            return;
        }
        if (BondWillTimeStopState.start(uuid)) {
            BondWillTimeStopManager.addOrUpdateInstance(player);
            sync(player, true);
            return;
        }
        BondWillTimeStopManager.addOrUpdateInstance(player);
        if (BondWillTimeStopState.tick(uuid)) {
            BondWillState.markRevealed(uuid, player.server.getTickCount());
            cancel(player, true);
        }
    }

    /** 清除玩家的时停效果 */
    private static void clearTimeStop(ServerPlayer player) {
        BondWillTimeStopState.clear(player.getUUID());
        BondWillTimeStopManager.removeInstance(player);
    }

    /** 向客户端同步当前状态 */
    private static void sync(ServerPlayer player, boolean active) {
        UUID uuid = player.getUUID();
        boolean equipped = BondWillState.isEquipped(uuid);
        int cooldownTicks = equipped ? BondWillState.getCooldownTicks(uuid, player.server.getTickCount()) : 0;
        boolean timeStopActive = BondWillTimeStopState.isTimeStopActive(uuid);
        float countdownProgress = timeStopActive ? BondWillTimeStopState.getCountdownProgress(uuid) : 0.0F;
        ModNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new BondWillSyncPacket(
                (float) BondWillState.getProgress(uuid),
                (float) BondWillState.getBonus(uuid),
                BondWillConfig.MAX_BONUS.get().floatValue(),
                active,
                equipped,
                cooldownTicks,
                timeStopActive,
                countdownProgress,
                getEquippedSettings(player)
            )
        );
    }

    /** 获取玩家装备的邦德的意志物品的设置 */
    private static CompoundTag getEquippedSettings(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
            .resolve()
            .flatMap(handler -> handler.findCurios(ModItems.BOND_WILL.get()).stream().findFirst())
            .map(result -> BondWillSettings.copySettings(result.stack()))
            .orElse(new CompoundTag());
    }

    /** 统计玩家装备的邦德的意志数量 */
    private static int countEquipped(ServerPlayer player) {
        return com.luckgoose.goosecurios.util.CuriosUtils.countEquippedCurios(player, ModItems.BOND_WILL.get());
    }
}
