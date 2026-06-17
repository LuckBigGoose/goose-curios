package com.luckgoose.goosecurios.compat.tacz.bondwill;

import com.luckgoose.goosecurios.config.BondWillConfig;
import net.minecraft.util.Mth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 羁绊意志 - 时停状态管理
 * 
 * 管理每个玩家的时停剩余时间
 */
public final class BondWillTimeStopState {
    private static final ConcurrentMap<UUID, Integer> REMAINING_TICKS = new ConcurrentHashMap<>();

    private BondWillTimeStopState() {
    }

    public static boolean isTimeStopActive(UUID uuid) {
        return REMAINING_TICKS.containsKey(uuid);
    }

    /**
     * 启动时停效果
     * 
     * @param uuid 玩家UUID
     * @return true 表示成功启动，false 表示已经在时停中
     */
    public static boolean start(UUID uuid) {
        if (REMAINING_TICKS.containsKey(uuid)) {
            return false;
        }
        int duration = BondWillConfig.TIME_STOP_DURATION.get();
        REMAINING_TICKS.put(uuid, duration);
        return true;
    }

    /**
     * 每tick调用，减少剩余时间
     * 
     * @param uuid 玩家UUID
     * @return true 表示时停已结束，false 表示仍在持续
     */
    public static boolean tick(UUID uuid) {
        Integer ticks = REMAINING_TICKS.get(uuid);
        if (ticks == null) {
            return false;
        }
        if (ticks <= 1) {
            REMAINING_TICKS.remove(uuid);
            return true;
        }
        REMAINING_TICKS.put(uuid, ticks - 1);
        return false;
    }

    public static void clear(UUID uuid) {
        REMAINING_TICKS.remove(uuid);
    }

    public static int getRemainingTicks(UUID uuid) {
        return REMAINING_TICKS.getOrDefault(uuid, 0);
    }

    /**
     * 获取时停倒计时进度（0.0 = 已结束，1.0 = 刚开始）
     */
    public static float getCountdownProgress(UUID uuid) {
        int duration = BondWillConfig.TIME_STOP_DURATION.get();
        return Mth.clamp(getRemainingTicks(uuid) / (float) duration, 0.0F, 1.0F);
    }
}

