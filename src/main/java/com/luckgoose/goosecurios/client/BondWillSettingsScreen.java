package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.network.BondWillSettingsUpdatePacket;
import com.luckgoose.goosecurios.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 邦德的意志设置界面
 * 
 * <p>提供游戏内UI界面让玩家配置邦德的意志饰品的特效开关
 * 
 * <p>可配置选项：
 * <ul>
 *   <li>时停降饱和度：是否显示时停的灰度效果</li>
 *   <li>时停扭曲：是否显示时停的模糊扭曲效果</li>
 *   <li>射击音效：是否播放射击冲击音效</li>
 *   <li>射击特效：是否显示射击冲击特效</li>
 *   <li>爆头准星：是否显示爆头辅助准星</li>
 * </ul>
 * 
 * <p>界面特点：
 * <ul>
 *   <li>暖色调复古风格</li>
 *   <li>实时保存配置</li>
 *   <li>通过NBT存储在物品上</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class BondWillSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 274;  // 增加高度以容纳第5个选项
    private static final int BORDER = 0xCC2A1810;
    private static final int PANEL = 0xEE1C1512;
    private static final int ROW = 0xFF2D221C;
    private static final int ROW_ALT = 0xFF382A22;
    private static final int ACCENT = 0xFFFFB347;
    private static final int TEXT = 0xFFFFF3D0;
    private static final int MUTED = 0xFFBFAE91;

    private final Screen parent;
    private final ItemStack stack;
    @Nullable
    private final Consumer<ItemStack> changeListener;
    private Button desaturationButton;
    private Button distortionButton;
    private Button shotSoundButton;
    private Button shotEffectButton;
    private Button hitboxDisplayButton;  // 新增碰撞箱按钮

    public BondWillSettingsScreen(Screen parent, ItemStack stack, @Nullable Consumer<ItemStack> changeListener) {
        super(Component.translatable("gui.goose_curios.bond_will_settings.title"));
        this.parent = parent;
        this.stack = stack.copy();
        this.changeListener = changeListener;
        BondWillSettings.writeDefaultsIfMissing(this.stack);
    }

    @Override
    protected void init() {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        desaturationButton = Button.builder(valueLabel(BondWillSettings.isTimeStopDesaturationEnabled(stack)), button -> toggleDesaturation()).bounds(panelX + 190, panelY + 48, 92, 20).build();
        distortionButton = Button.builder(valueLabel(BondWillSettings.isTimeStopDistortionEnabled(stack)), button -> toggleDistortion()).bounds(panelX + 190, panelY + 84, 92, 20).build();
        shotSoundButton = Button.builder(valueLabel(BondWillSettings.isShotSoundEnabled(stack)), button -> toggleShotSound()).bounds(panelX + 190, panelY + 120, 92, 20).build();
        shotEffectButton = Button.builder(valueLabel(BondWillSettings.isShotEffectEnabled(stack)), button -> toggleShotEffect()).bounds(panelX + 190, panelY + 156, 92, 20).build();
        hitboxDisplayButton = Button.builder(valueLabel(BondWillSettings.isHitboxDisplayEnabled(stack)), button -> toggleHitboxDisplay()).bounds(panelX + 190, panelY + 192, 92, 20).build();
        addRenderableWidget(desaturationButton);
        addRenderableWidget(distortionButton);
        addRenderableWidget(shotSoundButton);
        addRenderableWidget(shotEffectButton);
        addRenderableWidget(hitboxDisplayButton);
        addRenderableWidget(Button.builder(Component.translatable("gui.goose_curios.bond_will_settings.done"), button -> onClose()).bounds(panelX + 92, panelY + 238, 136, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, BORDER);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, ACCENT);
        graphics.drawCenteredString(font, title, width / 2, panelY + 16, TEXT);
        drawRow(graphics, panelX + 20, panelY + 42, ROW, Component.translatable("gui.goose_curios.bond_will_settings.desaturation"), Component.translatable("gui.goose_curios.bond_will_settings.desaturation_hint"));
        drawRow(graphics, panelX + 20, panelY + 78, ROW_ALT, Component.translatable("gui.goose_curios.bond_will_settings.distortion"), Component.translatable("gui.goose_curios.bond_will_settings.distortion_hint"));
        drawRow(graphics, panelX + 20, panelY + 114, ROW, Component.translatable("gui.goose_curios.bond_will_settings.shot_sound"), Component.translatable("gui.goose_curios.bond_will_settings.shot_sound_hint"));
        drawRow(graphics, panelX + 20, panelY + 150, ROW_ALT, Component.translatable("gui.goose_curios.bond_will_settings.shot_effect"), Component.translatable("gui.goose_curios.bond_will_settings.shot_effect_hint"));
        drawRow(graphics, panelX + 20, panelY + 186, ROW, Component.translatable("gui.goose_curios.bond_will_settings.hitbox_display"), Component.translatable("gui.goose_curios.bond_will_settings.hitbox_display_hint"));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    public ItemStack getConfiguredStack() {
        return stack.copy();
    }

    private void drawRow(GuiGraphics graphics, int x, int y, int color, Component label, Component hint) {
        graphics.fill(x, y, x + 280, y + 32, color);
        graphics.fill(x, y, x + 3, y + 32, ACCENT);
        graphics.drawString(font, label, x + 12, y + 6, TEXT, false);
        graphics.drawString(font, hint, x + 12, y + 18, MUTED, false);
    }

    private void toggleDesaturation() {
        BondWillSettings.setTimeStopDesaturationEnabled(stack, !BondWillSettings.isTimeStopDesaturationEnabled(stack));
        desaturationButton.setMessage(valueLabel(BondWillSettings.isTimeStopDesaturationEnabled(stack)));
        notifyChanged();
    }

    private void toggleDistortion() {
        BondWillSettings.setTimeStopDistortionEnabled(stack, !BondWillSettings.isTimeStopDistortionEnabled(stack));
        distortionButton.setMessage(valueLabel(BondWillSettings.isTimeStopDistortionEnabled(stack)));
        notifyChanged();
    }

    private void toggleShotSound() {
        BondWillSettings.setShotSoundEnabled(stack, !BondWillSettings.isShotSoundEnabled(stack));
        shotSoundButton.setMessage(valueLabel(BondWillSettings.isShotSoundEnabled(stack)));
        notifyChanged();
    }

    private void toggleShotEffect() {
        BondWillSettings.setShotEffectEnabled(stack, !BondWillSettings.isShotEffectEnabled(stack));
        shotEffectButton.setMessage(valueLabel(BondWillSettings.isShotEffectEnabled(stack)));
        notifyChanged();
    }

    private void toggleHitboxDisplay() {
        BondWillSettings.setHitboxDisplayEnabled(stack, !BondWillSettings.isHitboxDisplayEnabled(stack));
        hitboxDisplayButton.setMessage(valueLabel(BondWillSettings.isHitboxDisplayEnabled(stack)));
        notifyChanged();
    }

    private Component valueLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.goose_curios.bond_will_settings.on" : "gui.goose_curios.bond_will_settings.off");
    }

    private void notifyChanged() {
        if (changeListener != null) {
            changeListener.accept(stack.copy());
        }
        // 发送网络包到服务器保存设置
        ModNetwork.CHANNEL.sendToServer(new BondWillSettingsUpdatePacket(BondWillSettings.copySettings(stack)));
    }
}

