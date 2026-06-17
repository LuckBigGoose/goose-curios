package com.luckgoose.goosecurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 邦德的意志配置类
 */
public class BondWillConfig extends BaseGooseConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BONUS_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MAX_BONUS;
    public static final ForgeConfigSpec.IntValue OUT_OF_COMBAT_TICKS;
    public static final ForgeConfigSpec.DoubleValue AIMING_PROGRESS_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue TIME_STOP_RADIUS;
    public static final ForgeConfigSpec.IntValue TIME_STOP_SCAN_INTERVAL;
    public static final ForgeConfigSpec.IntValue TIME_STOP_DURATION;

    static {
        ForgeConfigSpec.Builder builder = createBuilder("bond_will");
        
        addComment(builder, 
            "伤害加成增长速率（每秒），范围：0.0-1.0，推荐：0.05-0.20",
            "过高的值会导致玩家瞬间达到满加成，破坏游戏平衡");
        BONUS_PER_SECOND = builder.defineInRange("bonusPerSecond", 0.10D, 0.0D, 1.0D);
        
        addComment(builder,
            "最大伤害加成，范围：0.0-5.0，推荐：0.3-1.0",
            "1.0 表示 100% 加成（双倍伤害）");
        MAX_BONUS = builder.defineInRange("maxBonus", 0.50D, 0.0D, 5.0D);
        
        addComment(builder,
            "脱离战斗所需时间（tick），范围：0-6000（0-5分钟），推荐：60-200（3-10秒）",
            "20 tick = 1 秒");
        OUT_OF_COMBAT_TICKS = builder.defineInRange("outOfCombatTicks", 100, 0, 6000);
        
        addComment(builder,
            "触发隐身所需的瞄准进度，范围：0.0-1.0，推荐：0.8-0.95",
            "0.0 = 不瞄准也可触发，1.0 = 必须完全瞄准");
        AIMING_PROGRESS_THRESHOLD = builder.defineInRange("aimingProgressThreshold", 0.90D, 0.0D, 1.0D);
        
        addComment(builder,
            "冷却期间的伤害削减比例，范围：0.0-1.0，推荐：0.5-0.9",
            "0.9 = 削减 90% 伤害（仅造成 10% 伤害）");
        COOLDOWN_DAMAGE_REDUCTION = builder.defineInRange("cooldownDamageReduction", 0.90D, 0.0D, 1.0D);
        
        addComment(builder,
            "时停效果半径（方块），范围：8.0-128.0，推荐：30.0-64.0",
            "30.0 表示以玩家为中心 30 格半径内的实体会被冻结",
            "较大的值会增加性能消耗");
        TIME_STOP_RADIUS = builder.defineInRange("timeStopRadius", 30.0D, 8.0D, 128.0D);
        
        addComment(builder,
            "时停扫描间隔（tick），范围：1-20，推荐：4-8",
            "4 表示每 0.2 秒扫描一次（20 tick = 1 秒）",
            "较小的值更精确但消耗更多性能，较大的值更节省性能");
        TIME_STOP_SCAN_INTERVAL = builder.defineInRange("timeStopScanInterval", 4, 1, 20);
        
        addComment(builder,
            "时停持续时间（tick），范围：20-1200（1-60秒），推荐：100-400（5-20秒）",
            "200 表示时停持续 10 秒（20 tick = 1 秒）",
            "达到满伤害加成后可触发时停效果");
        TIME_STOP_DURATION = builder.defineInRange("timeStopDuration", 200, 20, 1200);
        
        SPEC = build(builder);
    }
}

