package com.luckgoose.goosecurios.event;

import com.luckgoose.goosecurios.config.LovinsWrathConfig;
import com.luckgoose.goosecurios.item.LovinsWrathItem;
import com.luckgoose.goosecurios.network.ModNetwork;
import com.luckgoose.goosecurios.network.ShortStatusMessagePacket;
import com.luckgoose.goosecurios.util.LovinsWrathRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.CurioEquipEvent;

import javax.annotation.Nullable;

/**
 * lovin的愤怒事件处理器。
 *
 * <p>负责饰品互斥、护甲禁止、武器白名单、反伤和击杀成长。
 */
public class LovinsWrathEventHandler {
    private static final String IRONS_SPELLBOOKS_NAMESPACE = "irons_spellbooks";
    private static final String DAMAGE_LOCK_MESSAGE_COOLDOWN = "goose_curios.lovins_wrath.damage_lock_cooldown";
    private static final long DAMAGE_LOCK_MESSAGE_COOLDOWN_TICKS = 20L * 3L;
    private static final int DAMAGE_LOCK_MESSAGE_DURATION_TICKS = 30;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCurioEquip(CurioEquipEvent event) {
        ItemStack stack = event.getStack();
        LivingEntity entity = event.getSlotContext().entity();

        if (LovinsWrathItem.isLovinsWrath(stack)) {
            boolean wrongSlot = !LovinsWrathRules.RING_SLOT.equals(event.getSlotContext().identifier());
            boolean alreadyEquipped = LovinsWrathRules.hasLovinsWrath(entity);
            if (wrongSlot || alreadyEquipped) {
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        if (LovinsWrathRules.hasLovinsWrath(entity) && !LovinsWrathRules.isAllowedCurio(stack)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (event.getTo().isEmpty() || !LovinsWrathRules.hasLovinsWrath(event.getEntity())) {
            return;
        }

        if (LovinsWrathItem.isLovinsWrath(event.getTo())) {
            LovinsWrathRules.dropOtherCurios(event.getEntity(), event.getIdentifier(), event.getSlotIndex());
            LovinsWrathRules.dropInvalidArmor(event.getEntity());
            return;
        }

        if (!LovinsWrathRules.isAllowedCurio(event.getTo())) {
            LovinsWrathRules.dropInvalidCurioSlot(event.getEntity(), event.getIdentifier(), event.getSlotIndex());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingAttack(LivingAttackEvent event) {
        Player attacker = resolveDamagePlayer(event.getSource());
        if (attacker == null || !LovinsWrathRules.hasLovinsWrath(attacker)) {
            return;
        }

        if (isIronsSpellsDamage(event.getSource()) || !LovinsWrathRules.isAllowedWeapon(attacker.getMainHandItem())) {
            showDamageLockMessage(attacker);
            event.setCanceled(true);
            reflectDamage(attacker, event.getAmount());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDamage(LivingDamageEvent event) {
        Player attacker = resolveDamagePlayer(event.getSource());
        if (attacker == null) {
            return;
        }

        if (LovinsWrathRules.hasLovinsWrath(attacker)) {
            handleLovinDamage(event, attacker);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Player killer = resolveDamagePlayer(event.getSource());
        if (killer == null || killer.level().isClientSide()) {
            return;
        }

        LivingEntity killed = event.getEntity();
        if (!LovinsWrathConfig.isGrowthTarget(killed.getType())) {
            return;
        }

        int points = LovinsWrathConfig.getGrowthPoints(killed.getType());
        LovinsWrathRules.getLovinsWrath(killer)
                .map(top.theillusivec4.curios.api.SlotResult::stack)
                .ifPresent(stack -> LovinsWrathItem.addKillPoints(stack, points));
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getTo().isEmpty()) {
            return;
        }

        EquipmentSlot slot = event.getSlot();
        LivingEntity entity = event.getEntity();
        if (LovinsWrathRules.blocksArmor(entity, slot, event.getTo())) {
            LovinsWrathRules.dropArmorSlot(entity, slot);
        }
    }

    private static void handleLovinDamage(LivingDamageEvent event, Player attacker) {
        if (event.getAmount() <= 0.0F) {
            return;
        }

        if (isIronsSpellsDamage(event.getSource())) {
            float reflected = event.getAmount();
            event.setAmount(0.0F);
            showDamageLockMessage(attacker);
            reflectDamage(attacker, reflected);
            return;
        }

        if (LovinsWrathRules.isAllowedWeapon(attacker.getMainHandItem())) {
            return;
        }

        float reflected = event.getAmount();
        event.setAmount(0.0F);
        showDamageLockMessage(attacker);
        reflectDamage(attacker, reflected);
    }

    private static boolean isIronsSpellsDamage(DamageSource source) {
        ResourceLocation damageTypeLocation = source.typeHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (damageTypeLocation != null && IRONS_SPELLBOOKS_NAMESPACE.equals(damageTypeLocation.getNamespace())) {
            return true;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity == null) {
            return false;
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
        return entityType != null && IRONS_SPELLBOOKS_NAMESPACE.equals(entityType.getNamespace());
    }

    private static void reflectDamage(Player attacker, float amount) {
        if (amount > 0.0F) {
            attacker.hurt(attacker.damageSources().magic(), amount);
        }
    }

    private static void showDamageLockMessage(Player player) {
        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().getLong(DAMAGE_LOCK_MESSAGE_COOLDOWN) > gameTime) {
            return;
        }

        player.getPersistentData().putLong(DAMAGE_LOCK_MESSAGE_COOLDOWN, gameTime + DAMAGE_LOCK_MESSAGE_COOLDOWN_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            Component message = Component.translatable("message.goose_curios.lovins_wrath.damage_lock").withStyle(ChatFormatting.RED);
            ModNetwork.CHANNEL.sendTo(
                    new ShortStatusMessagePacket(message, DAMAGE_LOCK_MESSAGE_DURATION_TICKS),
                    serverPlayer.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    @Nullable
    private static Player resolveDamagePlayer(DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            return player;
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }

        return direct instanceof Player player ? player : null;
    }
}
