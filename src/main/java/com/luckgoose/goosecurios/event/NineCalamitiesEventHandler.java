package com.luckgoose.goosecurios.event;

import com.Polarice3.Goety.api.magic.ISpell;
import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.events.spell.CastMagicEvent;
import com.Polarice3.Goety.common.events.spell.CastingMagicEvent;
import com.Polarice3.Goety.common.items.magic.DarkWand;
import com.luckgoose.goosecurios.config.NineCalamitiesConfig;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.network.ModNetwork;
import com.luckgoose.goosecurios.network.NineCalamitiesCastBonusSyncPacket;
import com.luckgoose.goosecurios.network.ShortStatusMessagePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 九魔九厄戒指事件处理器
 * 
 * <p>实现核心游戏机制：
 * <ul>
 *   <li>伤害锁定：只有手持魔杖才能造成伤害（防止近战击杀）</li>
 *   <li>基础加成：根据装备的魔杖流派数量提供伤害加成</li>
 *   <li>施法加成：施放匹配流派的法术后，提供额外的层数加成</li>
 * </ul>
 * 
 * <p>加成计算公式：
 * <pre>
 * 总加成 = 基础加成 + 施法加成
 * 基础加成 = 魔杖流派数 × 单流派基础加成
 * 施法加成 = 魔杖流派数 × 单流派加成 × 施法层数
 * 施法层数 = 已施放的不同流派数量（最多等于魔杖流派数）
 * </pre>
 * 
 * <p>设计理念：
 * <ul>
 *   <li>鼓励多流派魔杖：装备越多不同流派的魔杖，基础加成越高</li>
 *   <li>施法刷新机制：施放匹配流派的法术才能刷新施法加成</li>
 *   <li>流派匹配要求：魔杖流派必须与施放的法术流派一致</li>
 * </ul>
 * 
 * <p>线程安全：使用HIGH优先级确保伤害锁定生效
 * 
 * @author luckgoose
 * @see NineCalamitiesConfig
 */
public class NineCalamitiesEventHandler {

    // ==================== NBT 键名 ====================
    
    /** NBT键：施法加成过期时间（游戏刻） */
    private static final String CAST_BONUS_UNTIL = "goose_curiosNineCalamitiesCastBonusUntil";
    
    /** NBT键：已施放的法术流派列表 */
    private static final String CAST_BONUS_TYPES = "goose_curiosNineCalamitiesCastBonusTypes";
    
    /** NBT键：施法加成版本号（用于客户端同步） */
    private static final String CAST_BONUS_VERSION = "goose_curiosNineCalamitiesCastBonusVersion";
    
    /** NBT键：伤害锁定提示冷却时间 */
    private static final String DAMAGE_LOCK_MESSAGE_COOLDOWN = "goose_curiosNineCalamitiesDamageLockMessageCooldown";
    
    // ==================== 常量配置 ====================
    
    /** 
     * 伤害锁定提示的冷却时间：3秒（60 tick）
     * 防止同一提示频繁弹出干扰玩家
     */
    private static final long DAMAGE_LOCK_MESSAGE_COOLDOWN_TICKS = 20 * 3; // 3秒
    
    /** 
     * 伤害锁定提示的显示时长：1.5秒（30 tick）
     * 提示消息在屏幕上停留的时间
     */
    private static final int DAMAGE_LOCK_MESSAGE_DURATION_TICKS = 30; // 1.5秒
    
    // ==================== 伤害拦截 ====================
    
    /**
     * 检查是否为 Irons Spells 的伤害
     * 
     * <p>检查两个维度：
     * <ul>
     *   <li>伤害类型的命名空间是否为 irons_spellbooks</li>
     *   <li>直接伤害实体的类型是否来自 irons_spellbooks</li>
     * </ul>
     * 
     * @param source 伤害来源
     * @return 是否为 Irons Spells 的伤害
     */
    private static boolean isIronsSpellsDamage(DamageSource source) {
        // 检查伤害类型的注册名是否来自 irons_spellbooks
        ResourceLocation damageTypeLocation = source.typeHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        
        if (damageTypeLocation != null && "irons_spellbooks".equals(damageTypeLocation.getNamespace())) {
            return true;
        }
        
        // 检查直接伤害实体是否来自 irons_spellbooks
        Entity directEntity = source.getDirectEntity();
        if (directEntity != null) {
            ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
            if (entityType != null && "irons_spellbooks".equals(entityType.getNamespace())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 拦截不允许的伤害
     * 
     * <p>伤害拦截规则（按优先级）：
     * <ol>
     *   <li>Irons Spells 伤害 → 始终拦截（防止使用铁魔法绕过限制）</li>
     *   <li>主手持魔杖 → 允许造成伤害</li>
     *   <li>非手持魔杖 → 拦截伤害（防止近战击杀）</li>
     * </ol>
     * 
     * <p>使用HIGH优先级确保在其他mod之前执行
     * 
     * @param event 生物攻击事件
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingAttack(LivingAttackEvent event) {
        Player player = resolveDamagePlayer(event.getSource());
        if (player == null || !hasRing(player)) {
            return;
        }

        // 如果是 Irons Spells 的伤害，直接拦截
        if (isIronsSpellsDamage(event.getSource())) {
            showDamageLockMessage(player);
            event.setCanceled(true);
            return;
        }

        // 如果主手持魔杖，允许造成伤害
        if (isWand(player.getMainHandItem())) {
            return;
        }
        
        // 非手持魔杖，拦截伤害并提示玩家
        showDamageLockMessage(player);
        event.setCanceled(true);
    }

    /**
     * 应用伤害加成：基础加成 + 施法加成
     * 
     * <p>只有手持魔杖且装备戒指时才应用加成
     * 
     * @param event 生物伤害事件
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        Player player = resolveDamagePlayer(event.getSource());
        if (player == null || !hasRing(player) || !isWand(player.getMainHandItem())) {
            return;
        }

        // 计算总加成
        double bonus = getBaseBonus(player) + getCastBonus(player);
        if (bonus > 0.0D) {
            event.setAmount((float) (event.getAmount() * (1.0D + bonus)));
        }
    }

    // ==================== 施法事件处理 ====================
    
    /**
     * 处理瞬发施法事件（施法完成时触发）
     * 
     * @param event 施法事件
     */
    public void handleCastMagic(CastMagicEvent event) {
        processSpellCast(event.getEntity(), event.getSpell());
    }
    
    /**
     * 处理持续施法事件（施法过程中每tick触发）
     * 
     * <p>使用节流机制避免频繁同步
     * 
     * @param event 持续施法事件
     */
    public void handleCastingMagic(CastingMagicEvent event) {
        processSpellCast(event.getEntity(), event.getSpell());
    }
    
    /**
     * 处理Goety施法：只有魔杖流派与法术流派一致时刷新加成
     * 
     * <p>核心逻辑：
     * <ol>
     *   <li>检查玩家是否装备戒指且手持魔杖</li>
     *   <li>获取匹配的流派（魔杖流派 = 法术流派）</li>
     *   <li>刷新施法加成持续时间</li>
     *   <li>添加新的流派层数（去重）</li>
     *   <li>使用节流机制同步到客户端</li>
     * </ol>
     * 
     * @param entity 施法实体
     * @param spell 法术
     */
    private void processSpellCast(LivingEntity entity, ISpell spell) {
        if (!(entity instanceof Player player) || !hasRing(player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        
        if (!isWand(stack) || spell == null) {
            return;
        }

        List<String> previousTypes = getCastBonusTypes(player);
        long previousUntil = player.getPersistentData().getLong(CAST_BONUS_UNTIL);
        List<SpellType> matchedSpellTypes = getMatchingSpellTypes(stack, spell);
        if (matchedSpellTypes.isEmpty()) {
            return;
        }

        long gameTime = player.level().getGameTime();
        
        for (SpellType spellType : matchedSpellTypes) {
            addCastBonusType(player, spellType);
        }
        
        long until = gameTime + NineCalamitiesConfig.CAST_BONUS_DURATION_TICKS.get();
        player.getPersistentData().putLong(CAST_BONUS_UNTIL, until);
        List<String> currentTypes = getCastBonusTypes(player);
        if (!shouldSyncCastBonus(gameTime, previousUntil, previousTypes, currentTypes)) {
            return;
        }
        
        long version = nextCastBonusVersion(player);
        syncCastBonusToClient(player, until, currentTypes, version);
    }

    // ==================== 加成计算 ====================
    
    /**
     * 检查玩家是否装备九魔九厄戒指
     * 
     * @param player 玩家
     * @return 是否装备戒指
     */
    private static boolean hasRing(Player player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(ModItems.NINE_DEMONS_NINE_CALAMITIES.get()).isPresent())
                .orElse(false);
    }

    /**
     * 公开API：检查玩家是否装备九魔九厄戒指
     * 
     * @param player 玩家
     * @return 是否装备戒指
     */
    public static boolean hasNineCalamitiesRing(Player player) {
        return hasRing(player);
    }

    /**
     * 公开API：检查物品是否为Goety魔杖
     * 
     * @param stack 物品堆
     * @return 是否为魔杖
     */
    public static boolean isGoetyWand(ItemStack stack) {
        return isWand(stack);
    }

    /**
     * 检查物品是否为魔杖
     * 
     * @param stack 物品堆
     * @return 是否为魔杖
     */
    private static boolean isWand(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DarkWand;
    }

    /**
     * 统计快捷栏内不同魔杖的种类数量
     * 
     * <p>用作基础加成的层数计算依据，上限由配置控制
     * 
     * @param player 玩家
     * @return 不同魔杖种类数量
     */
    public static int getWandTypeCount(Player player) {
        Set<ResourceLocation> ids = new HashSet<>();
        int count = 0;
        int max = NineCalamitiesConfig.MAX_WANDS.get();
        
        for (int i = 0; i < 9 && count < max; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!isWand(stack)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ids.add(id)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取基础伤害加成
     * 
     * <p>基础加成 = Σ(第i个魔杖加成)，其中每个魔杖加成会递减
     * 
     * @param player 玩家
     * @return 基础加成百分比（例如0.2表示20%）
     */
    public static double getBaseBonus(Player player) {
        int count = getWandTypeCount(player);
        return getBaseBonusForCount(count);
    }

    /**
     * 根据魔杖数量计算基础加成
     * 
     * <p>使用递减公式：第i个魔杖加成 = max(首个加成 - 递减 × i, 最小加成)
     * 
     * @param count 魔杖数量
     * @return 基础加成百分比
     */
    public static double getBaseBonusForCount(int count) {
        double first = NineCalamitiesConfig.FIRST_WAND_BONUS_PERCENT.get() / 100.0D;
        double decay = NineCalamitiesConfig.BONUS_DECAY_PER_WAND_PERCENT.get() / 100.0D;
        double min = NineCalamitiesConfig.MIN_WAND_BONUS_PERCENT.get() / 100.0D;
        double total = 0.0D;
        
        for (int i = 0; i < count; i++) {
            total += Math.max(first - decay * i, min);
        }
        return total;
    }

    /**
     * 获取施法加成
     * 
     * <p>施法加成 = 单层加成 × 施法层数
     * <p>单层加成 = 魔杖种类数 × 单流派施法加成百分比
     * <p>施法层数 = 已施放的不同流派数量
     * 
     * @param player 玩家
     * @return 施法加成百分比
     */
    public static double getCastBonus(Player player) {
        if (getCastBonusRemainingTicks(player) <= 0) {
            return 0.0D;
        }
        return getSingleCastLayerBonus(player) * getCastBonusLayerCount(player);
    }

    /**
     * 获取单层施法加成
     * 
     * <p>单层加成 = 魔杖种类数 × 单流派施法加成百分比
     * 
     * @param player 玩家
     * @return 单层施法加成百分比
     */
    public static double getSingleCastLayerBonus(Player player) {
        return getWandTypeCount(player) * NineCalamitiesConfig.CAST_BONUS_PER_WAND_PERCENT.get() / 100.0D;
    }

    /**
     * 获取施法加成层数
     * 
     * <p>层数 = 已施放的不同流派数量（最多等于魔杖种类数）
     * 
     * @param player 玩家
     * @return 施法加成层数
     */
    public static int getCastBonusLayerCount(Player player) {
        if (getCastBonusRemainingTicks(player) <= 0) {
            return 0;
        }
        return getCastBonusTypes(player).size();
    }

    /**
     * 获取施法加成剩余时间
     * 
     * <p>如果已过期则自动清理NBT数据
     * 
     * @param player 玩家
     * @return 剩余游戏刻数
     */
    public static long getCastBonusRemainingTicks(Player player) {
        long remainingTicks = player.getPersistentData().getLong(CAST_BONUS_UNTIL) - player.level().getGameTime();
        if (remainingTicks <= 0L) {
            clearCastBonus(player);
            return 0L;
        }
        return remainingTicks;
    }

    /**
     * 获取总加成：基础加成 + 施法加成
     * 
     * @param player 玩家
     * @return 总加成百分比
     */
    public static double getTotalBonus(Player player) {
        return getBaseBonus(player) + getCastBonus(player);
    }

    // ==================== 匹配逻辑 ====================
    
    /**
     * 获取匹配的法术类型（魔杖流派与施法流派必须一致）
     */
    private static List<SpellType> getMatchingSpellTypes(ItemStack wandStack, ISpell spell) {
        List<SpellType> matchedSpellTypes = new ArrayList<>();
        SpellType castSpellType = spell.getSpellType();
        if (castSpellType == null) {
            return matchedSpellTypes;
        }
        
        // 获取魔杖本身的流派（不是聚晶的流派）
        if (!(wandStack.getItem() instanceof DarkWand darkWand)) {
            return matchedSpellTypes;
        }
        
        // 检查魔杖的流派类型
        SpellType wandSpellType = darkWand.getSpellType();
        
        // 只有当魔杖流派与施法流派一致时才匹配
        if (wandSpellType != null && wandSpellType == castSpellType) {
            matchedSpellTypes.add(castSpellType);
        }
        
        return matchedSpellTypes;
    }

    // ==================== 伤害来源解析 ====================
    
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

    // ==================== 消息提示 ====================
    
    private static void showDamageLockMessage(Player player) {
        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().getLong(DAMAGE_LOCK_MESSAGE_COOLDOWN) > gameTime) {
            return;
        }
        
        player.getPersistentData().putLong(DAMAGE_LOCK_MESSAGE_COOLDOWN, 
                gameTime + DAMAGE_LOCK_MESSAGE_COOLDOWN_TICKS);
        
        if (player instanceof ServerPlayer serverPlayer) {
            Component message = Component.translatable("message.goose_curios.nine_calamities.need_wand")
                    .withStyle(ChatFormatting.RED);
            ModNetwork.CHANNEL.sendTo(
                    new ShortStatusMessagePacket(message, DAMAGE_LOCK_MESSAGE_DURATION_TICKS),
                    serverPlayer.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    // ==================== 施法加成数据管理 ====================
    
    private static void addCastBonusType(Player player, SpellType spellType) {
        List<String> types = getCastBonusTypes(player);
        String key = getSpellTypeKey(spellType);
        // 防止同一流派多次叠加：只添加一次
        if (!types.contains(key)) {
            types.add(key);
        }
        setCastBonusTypes(player, types);
    }

    private static String getSpellTypeKey(SpellType spellType) {
        return spellType.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static List<String> getCastBonusTypes(Player player) {
        if (getCastBonusRemainingTicks(player) <= 0) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        CompoundTag tag = player.getPersistentData();
        
        if (!tag.contains(CAST_BONUS_TYPES, Tag.TAG_LIST)) {
            return result;
        }
        
        ListTag stored = tag.getList(CAST_BONUS_TYPES, Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) {
            String value = stored.getString(i);
            if (!value.isBlank() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static void setCastBonusTypes(Player player, List<String> types) {
        ListTag tag = new ListTag();
        for (String type : types) {
            tag.add(StringTag.valueOf(type));
        }
        player.getPersistentData().put(CAST_BONUS_TYPES, tag);
    }

    /**
     * 清理施法加成相关的NBT数据
     */
    private static void clearCastBonus(Player player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(CAST_BONUS_UNTIL);
        tag.remove(CAST_BONUS_TYPES);
        tag.remove(CAST_BONUS_VERSION);
    }

    private static long nextCastBonusVersion(Player player) {
        long version = player.getPersistentData().getLong(CAST_BONUS_VERSION) + 1L;
        player.getPersistentData().putLong(CAST_BONUS_VERSION, version);
        return version;
    }

    /**
     * 同步施法加成到客户端
     */
    public static void syncCastBonus(Player player, long until, List<String> types, long version) {
        if (player.getPersistentData().getLong(CAST_BONUS_VERSION) > version) {
            return;
        }
        if (until <= player.level().getGameTime() || types.isEmpty()) {
            clearCastBonus(player);
            player.getPersistentData().putLong(CAST_BONUS_VERSION, version);
            return;
        }
        player.getPersistentData().putLong(CAST_BONUS_VERSION, version);
        player.getPersistentData().putLong(CAST_BONUS_UNTIL, until);
        setCastBonusTypes(player, types);
    }

    private static boolean shouldSyncCastBonus(long gameTime, long previousUntil, List<String> previousTypes, List<String> currentTypes) {
        return !previousTypes.equals(currentTypes) || previousUntil <= gameTime || gameTime % 10L == 0L;
    }

    private static void syncCastBonusToClient(Player player, long until, List<String> types, long version) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.CHANNEL.sendTo(
                    new NineCalamitiesCastBonusSyncPacket(until, types, version),
                    serverPlayer.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }
}
