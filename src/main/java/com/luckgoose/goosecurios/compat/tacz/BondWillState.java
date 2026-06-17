package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.config.BondWillConfig;
import com.luckgoose.goosecurios.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 邦德的意志全局状态管理器
 * 
 * <p>职责：
 * <ul>
 *   <li>装备状态：记录玩家装备数量</li>
 *   <li>伤害加成：记录当前累积的加成百分比</li>
 *   <li>战斗状态：追踪造成伤害、受到伤害、暴露的时间点</li>
 * </ul>
 * 
 * <p>脱战判定：当前时间 - max(最后伤害时间, 最后受伤时间, 最后暴露时间) &gt; 脱战阈值
 * 
 * <p>线程安全：使用ConcurrentHashMap保证多线程环境下的数据安全
 * 
 * @author luckgoose
 * @see BondWillTaczEvents
 */
public class BondWillState {

    /** 装备计数：玩家UUID → 装备数量 */
    private static final ConcurrentMap<UUID, Integer> EQUIPPED_COUNTS = new ConcurrentHashMap<>();
    
    /** 伤害加成：玩家UUID → 加成百分比(0.0-1.0) */
    private static final ConcurrentMap<UUID, Double> BONUSES = new ConcurrentHashMap<>();
    
    /** 加成更新时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_BONUS_TICKS = new ConcurrentHashMap<>();
    
    /** 最后造成伤害时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_DAMAGE_DEALT_TICKS = new ConcurrentHashMap<>();
    
    /** 最后受到伤害时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_DAMAGE_TAKEN_TICKS = new ConcurrentHashMap<>();
    
    /** 最后暴露时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_REVEAL_TICKS = new ConcurrentHashMap<>();

    /** 检查玩家是否装备邦德的意志 */
    public static boolean isEquipped(UUID uuid) {
        return EQUIPPED_COUNTS.getOrDefault(uuid, 0) > 0;
    }

    /**
     * 设置玩家的装备数量
     * 
     * @param uuid 玩家UUID
     * @param count 装备数量，小于等于0时移除记录
     */
    public static void setEquippedCount(UUID uuid, int count) {
        if (count > 0) {
            EQUIPPED_COUNTS.put(uuid, count);
        } else {
            EQUIPPED_COUNTS.remove(uuid);
        }
    }

    /** 增加装备计数，使用原子操作保证线程安全 */
    public static void addEquipped(UUID uuid) {
        EQUIPPED_COUNTS.merge(uuid, 1, Integer::sum);
    }

    /**
     * 减少装备计数，使用原子操作保证线程安全
     * 
     * <p>当计数减至0时自动移除记录以节省内存
     * 
     * @param uuid 玩家UUID
     */
    public static void removeEquipped(UUID uuid) {
        EQUIPPED_COUNTS.compute(uuid, (id, count) -> count == null || count <= 1 ? null : count - 1);
    }

    /**
     * 判断玩家是否脱离战斗
     * 
     * 【脱战条件】
     * 距离最后一次战斗行为的时间 > OUT_OF_COMBAT_TICKS（默认200tick = 10秒）
     * 
     * 战斗行为包括：
     * - 造成伤害
     * - 受到伤害
     * - 被"暴露"（时停结束、伤害窗口超时等）
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     * @return 是否脱战
     */
    public static boolean isOutOfCombat(UUID uuid, int currentTick) {
        return currentTick - getLastCombatTick(uuid) > BondWillConfig.OUT_OF_COMBAT_TICKS.get();
    }

    /**
     * 获取脱战冷却剩余时间
     * 
     * 【用途】
     * 用于客户端HUD显示冷却进度条
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     * @return 剩余冷却时间（tick），最小为0
     */
    public static int getCooldownTicks(UUID uuid, int currentTick) {
        int lastCombat = getLastCombatTick(uuid);
        int elapsed = currentTick - lastCombat;
        return Math.max(0, BondWillConfig.OUT_OF_COMBAT_TICKS.get() - elapsed + 1);
    }

    /**
     * 获取最后一次战斗行为的时间
     * 
     * 取三个时间戳的最大值，使用 Integer.MIN_VALUE / 2 作为默认值
     * 避免使用 Integer.MIN_VALUE 防止后续计算溢出
     */
    private static int getLastCombatTick(UUID uuid) {
        int lastDealt = LAST_DAMAGE_DEALT_TICKS.getOrDefault(uuid, Integer.MIN_VALUE / 2);
        int lastTaken = LAST_DAMAGE_TAKEN_TICKS.getOrDefault(uuid, Integer.MIN_VALUE / 2);
        int lastReveal = LAST_REVEAL_TICKS.getOrDefault(uuid, Integer.MIN_VALUE / 2);
        return Math.max(Math.max(lastDealt, lastTaken), lastReveal);
    }

    /**
     * 标记玩家造成伤害
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     */
    public static void markDamageDealt(UUID uuid, int currentTick) {
        LAST_DAMAGE_DEALT_TICKS.put(uuid, currentTick);
    }

    /**
     * 标记玩家受到伤害
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     */
    public static void markDamageTaken(UUID uuid, int currentTick) {
        LAST_DAMAGE_TAKEN_TICKS.put(uuid, currentTick);
    }

    /**
     * 标记玩家被"暴露"
     * 
     * <p>
     * "暴露"发生在以下情况：
     * <ul>
     *   <li>时停效果结束</li>
     *   <li>伤害窗口超时</li>
     * </ul>
     * </p>
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     */
    public static void markRevealed(UUID uuid, int currentTick) {
        LAST_REVEAL_TICKS.put(uuid, currentTick);
    }

    /**
     * 获取当前伤害加成值
     * 
     * @param uuid 玩家UUID
     * @return 伤害加成值（0.0到MAX_BONUS）
     */
    public static double getBonus(UUID uuid) {
        return BONUSES.getOrDefault(uuid, 0.0D);
    }

    /**
     * 获取加成进度（0.0到1.0）
     * 
     * <p>
     * 进度 = 当前加成 / 最大加成
     * 用于判断是否达到时停条件和客户端进度条显示。
     * </p>
     * 
     * @param uuid 玩家UUID
     * @return 加成进度（0.0到1.0）
     */
    public static double getProgress(UUID uuid) {
        double max = BondWillConfig.MAX_BONUS.get();
        return max <= 0.0D ? 0.0D : Math.min(1.0D, getBonus(uuid) / max);
    }

    /**
     * 更新伤害加成（每tick调用）
     * 每20tick增加一次，支持跳帧，使用原子操作保证线程安全
     */
    public static double tickBonus(UUID uuid, int currentTick) {
        int lastTick = LAST_BONUS_TICKS.computeIfAbsent(uuid, k -> currentTick);
        
        if (lastTick == currentTick) {
            return getBonus(uuid);
        }
        
        if (currentTick - lastTick >= 20) {
            int steps = (currentTick - lastTick) / 20;
            
            double newBonus = BONUSES.compute(uuid, (k, oldBonus) -> {
                double current = oldBonus != null ? oldBonus : 0.0;
                return Math.min(current + BondWillConfig.BONUS_PER_SECOND.get() * steps, 
                              BondWillConfig.MAX_BONUS.get());
            });
            
            LAST_BONUS_TICKS.put(uuid, lastTick + steps * 20);
            return newBonus;
        }
        return getBonus(uuid);
    }

    /**
     * 清除伤害加成
     * 
     * <p>
     * 清除加成值和加成更新时间戳。
     * 在取消隐身、卸下装备时调用。
     * </p>
     * 
     * @param uuid 玩家UUID
     */
    public static void clearBonus(UUID uuid) {
        BONUSES.remove(uuid);
        LAST_BONUS_TICKS.remove(uuid);
    }

    /**
     * 检查玩家是否处于隐身状态
     * 
     * @param player 服务器玩家
     * @return 是否有BondVanishing效果
     */
    public static boolean isStealthActive(ServerPlayer player) {
        return player.hasEffect(ModEffects.BOND_VANISHING.get());
    }

    /**
     * 激活隐身效果
     * 
     * <p>
     * 施加BondVanishing效果，持续6tick（0.3秒）。
     * 效果会在每tick刷新，因此实际持续时间取决于隐身条件是否满足。
     * </p>
     * 
     * @param player 服务器玩家
     */
    public static void activateStealth(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.BOND_VANISHING.get(), 6, 0, false, false, true));
    }

    /**
     * 取消隐身效果
     * 
     * @param player 服务器玩家
     */
    public static void cancelStealth(ServerPlayer player) {
        player.removeEffect(ModEffects.BOND_VANISHING.get());
    }

    /**
     * 清除玩家的激活状态
     * 
     * <p>
     * 清除隐身效果和伤害加成，但保留装备计数和战斗时间戳。
     * 在卸下装备时调用。
     * </p>
     * 
     * @param player 服务器玩家
     */
    public static void clearActiveState(ServerPlayer player) {
        UUID uuid = player.getUUID();
        cancelStealth(player);
        BONUSES.remove(uuid);
        LAST_BONUS_TICKS.remove(uuid);
    }

    /**
     * 清除玩家的所有数据
     * 
     * <p>
     * 清除装备计数、激活状态、战斗时间戳。
     * 在玩家登出时调用，完全移除该玩家的数据。
     * </p>
     * 
     * @param player 服务器玩家
     */
    public static void clear(ServerPlayer player) {
        UUID uuid = player.getUUID();
        clearActiveState(player);
        EQUIPPED_COUNTS.remove(uuid);
        LAST_DAMAGE_DEALT_TICKS.remove(uuid);
        LAST_DAMAGE_TAKEN_TICKS.remove(uuid);
        LAST_REVEAL_TICKS.remove(uuid);
    }
    
    /**
     * 定期清理过期数据，防止内存泄漏
     * 
     * 【清理策略】
     * 1. 清理超过阈值时间未更新的时间戳记录
     * 2. 清理已卸下装备但仍有加成数据的玩家
     * 
     * 【调用频率】
     * 由 BondWillTaczEvents 每60秒调用一次
     * 阈值默认为5分钟（6000 tick）
     * 
     * 【原因】
     * 如果不清理，离线玩家的数据会永久占用内存，
     * 长时间运行的服务器会逐渐消耗大量内存
     * 
     * @param currentTick 当前游戏刻
     * @param staleThreshold 过期阈值（tick）
     */
    public static void cleanupStaleData(int currentTick, int staleThreshold) {
        // 清理过期的战斗时间戳
        LAST_DAMAGE_DEALT_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        LAST_DAMAGE_TAKEN_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        LAST_REVEAL_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        LAST_BONUS_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        // 清理没有装备但仍有数据的UUID（玩家已卸下装备）
        BONUSES.keySet().removeIf(uuid -> !EQUIPPED_COUNTS.containsKey(uuid));
    }
}

