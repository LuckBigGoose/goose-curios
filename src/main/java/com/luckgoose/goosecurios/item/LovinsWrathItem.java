package com.luckgoose.goosecurios.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.luckgoose.goosecurios.config.LovinsWrathConfig;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.util.ClientUtils;
import com.luckgoose.goosecurios.util.LovinsWrathRules;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * lovin的愤怒 - 限甲、限饰品、限武器并随击杀成长的契约饰品。
 */
public class LovinsWrathItem extends Item implements ICurioItem {
    public static final String NBT_KILL_POINTS = "LovinsWrathKillPoints";
    public static final String NBT_KILLS = "LovinsWrathKills";

    public LovinsWrathItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.DARK_RED);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide()) {
            LovinsWrathRules.dropOtherCurios(slotContext.entity(), slotContext.identifier(), slotContext.index());
            LovinsWrathRules.dropInvalidArmor(slotContext.entity());
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return LovinsWrathRules.RING_SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        int points = getKillPoints(stack);
        for (LovinsWrathConfig.ConfiguredAttribute attribute : LovinsWrathConfig.attributes()) {
            modifiers.put(attribute.attribute(), attribute.modifier(points));
        }
        return modifiers;
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        return LovinsWrathConfig.attributeBonus().mode() == LovinsWrathConfig.Mode.ALL
                ? List.of()
                : tooltips;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.lore.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.lore.2"));
        tooltip.add(Component.empty());

        if (level != null && level.isClientSide && isShiftKeyDownClient()) {
            addDetailedEffects(stack, tooltip);
            return;
        }

        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.3"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.4"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.hold_shift"));
    }

    private static boolean isShiftKeyDownClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
                && ClientUtils.isShiftKeyDown();
    }

    private void addDetailedEffects(ItemStack stack, List<Component> tooltip) {
        int points = getKillPoints(stack);

        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.1.detail"));
        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.2.detail1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.2.detail2"));
        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.3"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.3.detail"));
        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.4"));
        tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.ability.4.detail", getKills(stack), points));
        LovinsWrathConfig.AttributeBonus bonus = LovinsWrathConfig.attributeBonus();
        if (bonus.mode() == LovinsWrathConfig.Mode.ALL) {
            LovinsWrathConfig.AttributeScale scale = bonus.allScale();
            tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.growth.all_attributes",
                    scale.format(scale.value(points)),
                    limitText(scale),
                    scale.format(scale.basePercent()),
                    points,
                    scale.format(scale.perWrathPercent())).withStyle(ChatFormatting.RED));
        } else {
            for (LovinsWrathConfig.ConfiguredAttribute attribute : bonus.attributes()) {
                LovinsWrathConfig.AttributeScale scale = attribute.scale();
                tooltip.add(Component.translatable("tooltip.goose_curios.lovins_wrath.growth.attribute",
                        attribute.label(),
                        attribute.format(attribute.displayValue(points)),
                        limitText(scale),
                        attribute.format(scale.basePercent()),
                        points,
                        attribute.format(scale.perWrathPercent())).withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public static boolean isLovinsWrath(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.LOVINS_WRATH.get());
    }

    public static int getKills(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(NBT_KILLS);
    }

    public static int getKillPoints(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KILL_POINTS, Tag.TAG_INT)) {
            return LovinsWrathConfig.initialWrath();
        }
        return tag.getInt(NBT_KILL_POINTS);
    }

    public static void addKillPoints(ItemStack stack, int points) {
        if (points <= 0) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_KILLS, getKills(stack) + 1);
        tag.putInt(NBT_KILL_POINTS, getKillPoints(stack) + points);
    }

    private static Component limitText(LovinsWrathConfig.AttributeScale scale) {
        return scale.hasLimit()
                ? Component.literal(scale.formatLimit())
                : Component.translatable("tooltip.goose_curios.lovins_wrath.growth.unlimited");
    }
}
