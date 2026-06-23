package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.config.BondWillConfig;
import com.luckgoose.goosecurios.init.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Set;
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
 *   <li>战斗状态：追踪造成有效枪械伤害的时间点</li>
 * </ul>
 * 
 * <p>脱战判定：当前时间 - 最后造成有效枪械伤害时间 &gt; 脱战阈值
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
    
    /** 最后造成有效枪械伤害时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_DAMAGE_DEALT_TICKS = new ConcurrentHashMap<>();
    
    /** 加成消耗时间：玩家UUID → 消耗时的游戏刻（用于延迟清空，允许穿甲弹第二段也享受加成） */
    private static final ConcurrentMap<UUID, Integer> BONUS_CONSUMED_TICK = new ConcurrentHashMap<>();

    /** 加成清空延迟：延迟2 tick清空，允许穿甲弹两段伤害都享受加成 */
    private static final int BONUS_CLEAR_DELAY_TICKS = 2;

    /**
     * 攻击追踪信息
     * 用于判断玩家是否击杀了攻击目标（一击必杀不进战斗）
     */
    private static class AttackTracking {
        /** 正在攻击的目标UUID集合 */
        final Set<UUID> targets = ConcurrentHashMap.newKeySet();
        /** 已击杀的目标UUID集合 */
        final Set<UUID> killedTargets = ConcurrentHashMap.newKeySet();
        /** 攻击开始时间 */
        int startTick;
        
        AttackTracking(int tick) {
            this.startTick = tick;
        }
    }

    /** 攻击追踪：玩家UUID → 攻击追踪信息 */
    private static final ConcurrentMap<UUID, AttackTracking> ATTACK_TRACKING = new ConcurrentHashMap<>();

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
     * 脱战条件：距离最后一次战斗行为的时间超过配置阈值（默认100tick）
     * 战斗行为只包括玩家造成有效TACZ枪械伤害，受到伤害和暴露不会进入战斗。
     * 
     * 使用long类型计算避免tick溢出问题（服务器运行约50天后tick会溢出）
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     * @return 是否脱战
     */
    public static boolean isOutOfCombat(UUID uuid, int currentTick) {
        int lastCombat = getLastCombatTick(uuid);
        long elapsed = (long) currentTick - lastCombat;
        return elapsed > BondWillConfig.OUT_OF_COMBAT_TICKS.get();
    }

    /**
     * 获取脱战冷却剩余时间
     * 
     * 用于客户端HUD显示冷却进度条
     * 使用long类型避免tick溢出
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     * @return 剩余冷却时间（tick），最小为0
     */
    public static int getCooldownTicks(UUID uuid, int currentTick) {
        int lastCombat = getLastCombatTick(uuid);
        long elapsed = (long) currentTick - lastCombat;
        long remaining = BondWillConfig.OUT_OF_COMBAT_TICKS.get() - elapsed + 1;
        return (int) Math.max(0, Math.min(remaining, Integer.MAX_VALUE));
    }

    /** 默认战斗时间戳，确保新玩家立即处于脱战状态 */
    private static final int DEFAULT_COMBAT_TICK = Integer.MIN_VALUE / 2;

    /**
     * 获取最后一次战斗行为的时间
     * 
     * 只取最后一次造成有效枪械伤害的时间戳。
     * 
     * @param uuid 玩家UUID
     * @return 最后战斗时间戳
     */
    private static int getLastCombatTick(UUID uuid) {
        return LAST_DAMAGE_DEALT_TICKS.getOrDefault(uuid, DEFAULT_COMBAT_TICK);
    }

    /**
     * 标记玩家造成有效枪械伤害。
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     */
    public static void markDamageDealt(UUID uuid, int currentTick) {
        LAST_DAMAGE_DEALT_TICKS.put(uuid, currentTick);
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

    /** 隐身效果持续时长：每tick刷新，实际持续取决于隐身条件 */
    private static final int STEALTH_DURATION_TICKS = 6;

    /**
     * 激活隐身效果
     *
     * <p>
     * 施加BondVanishing效果，持续 STEALTH_DURATION_TICKS（6tick = 0.3秒）。
     * 效果会在每tick刷新，因此实际持续时间取决于隐身条件是否满足。
     * </p>
     *
     * @param player 服务器玩家
     */
    public static void activateStealth(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.BOND_VANISHING.get(), STEALTH_DURATION_TICKS, 0, false, false, true));
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
     * 清除隐身效果和伤害加成，但保留装备计数和战斗时间戳
     * 在卸下装备时调用
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
     * 标记加成已消耗
     * 
     * 延迟2 tick清空加成，允许穿甲弹两段伤害都享受加成
     * 
     * @param uuid 玩家UUID
     * @param tick 消耗时的游戏刻
     */
    public static void markBonusConsumed(UUID uuid, int tick) {
        BONUS_CONSUMED_TICK.put(uuid, tick);
    }

    /**
     * 获取加成消耗时间
     * 
     * @param uuid 玩家UUID
     * @return 消耗时的游戏刻，未消耗返回null
     */
    public static Integer getBonusConsumedTick(UUID uuid) {
        return BONUS_CONSUMED_TICK.get(uuid);
    }

    /**
     * 清除加成消耗标记
     * 
     * @param uuid 玩家UUID
     */
    public static void clearBonusConsumed(UUID uuid) {
        BONUS_CONSUMED_TICK.remove(uuid);
    }

    /**
     * 清理战斗历史记录
     * 
     * 在完美击杀后调用，清除所有战斗标记，奖励玩家立即脱战
     * 这是对"刺客"玩法的奖励机制：一击必杀，无痕无踪
     * 
     * @param uuid 玩家UUID
     */
    public static void clearCombatHistory(UUID uuid) {
        LAST_DAMAGE_DEALT_TICKS.remove(uuid);
    }

    /**
     * 标记玩家正在攻击目标
     * 
     * 用于击杀判定：如果目标在2 tick内死亡，视为一击必杀
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标实体UUID
     * @param tick 当前游戏刻
     */
    public static void markAttacking(UUID playerUuid, UUID targetUuid, int tick) {
        AttackTracking tracking = ATTACK_TRACKING.computeIfAbsent(playerUuid, k -> new AttackTracking(tick));
        tracking.targets.add(targetUuid);
    }

    /**
     * 检查玩家是否正在攻击指定目标
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标实体UUID
     * @return 是否正在攻击该目标
     */
    public static boolean isAttackingTarget(UUID playerUuid, UUID targetUuid) {
        AttackTracking tracking = ATTACK_TRACKING.get(playerUuid);
        return tracking != null && tracking.targets.contains(targetUuid);
    }

    /**
     * 标记玩家击杀了目标
     * 
     * 在实体死亡事件中调用
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标实体UUID
     */
    public static void markKilledTarget(UUID playerUuid, UUID targetUuid) {
        AttackTracking tracking = ATTACK_TRACKING.get(playerUuid);
        if (tracking != null) {
            tracking.killedTargets.add(targetUuid);
        }
    }

    /**
     * 检查玩家是否在本次攻击中击杀了目标
     * 
     * @param playerUuid 玩家UUID
     * @return 是否击杀了目标（一击必杀）
     */
    public static boolean hasKilledRecently(UUID playerUuid) {
        AttackTracking tracking = ATTACK_TRACKING.get(playerUuid);
        return tracking != null && !tracking.killedTargets.isEmpty();
    }

    /**
     * 清除攻击追踪信息
     * 
     * 在延迟清空完成后调用
     * 
     * @param playerUuid 玩家UUID
     */
    public static void clearAttackTracking(UUID playerUuid) {
        ATTACK_TRACKING.remove(playerUuid);
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
    }
    
    /**
     * 定期清理过期数据，防止内存泄漏
     * 
     * 清理策略：
     * 1. 清理超过阈值时间未更新的时间戳记录
     * 2. 清理已卸下装备但仍有加成数据的玩家
     * 
     * 调用频率：由 BondWillTaczEvents 每60秒调用一次
     * 阈值默认为5分钟（6000 tick）
     * 
     * @param currentTick 当前游戏刻
     * @param staleThreshold 过期阈值（tick）
     */
    public static void cleanupStaleData(int currentTick, int staleThreshold) {
        // 清理过期的战斗时间戳
        LAST_DAMAGE_DEALT_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        LAST_BONUS_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        
        // 清理过期的加成消耗标记
        BONUS_CONSUMED_TICK.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        
        // 清理过期的攻击追踪
        ATTACK_TRACKING.entrySet().removeIf(
            entry -> currentTick - entry.getValue().startTick > staleThreshold
        );
        
        // 清理没有装备但仍有数据的UUID（玩家已卸下装备）
        BONUSES.keySet().removeIf(uuid -> !EQUIPPED_COUNTS.containsKey(uuid));
    }
}

