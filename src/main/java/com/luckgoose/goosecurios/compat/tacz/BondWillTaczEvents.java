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
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
 *   <li>伤害加成应用：首次有效枪械伤害触发，立即清空加成值</li>
 *   <li>时停效果触发：满蓄力时可触发范围时停</li>
 * </ul>
 *
 * @author luckgoose
 * @see BondWillState 状态管理器
 * @see BondWillTimeStopManager 时停管理器
 */
public class BondWillTaczEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(BondWillTaczEvents.class);

    /** 状态同步间隔：每5个tick同步一次，平衡实时性与带宽 */
    private static final int SYNC_INTERVAL_TICKS = 5;

    /** 过期数据清理间隔：每30分钟清理一次 */
    private static final int CLEANUP_INTERVAL_TICKS = 36000;

    /** 过期数据阈值：1小时未活动则清理 */
    private static final int STALE_DATA_THRESHOLD_TICKS = 72000;

    /**
     * 玩家Tick事件：状态更新、加成累积、时停管理
     *
     * 执行流程：
     * 1. 验证玩家是否装备邦德的意志
     * 2. 处理延迟的加成清空（穿甲弹处理完毕后）
     * 3. 判断是否满足隐身条件
     * 4. 更新伤害加成累积值
     * 5. 处理时停效果逻辑
     * 6. 同步状态到客户端
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

        // 处理延迟的加成清空和战斗判定（2 tick后统一处理）
        Integer consumedTick = BondWillState.getBonusConsumedTick(uuid);
        if (consumedTick != null && currentTick - consumedTick >= 2) {
            // 检查是否击杀了目标
            boolean killedTarget = BondWillState.hasKilledRecently(uuid);

            // 清空加成
            BondWillState.clearActiveState(player);

            // 根据击杀状态决定是否标记战斗
            if (!killedTarget) {
                // 未击杀：标记战斗，进入冷却期
                BondWillState.markDamageDealt(uuid, currentTick);
            } else {
                // 击杀：清理所有旧战斗记录，奖励完美刺杀
                BondWillState.clearCombatHistory(uuid);
            }

            // 清理追踪信息
            BondWillState.clearBonusConsumed(uuid);
            BondWillState.clearAttackTracking(uuid);
        }

        if (!canStealth(player, uuid, currentTick)) {
            cancel(player, false);
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
     * 处理生物伤害事件：应用邦德的意志加成或冷却惩罚
     *
     * 此时伤害已经过护甲、抗性等计算，加成为最终独立乘区
     *
     * 逻辑：
     * - 有加成且最终伤害大于0：应用加成伤害，标记消耗（延迟2 tick清空，允许穿甲弹第二段也享受）
     * - 延迟期内：继续应用加成（穿甲弹第二段）
     * - 无加成且战斗中：应用冷却惩罚（-90%伤害）
     * - 最终伤害为0：不消耗加成，不标记战斗
     *
     * @param event 生物伤害事件
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        // 只处理由玩家造成的伤害
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        if (!BondWillState.isEquipped(uuid)) {
            return;
        }

        // 只对枪械伤害生效
        if (!isGunDamage(event)) {
            return;
        }

        int currentTick = player.server.getTickCount();

        // 检查是否在延迟清空期间（穿甲弹第二段）
        Integer consumedTick = BondWillState.getBonusConsumedTick(uuid);
        if (consumedTick != null && currentTick - consumedTick < 2) {
            // 在延迟窗口内，继续应用加成
            double bonus = BondWillState.getBonus(uuid);
            if (bonus > 0.0D) {
                event.setAmount((float) (event.getAmount() * (1.0D + bonus)));
            }
            return; // 不重复处理
        }

        double bonus = BondWillState.getBonus(uuid);
        boolean outOfCombat = BondWillState.isOutOfCombat(uuid, currentTick);

        if (bonus > 0.0D) {
            // 应用伤害加成
            event.setAmount((float) (event.getAmount() * (1.0D + bonus)));
            // 0伤害命中不算暴露：不消耗加成、不追踪目标、不进入战斗。
            if (!hasEffectiveDamage(event)) {
                return;
            }

            // 只在接近满值时触发冲击波特效（进度 >= 0.95）
            double progress = BondWillState.getProgress(uuid);
            if (progress >= 0.95 && event.getEntity() != null) {
                ModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> event.getEntity()),
                    new BondWillImpactPacket(event.getEntity().getId(), (float) progress)
                );
            }

            // 标记正在攻击目标（用于击杀判定）
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                BondWillState.markAttacking(uuid, target.getUUID(), currentTick);
            }

            // 标记加成已消耗，延迟清空（允许穿甲弹第二段）
            BondWillState.markBonusConsumed(uuid, currentTick);
            return;
        }

        if (!outOfCombat) {
            // 冷却期间应用伤害削减
            double multiplier = 1.0D - BondWillConfig.COOLDOWN_DAMAGE_REDUCTION.get();
            event.setAmount((float) (event.getAmount() * multiplier));
        }

        if (!hasEffectiveDamage(event)) {
            return;
        }

        // 再次检查是否在延迟窗口内（避免重复标记战斗）
        if (consumedTick != null && currentTick - consumedTick < 2) {
            // 在延迟窗口内，可能是穿甲弹后续伤害或击杀判定窗口
            // 不标记战斗，等待延迟清空时统一判定击杀状态
            return;
        }

        // 标记造成伤害并取消隐身
        BondWillState.markDamageDealt(uuid, currentTick);
        cancel(player, true);
    }

    private static boolean hasEffectiveDamage(LivingDamageEvent event) {
        return event.getAmount() > 0.0F;
    }

    /**
     * 判断伤害是否由TACZ枪械造成
     *
     * <p>优化版本：使用 ResourceLocation 比较而非字符串，减少 GC 压力
     *
     * @param event 伤害事件
     * @return 是否为枪械伤害
     */
    private static boolean isGunDamage(LivingDamageEvent event) {
        // 检查直接伤害实体是否为 TACZ 弹射物
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null) {
            net.minecraft.resources.ResourceLocation entityTypeId =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
            if (entityTypeId != null && "tacz".equals(entityTypeId.getNamespace())) {
                return true;
            }
        }

        // 检查伤害源类型是否包含枪械相关关键字
        String damageType = event.getSource().getMsgId();
        return damageType != null && (damageType.contains("bullet") || damageType.contains("gun"));
    }

    /**
     * 处理实体死亡事件
     *
     * 用于判定一击必杀：如果玩家击杀了正在攻击的目标，标记为击杀成功
     * 击杀成功的玩家不会进入战斗状态，可以继续连续暗杀
     */
    @SubscribeEvent
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        UUID playerUuid = player.getUUID();
        if (!BondWillState.isEquipped(playerUuid)) {
            return;
        }

        // 检查死亡的实体是否是玩家正在攻击的目标
        UUID targetUuid = event.getEntity().getUUID();
        if (BondWillState.isAttackingTarget(playerUuid, targetUuid)) {
            // 标记击杀成功
            BondWillState.markKilledTarget(playerUuid, targetUuid);
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
