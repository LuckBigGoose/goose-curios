package com.luckgoose.goosecurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置类
 * 
 * <p>负责管理所有客户端独有的配置项（UI、渲染、特效等）
 * 
 * @author luckgoose
 */
public class GooseClientConfig {

    public static final ForgeConfigSpec SPEC;
    
    /** 邦德的意志：是否显示爆头准星辅助 */
    public static final ForgeConfigSpec.BooleanValue BOND_WILL_HEADSHOT_GUIDE;
    
    /** 邦德的意志：爆头准星辅助最大距离 */
    public static final ForgeConfigSpec.DoubleValue BOND_WILL_HEADSHOT_GUIDE_MAX_DISTANCE;
    
    /** 九魔·九厄：HUD X坐标 */
    public static final ForgeConfigSpec.IntValue NINE_CALAMITIES_HUD_X;
    
    /** 九魔·九厄：HUD Y坐标 */
    public static final ForgeConfigSpec.IntValue NINE_CALAMITIES_HUD_Y;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("bond_will");
        BOND_WILL_HEADSHOT_GUIDE = builder.define("headshot_guide", true);
        BOND_WILL_HEADSHOT_GUIDE_MAX_DISTANCE = builder.defineInRange("headshot_guide_max_distance", 80.0D, 1.0D, 256.0D);
        builder.pop();
        builder.push("nine_calamities");
        NINE_CALAMITIES_HUD_X = builder.defineInRange("hud_x", 6, 0, 10000);
        NINE_CALAMITIES_HUD_Y = builder.defineInRange("hud_y", 22, 0, 10000);
        builder.pop();
        SPEC = builder.build();
    }
}
