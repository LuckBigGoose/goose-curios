package com.luckgoose.goosecurios.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * 组件工具类
 * 
 * 提供通用的Component创建辅助方法
 * 
 * @author luckgoose
 */
public final class ComponentUtils {
    
    private ComponentUtils() {
    }
    
    /**
     * 创建金色文本组件
     * 
     * @param value 文本内容
     * @return 金色样式的Component
     */
    public static Component gold(Object value) {
        return Component.literal(String.valueOf(value))
                        .withStyle(ChatFormatting.GOLD);
    }
    
    /**
     * 创建百分比文本组件（金色）
     * 
     * @param value 数值（0.5表示50%）
     * @return 格式化的百分比文本
     */
    public static Component percent(double value) {
        return gold(String.format(Locale.ROOT, "%.0f", value * 100.0D) + "%");
    }
    
    /**
     * 创建秒数文本组件（金色）
     * 
     * @param ticks 游戏刻数
     * @return 格式化的秒数文本
     */
    public static Component seconds(int ticks) {
        return gold(String.format(Locale.ROOT, "%.2f", ticks / 20.0D));
    }
    
    /**
     * 创建秒数文本组件（金色，double参数）
     * 
     * @param value 数值
     * @return 格式化的秒数文本
     */
    public static Component seconds(double value) {
        return gold(String.format(Locale.ROOT, "%.2f", value));
    }
}
