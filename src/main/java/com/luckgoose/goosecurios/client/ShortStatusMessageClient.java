package com.luckgoose.goosecurios.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 短状态消息客户端显示管理器
 * 
 * <p>用于在客户端显示临时的状态提示消息，支持淡入淡出动画效果
 * 
 * @author luckgoose
 */
public final class ShortStatusMessageClient {

    /** 当前显示的消息内容 */
    private static Component message = Component.empty();
    
    /** 剩余显示时间（游戏刻） */
    private static int ticks;
    
    /** 总显示时间（游戏刻） */
    private static int maxTicks;

    private ShortStatusMessageClient() {
    }

    /**
     * 显示一条短状态消息
     * 
     * @param component 消息内容
     * @param durationTicks 显示持续时间（游戏刻）
     */
    public static void show(Component component, int durationTicks) {
        message = component == null ? Component.empty() : component;
        ticks = Math.max(0, durationTicks);
        maxTicks = ticks;
    }

    /** 检查消息是否正在显示 */
    public static boolean isActive() {
        return ticks > 0 && !message.getString().isEmpty();
    }

    /** 获取当前显示的消息 */
    public static Component getMessage() {
        return message;
    }

    public static int getRemainingTicks() {
        return ticks;
    }

    public static int getMaxTicks() {
        return maxTicks;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || ticks <= 0 || message.getString().isEmpty()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int alpha = ticks < 10 ? Mth.clamp(ticks * 25, 0, 255) : 255;
        int textWidth = minecraft.font.width(message);
        int x = (width - textWidth) / 2;
        int y = height - 68;
        graphics.drawString(minecraft.font, message, x, y, 0xFFFFFF | (alpha << 24), true);
    }

    public static void tick() {
        if (ticks > 0) ticks--;
        if (ticks == 0 && maxTicks > 0) {
            message = Component.empty();
            maxTicks = 0;
        }
    }
}

