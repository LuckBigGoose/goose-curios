package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.GooseCuriosMod;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;

import java.util.Locale;

/**
 * 邦德的意志客户端显示管理器
 * 
 * <p>管理客户端UI渲染和状态显示
 * 
 * <p>核心功能：
 * <ul>
 *   <li>状态管理：保存服务端同步的数据</li>
 *   <li>UI渲染：蓄力进度条、状态图标</li>
 *   <li>特效控制：根据设置控制特效开关</li>
 * </ul>
 * 
 * <p>颜色渐变：橙色→金色（4级渐变）
 * 
 * @author luckgoose
 */
public class BondWillClientDisplay {
    /** 进度条颜色数组：从深橙到金色的渐变 */
    private static final int[] PROGRESS_COLORS = {
            0xCC7A4A08,
            0xCCD18A16,
            0xCCE8C15A,
            0xCCFFB02E
    };
    
    /** 满蓄力颜色 */
    private static final int PROGRESS_FULL_COLOR = 0xFFFFE680;
    
    /** 图标材质 */
    private static final ResourceLocation ICON = new ResourceLocation(GooseCuriosMod.MOD_ID, "textures/item/bond_will.png");

    /** 蓄力进度 */
    private static float progress;
    
    /** 伤害加成 */
    private static float bonus;
    
    /** 最大加成 */
    private static float maxBonus;
    
    /** 是否隐身 */
    private static boolean active;
    private static boolean equipped;
    private static int cooldownTicks;
    private static boolean timeStopActive;
    private static float timeStopCountdownProgress;
    private static final ItemStack SETTINGS = new ItemStack(ModItems.BOND_WILL.get());

    public static void set(float value, float valueBonus, float valueMaxBonus, boolean valueActive) {
        set(value, valueBonus, valueMaxBonus, valueActive, valueActive, 0);
    }

    public static void set(float value, float valueBonus, float valueMaxBonus, boolean valueActive, boolean valueEquipped, int valueCooldownTicks) {
        set(value, valueBonus, valueMaxBonus, valueActive, valueEquipped, valueCooldownTicks, false, 0.0F, new CompoundTag());
    }

    public static void set(float value, float valueBonus, float valueMaxBonus, boolean valueActive, boolean valueEquipped, int valueCooldownTicks, boolean valueTimeStopActive, float valueTimeStopCountdownProgress, CompoundTag settings) {
        progress = Math.max(0.0F, Math.min(1.0F, value));
        bonus = Math.max(0.0F, valueBonus);
        maxBonus = Math.max(0.0F, valueMaxBonus);
        active = valueActive;
        equipped = valueEquipped;
        cooldownTicks = Math.max(0, valueCooldownTicks);
        timeStopActive = valueTimeStopActive && equipped && active;
        timeStopCountdownProgress = Math.max(0.0F, Math.min(1.0F, valueTimeStopCountdownProgress));
        
        BondWillSettings.applySettings(SETTINGS, settings);
        
        if (!equipped) {
            BondWillImpactEffects.clear();
            BondWillGrayPostEffect.clear();
            BondWillPostEffectCoordinator.clear(Minecraft.getInstance());
        }
    }
    
    /**
     * 清理客户端状态（饰品摘除时调用）
     */
    public static void clear() {
        progress = 0.0F;
        bonus = 0.0F;
        maxBonus = 0.0F;
        active = false;
        equipped = false;
        cooldownTicks = 0;
        timeStopActive = false;
        timeStopCountdownProgress = 0.0F;
        BondWillImpactEffects.clear();
        BondWillGrayPostEffect.clear();
        BondWillPostEffectCoordinator.clear(Minecraft.getInstance());
    }

    public static ItemStack settingsStack() {
        return SETTINGS;
    }

    public static boolean isShotSoundEnabled() {
        return equipped && BondWillSettings.isShotSoundEnabled(SETTINGS);
    }

    public static boolean isShotEffectEnabled() {
        return equipped && BondWillSettings.isShotEffectEnabled(SETTINGS);
    }

    public static boolean isTimeStopDesaturationEnabled() {
        return equipped && BondWillSettings.isTimeStopDesaturationEnabled(SETTINGS);
    }

    public static boolean isTimeStopDistortionEnabled() {
        return equipped && BondWillSettings.isTimeStopDistortionEnabled(SETTINGS);
    }

    public static boolean isHitboxDisplayEnabled() {
        return equipped && BondWillSettings.isHitboxDisplayEnabled(SETTINGS);
    }

    public static boolean isEquipped() {
        return equipped;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean canPlayImpact() {
        return equipped && active && progress >= 0.999F && isShotEffectEnabled();
    }

    public static boolean isTimeStopActive() {
        return active && timeStopActive && (isTimeStopDesaturationEnabled() || isTimeStopDistortionEnabled());
    }

    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        renderStatus(event, minecraft);
        if (!active) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int barWidth = 74;
        int barHeight = 4;
        int x = width / 2 - barWidth / 2;
        int y = height / 2 + 14;
        float displayProgress = timeStopActive ? timeStopCountdownProgress : progress;
        int filled = Math.round((barWidth - 2) * displayProgress);
        String text = String.format(Locale.ROOT, "%.2f%% / %.2f%%", bonus * 100.0F, maxBonus * 100.0F);
        int textWidth = minecraft.font.width(text);
        int textColor = getProgressColor(displayProgress);

        graphics.fill(x, y, x + barWidth, y + barHeight, 0x44000000);
        graphics.fill(x + 1, y + 1, x + barWidth - 1, y + barHeight - 1, 0x332A2A2A);
        for (int i = 0; i < filled; i++) {
            float localProgress = i / (float) Math.max(1, barWidth - 3);
            graphics.fill(x + 1 + i, y + 1, x + 2 + i, y + barHeight - 1, getProgressColor(timeStopActive ? displayProgress : progress >= 0.999F ? progress : localProgress));
        }
        graphics.drawString(minecraft.font, text, width / 2 - textWidth / 2, y + 7, textColor, true);
    }

    private static void renderStatus(RenderGuiEvent.Post event, Minecraft minecraft) {
        if (!equipped || !TaczClientCompat.mainhandHoldGun(minecraft.player)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        String text = cooldownTicks > 0 ? String.format(Locale.ROOT, "%ds", Math.max(1, (cooldownTicks + 19) / 20)) : Component.translatable("hud.goose_curios.bond_will.ready").getString();
        int x = width - 117;
        int y = height - 58;
        int textColor = cooldownTicks > 0 ? 0xFFFF7777 : 0xFFFFE680;
        graphics.blit(ICON, x, y, 0, 0, 12, 12, 12, 12);
        graphics.drawString(minecraft.font, text, x + 16, y + 2, textColor, true);
    }

    private static int getProgressColor(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        if (clamped >= 0.999F) {
            return PROGRESS_FULL_COLOR;
        }
        float scaled = clamped * (PROGRESS_COLORS.length - 1);
        int index = Math.min((int) scaled, PROGRESS_COLORS.length - 2);
        float local = scaled - index;
        return lerpColor(PROGRESS_COLORS[index], PROGRESS_COLORS[index + 1], local);
    }

    private static int lerpColor(int from, int to, float progress) {
        int alpha = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, progress);
        int red = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, progress);
        int green = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, progress);
        int blue = lerp(from & 0xFF, to & 0xFF, progress);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int lerp(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

}

