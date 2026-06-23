package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * 邦德的意志灰度后处理特效
 * 
 * <p>实现时停的视觉效果：降低饱和度、模糊、扭曲
 * 
 * <p>着色器参数动态控制：
 * <ul>
 *   <li>Blur: 模糊强度（0.0 → 0.4，2.5秒过渡）</li>
 *   <li>FallOff: 扭曲衰减（12.0 → 3.0）</li>
 *   <li>Saturation: 饱和度（1.0 → 0.1，接近黑白）</li>
 * </ul>
 * 
 * <p>缓动函数：使用三次缓出（outCubic）实现平滑过渡
 * 
 * @author luckgoose
 */
public final class BondWillGrayPostEffect {
    /** 特效是否激活 */
    private static boolean active;
    
    /** 特效激活的tick数 */
    private static int activeTicks;

    private BondWillGrayPostEffect() {
    }

    /**
     * 清理特效状态
     */
    public static void clear() {
        active = false;
        activeTicks = 0;
        BondWillPostEffectCoordinator.release(Minecraft.getInstance(), BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY);
    }

    /**
     * 每tick更新特效状态
     * 
     * @param shouldBeActive 是否应该激活（时停活跃 && 配置启用）
     */
    public static void tick(boolean shouldBeActive) {
        Minecraft minecraft = Minecraft.getInstance();
        active = shouldBeActive && BondWillClientDisplay.isTimeStopDesaturationEnabled();
        if (active) {
            activeTicks++;
            BondWillPostEffectCoordinator.request(minecraft, BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY);
        } else {
            activeTicks = 0;
            BondWillPostEffectCoordinator.release(minecraft, BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY);
        }
    }

    /**
     * 每帧更新着色器参数
     * 
     * @param partialTick 部分tick（0.0-1.0）
     */
    public static void updateFrame(float partialTick) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BondWillPostEffectCoordinator.request(minecraft, BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY);
        PostChain current = minecraft.gameRenderer.currentEffect();
        if (!BondWillPostEffectCoordinator.isCurrent(minecraft, BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY)) return;
        float elapsedSeconds = (activeTicks + partialTick) / 20.0F;
        float eased = outCubic(Math.min(elapsedSeconds * 2.5F, 1.0F));
        float blur = BondWillClientDisplay.isTimeStopDistortionEnabled() ? Mth.lerp(eased, 0.0F, 0.4F) : 0.0F;
        float fallOff = BondWillClientDisplay.isTimeStopDistortionEnabled() ? Mth.lerp(eased, 12.0F, 3.0F) : 12.0F;
        float saturation = Mth.lerp(eased, 1.0F, 0.1F);
        try {
            for (PostPass pass : getPasses(current)) {
                setUniform(pass, "Blur", blur);
                setUniform(pass, "FallOff", fallOff);
                setUniform(pass, "Saturation", saturation);
            }
        } catch (RuntimeException ignored) {
            // 降级：禁用效果但不崩溃
            BondWillPostEffectCoordinator.release(minecraft, BondWillPostEffectCoordinator.Effect.TIME_STOP_GRAY);
        }
    }

    private static List<PostPass> getPasses(PostChain chain) {
        return ((PostChainAccessor) chain).goose_curios$getPasses();
    }

    private static void setUniform(PostPass pass, String name, float value) {
        Uniform uniform = pass.getEffect().getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static float outCubic(float progress) {
        float inverse = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }
}
