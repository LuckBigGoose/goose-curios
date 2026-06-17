package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/**
 * 邦德的意志后处理特效协调器
 * 
 * <p>管理多个后处理特效的优先级和切换
 * 
 * <p>特效优先级系统：
 * <ul>
 *   <li>TIME_STOP_GRAY（优先级0）：时停灰度特效</li>
 *   <li>PUNCH（优先级1）：射击冲击特效</li>
 * </ul>
 * 
 * <p>工作原理：
 * <ul>
 *   <li>特效请求：通过request()标记特效为需要</li>
 *   <li>特效释放：通过release()取消特效请求</li>
 *   <li>自动切换：根据优先级自动加载最高优先级的特效</li>
 *   <li>冲突处理：同时请求多个特效时，只显示优先级最高的</li>
 * </ul>
 * 
 * @author luckgoose
 */
public final class BondWillPostEffectCoordinator {
    public enum Effect {
        TIME_STOP_GRAY(new ResourceLocation(GooseCuriosMod.MOD_ID, "shaders/post/bond_will_timestop_gray.json"), "bond_will_timestop_gray", 0),
        PUNCH(new ResourceLocation(GooseCuriosMod.MOD_ID, "shaders/post/bond_will_punch.json"), "bond_will_punch", 1);

        private final ResourceLocation shader;
        private final String nameFragment;
        private final int priority;
        private boolean requested;

        Effect(ResourceLocation shader, String nameFragment, int priority) {
            this.shader = shader;
            this.nameFragment = nameFragment;
            this.priority = priority;
        }
    }

    private static Effect loaded;

    private BondWillPostEffectCoordinator() {
    }

    public static void request(Minecraft minecraft, Effect effect) {
        effect.requested = true;
        apply(minecraft);
    }

    public static void release(Minecraft minecraft, Effect effect) {
        effect.requested = false;
        apply(minecraft);
    }

    public static boolean isCurrent(Minecraft minecraft, Effect effect) {
        PostChain current = minecraft.gameRenderer.currentEffect();
        return current != null && current.getName().contains(effect.nameFragment);
    }

    public static void clear(Minecraft minecraft) {
        for (Effect effect : Effect.values()) {
            effect.requested = false;
        }
        PostChain current = minecraft.gameRenderer.currentEffect();
        if (isManaged(current)) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loaded = null;
    }

    private static void apply(Minecraft minecraft) {
        Effect desired = desiredEffect();
        PostChain current = minecraft.gameRenderer.currentEffect();
        if (desired == null) {
            if (isManaged(current)) {
                minecraft.gameRenderer.shutdownEffect();
            }
            loaded = null;
            return;
        }
        if (current != null && current.getName().contains(desired.nameFragment)) {
            loaded = desired;
            return;
        }
        if (isManaged(current)) {
            minecraft.gameRenderer.shutdownEffect();
        }
        minecraft.gameRenderer.loadEffect(desired.shader);
        loaded = desired;
    }

    private static Effect desiredEffect() {
        Effect desired = null;
        for (Effect effect : Effect.values()) {
            if (effect.requested && (desired == null || effect.priority > desired.priority)) {
                desired = effect;
            }
        }
        return desired;
    }

    private static boolean isManaged(PostChain current) {
        if (current == null) {
            return false;
        }
        for (Effect effect : Effect.values()) {
            if (current.getName().contains(effect.nameFragment)) {
                return true;
            }
        }
        return loaded != null;
    }
}
