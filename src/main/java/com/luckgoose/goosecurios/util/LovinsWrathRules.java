package com.luckgoose.goosecurios.util;

import com.luckgoose.goosecurios.config.LovinsWrathConfig;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.item.LovinsWrathItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

/**
 * lovin的愤怒的通用规则。
 *
 * <p>物品、事件和Mixin统一调用此类，避免契约规则分散。
 */
public class LovinsWrathRules {
    public static final String RING_SLOT = "ring";

    public static boolean hasLovinsWrath(LivingEntity entity) {
        return getLovinsWrath(entity).isPresent();
    }

    public static Optional<SlotResult> getLovinsWrath(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .flatMap(handler -> handler.findFirstCurio(ModItems.LOVINS_WRATH.get()));
    }

    public static boolean isAllowedCurio(ItemStack stack) {
        return LovinsWrathItem.isLovinsWrath(stack) || LovinsWrathConfig.isAllowedCurio(stack);
    }

    public static boolean isAllowedWeapon(ItemStack stack) {
        return LovinsWrathConfig.isAllowedWeapon(stack);
    }

    public static boolean isAllowedArmor(ItemStack stack) {
        return LovinsWrathConfig.isAllowedArmor(stack);
    }

    public static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    public static boolean blocksArmor(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return isArmorSlot(slot) && hasLovinsWrath(entity) && !isAllowedArmor(stack);
    }

    public static void dropOtherCurios(LivingEntity entity, String currentIdentifier, int currentIndex) {
        if (entity.level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(entity).resolve().ifPresent(handler -> dropOtherCurios(handler, currentIdentifier, currentIndex));
    }

    public static void dropInvalidCurioSlot(LivingEntity entity, String identifier, int index) {
        if (entity.level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.getStacksHandler(identifier))
                .ifPresent(stacksHandler -> dropCurioSlot(entity, stacksHandler, index));
    }

    public static void dropInvalidArmor(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!isArmorSlot(slot)) {
                continue;
            }
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (isAllowedArmor(stack)) {
                continue;
            }
            entity.setItemSlot(slot, ItemStack.EMPTY);
            drop(entity, stack);
        }
    }

    public static void dropArmorSlot(LivingEntity entity, EquipmentSlot slot) {
        if (entity.level().isClientSide() || !isArmorSlot(slot)) {
            return;
        }
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty() || isAllowedArmor(stack)) {
            return;
        }
        entity.setItemSlot(slot, ItemStack.EMPTY);
        drop(entity, stack);
    }

    public static void drop(LivingEntity entity, ItemStack stack) {
        if (!stack.isEmpty()) {
            entity.spawnAtLocation(stack.copy());
        }
    }

    private static void dropOtherCurios(ICuriosItemHandler handler, String currentIdentifier, int currentIndex) {
        handler.getCurios().forEach((identifier, stacksHandler) -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                if (identifier.equals(currentIdentifier) && slot == currentIndex) {
                    continue;
                }
                ItemStack stack = stacks.getStackInSlot(slot);
                if (LovinsWrathItem.isLovinsWrath(stack) || !isAllowedCurio(stack)) {
                    dropCurioSlot(handler.getWearer(), stacksHandler, slot);
                }
            }
        });
    }

    private static void dropCurioSlot(LivingEntity wearer, ICurioStacksHandler stacksHandler, int slot) {
        IDynamicStackHandler stacks = stacksHandler.getStacks();
        if (slot < 0 || slot >= stacks.getSlots()) {
            return;
        }

        ItemStack stack = stacks.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        stacks.setStackInSlot(slot, ItemStack.EMPTY);
        stacks.setPreviousStackInSlot(slot, ItemStack.EMPTY);
        drop(wearer, stack);
    }
}
