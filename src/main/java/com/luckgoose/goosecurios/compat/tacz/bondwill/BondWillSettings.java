package com.luckgoose.goosecurios.compat.tacz.bondwill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 邦德的意志设置管理器
 * 
 * <p>管理邦德的意志饰品的客户端特效配置
 * 
 * <p>配置项：
 * <ul>
 *   <li>TimeStopDesaturation：时停降饱和度（灰度效果）</li>
 *   <li>TimeStopDistortion：时停扭曲（模糊效果）</li>
 *   <li>ShotSound：射击冲击音效</li>
 *   <li>ShotEffect：射击冲击特效</li>
 *   <li>HitboxDisplay：爆头准星辅助</li>
 * </ul>
 * 
 * <p>存储方式：通过NBT存储在ItemStack上，每个物品独立配置
 * 
 * @author luckgoose
 */
public final class BondWillSettings {
    public static final String ROOT = "BondWillSettings";
    public static final String TIME_STOP_DESATURATION = "TimeStopDesaturation";
    public static final String TIME_STOP_DISTORTION = "TimeStopDistortion";
    public static final String SHOT_SOUND = "ShotSound";
    public static final String SHOT_EFFECT = "ShotEffect";
    public static final String HITBOX_DISPLAY = "HitboxDisplay";

    private BondWillSettings() {
    }

    public static boolean isTimeStopDesaturationEnabled(ItemStack stack) {
        return getBoolean(stack, TIME_STOP_DESATURATION);
    }

    public static boolean isTimeStopDistortionEnabled(ItemStack stack) {
        return getBoolean(stack, TIME_STOP_DISTORTION);
    }

    public static boolean isShotSoundEnabled(ItemStack stack) {
        return getBoolean(stack, SHOT_SOUND);
    }

    public static boolean isShotEffectEnabled(ItemStack stack) {
        return getBoolean(stack, SHOT_EFFECT);
    }

    public static boolean isHitboxDisplayEnabled(ItemStack stack) {
        return getBoolean(stack, HITBOX_DISPLAY);
    }

    public static void setTimeStopDesaturationEnabled(ItemStack stack, boolean enabled) {
        setBoolean(stack, TIME_STOP_DESATURATION, enabled);
    }

    public static void setTimeStopDistortionEnabled(ItemStack stack, boolean enabled) {
        setBoolean(stack, TIME_STOP_DISTORTION, enabled);
    }

    public static void setShotSoundEnabled(ItemStack stack, boolean enabled) {
        setBoolean(stack, SHOT_SOUND, enabled);
    }

    public static void setShotEffectEnabled(ItemStack stack, boolean enabled) {
        setBoolean(stack, SHOT_EFFECT, enabled);
    }

    public static void setHitboxDisplayEnabled(ItemStack stack, boolean enabled) {
        setBoolean(stack, HITBOX_DISPLAY, enabled);
    }

    public static void writeDefaultsIfMissing(ItemStack stack) {
        CompoundTag settings = getOrCreateSettings(stack);
        putDefault(settings, TIME_STOP_DESATURATION);
        putDefault(settings, TIME_STOP_DISTORTION);
        putDefault(settings, SHOT_SOUND);
        putDefault(settings, SHOT_EFFECT);
        putDefaultFalse(settings, HITBOX_DISPLAY);
    }

    public static CompoundTag copySettings(ItemStack stack) {
        writeDefaultsIfMissing(stack);
        return getOrCreateSettings(stack).copy();
    }

    public static void applySettings(ItemStack stack, CompoundTag settings) {
        // 修复：处理嵌套的BondWillSettings结构
        CompoundTag source = settings.contains(ROOT) ? settings.getCompound(ROOT) : settings;
        
        CompoundTag target = getOrCreateSettings(stack);
        target.putBoolean(TIME_STOP_DESATURATION, readBoolean(source, TIME_STOP_DESATURATION));
        target.putBoolean(TIME_STOP_DISTORTION, readBoolean(source, TIME_STOP_DISTORTION));
        target.putBoolean(SHOT_SOUND, readBoolean(source, SHOT_SOUND));
        target.putBoolean(SHOT_EFFECT, readBoolean(source, SHOT_EFFECT));
        target.putBoolean(HITBOX_DISPLAY, readBooleanDefaultFalse(source, HITBOX_DISPLAY));
    }

    private static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag settings = getOrCreateSettings(stack);
        putDefault(settings, key);
        return settings.getBoolean(key);
    }

    private static void setBoolean(ItemStack stack, String key, boolean enabled) {
        getOrCreateSettings(stack).putBoolean(key, enabled);
    }

    private static CompoundTag getOrCreateSettings(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            tag.put(ROOT, new CompoundTag());
        }
        return tag.getCompound(ROOT);
    }

    private static void putDefault(CompoundTag settings, String key) {
        if (!settings.contains(key)) {
            settings.putBoolean(key, true);
        }
    }

    private static void putDefaultFalse(CompoundTag settings, String key) {
        if (!settings.contains(key)) {
            settings.putBoolean(key, false);
        }
    }

    private static boolean readBoolean(CompoundTag settings, String key) {
        return !settings.contains(key) || settings.getBoolean(key);
    }

    private static boolean readBooleanDefaultFalse(CompoundTag settings, String key) {
        return settings.contains(key) && settings.getBoolean(key);
    }
}

