package com.luckgoose.goosecurios.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端工具类
 * 
 * 提供客户端专用的辅助方法
 * 
 * @author luckgoose
 */
public final class ClientUtils {
    
    private ClientUtils() {
    }
    
    /**
     * 检测Shift键是否按下
     * 
     * 同时检测左右Shift键，任一按下返回true
     * 
     * @return Shift键是否按下
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isShiftKeyDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
