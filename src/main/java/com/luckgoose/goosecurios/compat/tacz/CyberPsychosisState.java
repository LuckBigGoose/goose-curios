package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.config.CyberPsychosisConfig;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 赛博精神病全局状态管理器
 * 
 * <p>职责：
 * <ul>
 *   <li>装备状态：记录玩家装备数量</li>
 *   <li>触发概率：记录当前的暴击触发概率</li>
 *   <li>伤害时间：追踪最后造成伤害的时间点</li>
 * </ul>
 * 
 * <p>概率机制：每次造成伤害后增加触发概率，超时后重置
 * 
 * <p>线程安全：使用ConcurrentHashMap保证多线程环境下的数据安全
 * 
 * @author luckgoose
 * @see CyberPsychosisTaczEvents
 */
public class CyberPsychosisState {

    /** 装备计数：玩家UUID → 装备数量 */
    private static final ConcurrentMap<UUID, Integer> EQUIPPED_COUNTS = new ConcurrentHashMap<>();
    
    /** 触发概率：玩家UUID → 概率值(0.0-1.0) */
    private static final ConcurrentMap<UUID, Double> CHANCES = new ConcurrentHashMap<>();
    
    /** 最后造成伤害时间：玩家UUID → 游戏刻 */
    private static final ConcurrentMap<UUID, Integer> LAST_DAMAGE_TICKS = new ConcurrentHashMap<>();

    /** 检查玩家是否装备赛博精神病 */
    public static boolean isEquipped(UUID uuid) {
        return EQUIPPED_COUNTS.getOrDefault(uuid, 0) > 0;
    }

    /**
     * 设置玩家的装备数量，并初始化触发概率
     * 
     * @param uuid 玩家UUID
     * @param count 装备数量，小于等于0时移除记录
     */
    public static void setEquippedCount(UUID uuid, int count) {
        if (count > 0) {
            EQUIPPED_COUNTS.put(uuid, count);
            initChance(uuid);
        } else {
            EQUIPPED_COUNTS.remove(uuid);
        }
    }

    /** 增加装备计数并初始化触发概率 */
    public static void addEquipped(UUID uuid) {
        EQUIPPED_COUNTS.merge(uuid, 1, Integer::sum);
        initChance(uuid);
    }

    /** 减少装备计数，使用原子操作保证线程安全 */
    public static void removeEquipped(UUID uuid) {
        EQUIPPED_COUNTS.compute(uuid, (id, count) -> count == null || count <= 1 ? null : count - 1);
    }

    /** 初始化玩家的触发概率为配置的初始值 */
    public static void initChance(UUID uuid) {
        CHANCES.computeIfAbsent(uuid, id -> getInitialChance());
    }

    /** 获取玩家当前的触发概率 */
    public static double getChance(UUID uuid) {
        return CHANCES.computeIfAbsent(uuid, id -> getInitialChance());
    }

    /**
     * 检查并重置过期的触发概率
     * 
     * <p>如果距离最后造成伤害的时间超过阈值，重置概率为初始值
     * 
     * @param uuid 玩家UUID
     * @param currentTick 当前游戏刻
     */
    public static void resetChanceIfExpired(UUID uuid, int currentTick) {
        Integer lastTick = LAST_DAMAGE_TICKS.get(uuid);
        int resetAfterTicks = CyberPsychosisConfig.RESET_AFTER_TICKS.get();
        if (resetAfterTicks > 0 && lastTick != null && currentTick - lastTick > resetAfterTicks) {
            resetChance(uuid);
        }
    }

    /** 更新玩家最后造成伤害的时间 */
    public static void updateLastDamageTick(UUID uuid, int currentTick) {
        LAST_DAMAGE_TICKS.put(uuid, currentTick);
    }

    /** 重置玩家的触发概率为初始值 */
    public static void resetChance(UUID uuid) {
        CHANCES.put(uuid, getInitialChance());
    }

    /** 增加玩家的触发概率，不超过最大值 */
    public static void increaseChance(UUID uuid) {
        CHANCES.compute(uuid, (id, chance) -> Math.min((chance == null ? getInitialChance() : chance) + CyberPsychosisConfig.CHANCE_INCREMENT.get(), CyberPsychosisConfig.MAX_CHANCE.get()));
    }

    /** 清除玩家的所有状态数据 */
    public static void clear(UUID uuid) {
        EQUIPPED_COUNTS.remove(uuid);
        CHANCES.remove(uuid);
        LAST_DAMAGE_TICKS.remove(uuid);
    }

    /**
     * 清理过期数据（定期清理机制）
     * 
     * 清理超过指定时间未活动的玩家数据，防止长期运行的服务器内存泄漏
     * 
     * @param currentTick 当前游戏刻
     * @param staleThreshold 过期阈值（tick）
     */
    public static void cleanupStaleData(int currentTick, int staleThreshold) {
        // 清理过期的伤害时间戳
        LAST_DAMAGE_TICKS.entrySet().removeIf(
            entry -> currentTick - entry.getValue() > staleThreshold
        );
        
        // 清理没有装备但仍有数据的UUID
        CHANCES.keySet().removeIf(uuid -> !EQUIPPED_COUNTS.containsKey(uuid));
    }

    private static double getInitialChance() {
        return Math.min(CyberPsychosisConfig.INITIAL_CHANCE.get(), CyberPsychosisConfig.MAX_CHANCE.get());
    }
}

