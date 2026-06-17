package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.config.GooseClientConfig;
import com.luckgoose.goosecurios.config.NineCalamitiesConfig;
import com.luckgoose.goosecurios.event.NineCalamitiesEventHandler;
import com.luckgoose.goosecurios.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/**
 * 九魔·九厄客户端显示管理器
 * 
 * <p>负责九魔·九厄饰品的UI显示
 * 
 * <p>显示内容：
 * <ul>
 *   <li>物品提示：按住Shift显示当前魔杖数量和基础加成</li>
 *   <li>HUD显示：持魔杖时显示基础加成、施法加成和总加成</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class NineCalamitiesClientDisplay {

    /**
     * 添加物品提示信息（按住Shift时）
     */
    public static void addTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.NINE_DEMONS_NINE_CALAMITIES.get())) {
            return;
        }
        if (!Screen.hasShiftDown()) {
            return;
        }
        Player player = event.getEntity();
        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        if (player == null) {
            tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.current.unavailable").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int wandCount = NineCalamitiesEventHandler.getWandTypeCount(player);
        double baseBonus = NineCalamitiesEventHandler.getBaseBonusForCount(wandCount);
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.current.wands", wandCount, NineCalamitiesConfig.MAX_WANDS.get(), percent(baseBonus)).withStyle(ChatFormatting.GREEN));
    }

    /**
     * 渲染HUD（持魔杖时显示）
     */
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        if (!NineCalamitiesEventHandler.hasNineCalamitiesRing(player) || !NineCalamitiesEventHandler.isGoetyWand(player.getMainHandItem())) {
            return;
        }
        int wandCount = NineCalamitiesEventHandler.getWandTypeCount(player);
        double baseBonus = NineCalamitiesEventHandler.getBaseBonusForCount(wandCount);
        double castBonus = NineCalamitiesEventHandler.getSingleCastLayerBonus(player);
        int castLayers = NineCalamitiesEventHandler.getCastBonusLayerCount(player);
        double actualCastBonus = NineCalamitiesEventHandler.getCastBonus(player);
        long remainingTicks = NineCalamitiesEventHandler.getCastBonusRemainingTicks(player);
        GuiGraphics graphics = event.getGuiGraphics();
        int x = GooseClientConfig.NINE_CALAMITIES_HUD_X.get();
        int y = GooseClientConfig.NINE_CALAMITIES_HUD_Y.get();
        draw(graphics, Component.translatable("hud.goose_curios.nine_calamities.title").withStyle(ChatFormatting.DARK_PURPLE), x, y);
        y += 10;
        draw(graphics, Component.translatable("hud.goose_curios.nine_calamities.effect1", wandCount, NineCalamitiesConfig.MAX_WANDS.get(), percent(baseBonus)).withStyle(ChatFormatting.GREEN), x, y);
        y += 10;
        if (remainingTicks > 0L) {
            draw(graphics, Component.translatable("hud.goose_curios.nine_calamities.effect2", percent(castBonus), castLayers, seconds(remainingTicks)).withStyle(ChatFormatting.YELLOW), x, y);
        } else {
            draw(graphics, Component.translatable("hud.goose_curios.nine_calamities.effect2_inactive").withStyle(ChatFormatting.GRAY), x, y);
        }
        y += 10;
        draw(graphics, Component.translatable("hud.goose_curios.nine_calamities.total", percent(baseBonus + actualCastBonus)).withStyle(ChatFormatting.LIGHT_PURPLE), x, y);
    }

    private static void draw(GuiGraphics graphics, Component component, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, component, x, y, 0xFFFFFF, true);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f", value * 100.0D);
    }

    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0D);
    }
}

