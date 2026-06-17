package com.luckgoose.goosecurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 九魔·九厄配置类
 */
public class NineCalamitiesConfig extends BaseGooseConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_WANDS;
    public static final ForgeConfigSpec.DoubleValue FIRST_WAND_BONUS_PERCENT;
    public static final ForgeConfigSpec.DoubleValue BONUS_DECAY_PER_WAND_PERCENT;
    public static final ForgeConfigSpec.DoubleValue MIN_WAND_BONUS_PERCENT;
    public static final ForgeConfigSpec.DoubleValue CAST_BONUS_PER_WAND_PERCENT;
    public static final ForgeConfigSpec.IntValue CAST_BONUS_DURATION_TICKS;

    static {
        ForgeConfigSpec.Builder builder = createBuilder("nine_demons_nine_calamities");
        
        addComment(builder, "快捷栏中计算的最大魔杖种类数，范围：1-9，推荐：6-9");
        MAX_WANDS = builder.defineInRange("max_wands", 9, 1, 9);
        
        addComment(builder,
            "第一根魔杖的基础加成（百分比），范围：0-50，推荐：5-15",
            "9.0 表示 9% 伤害加成");
        FIRST_WAND_BONUS_PERCENT = builder.defineInRange("first_wand_bonus_percent", 9.0D, 0.0D, 50.0D);
        
        addComment(builder,
            "每增加一根魔杖的加成衰减（百分比），范围：0-10，推荐：0.5-2.0",
            "衰减机制防止无限堆叠魔杖");
        BONUS_DECAY_PER_WAND_PERCENT = builder.defineInRange("bonus_decay_per_wand_percent", 1.0D, 0.0D, 10.0D);
        
        addComment(builder,
            "单根魔杖的最小加成（百分比），范围：0-10，推荐：0.5-2.0",
            "确保即使衰减后每根魔杖仍有最低收益");
        MIN_WAND_BONUS_PERCENT = builder.defineInRange("min_wand_bonus_percent", 1.0D, 0.0D, 10.0D);
        
        addComment(builder,
            "施法加成：每根魔杖贡献的额外加成（百分比），范围：0-10，推荐：0.5-3.0",
            "此加成在施放匹配流派的法术时生效");
        CAST_BONUS_PER_WAND_PERCENT = builder.defineInRange("cast_bonus_per_wand_percent", 1.0D, 0.0D, 10.0D);
        
        addComment(builder,
            "施法加成持续时间（tick），范围：1-72000（1tick-1小时），推荐：60-200（3-10秒）",
            "20 tick = 1 秒");
        CAST_BONUS_DURATION_TICKS = builder.defineInRange("cast_bonus_duration_ticks", 100, 1, 72000);
        
        SPEC = build(builder);
    }
}

