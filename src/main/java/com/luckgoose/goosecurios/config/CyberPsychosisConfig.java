package com.luckgoose.goosecurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 赛博精神病配置类
 */
public class CyberPsychosisConfig extends BaseGooseConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue INITIAL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CHANCE_INCREMENT;
    public static final ForgeConfigSpec.DoubleValue MAX_CHANCE;
    public static final ForgeConfigSpec.IntValue RESET_AFTER_TICKS;

    static {
        ForgeConfigSpec.Builder builder = createBuilder("cyber_psychosis");
        
        addComment(builder,
            "初始爆头概率，范围：0.0-1.0，推荐：0.3-0.6",
            "0.50 表示 50% 的初始爆头率");
        INITIAL_CHANCE = builder.defineInRange("initialChance", 0.50D, 0.0D, 1.0D);
        
        addComment(builder,
            "每次攻击增加的爆头概率（未爆头时），范围：0.0-0.5，推荐：0.05-0.15",
            "0.10 表示每次未爆头的攻击增加 10% 爆头率");
        CHANCE_INCREMENT = builder.defineInRange("chanceIncrement", 0.10D, 0.0D, 0.5D);
        
        addComment(builder,
            "最大爆头概率，范围：0.0-1.0，推荐：0.6-1.0",
            "0.80 表示最高 80% 爆头率");
        MAX_CHANCE = builder.defineInRange("maxChance", 0.80D, 0.0D, 1.0D);
        
        addComment(builder,
            "累计概率重置时间（tick），范围：0-6000，推荐：100-400（5-20秒）",
            "20 tick = 1 秒，超过此时间未攻击则重置爆头率");
        RESET_AFTER_TICKS = builder.defineInRange("resetAfterTicks", 200, 0, 6000);
        
        SPEC = build(builder);
    }
}

