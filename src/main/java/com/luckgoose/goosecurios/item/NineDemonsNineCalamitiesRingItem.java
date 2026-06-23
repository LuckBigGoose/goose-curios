package com.luckgoose.goosecurios.item;

import com.luckgoose.goosecurios.config.NineCalamitiesConfig;
import com.luckgoose.goosecurios.util.ClientUtils;
import com.luckgoose.goosecurios.util.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Locale;
import java.util.List;

/**
 * 九魔·九厄 - 核心物品类
 * 
 * 【设计理念】
 * 这是一个依附于 Goety 的饰品。
 * 核心机制是鼓励玩家携带多种不同类型的魔杖获得伤害加成。
 * 
 * 【核心机制】
 * 1. 基础加成（静态）：
 *    - 快捷栏中每多一种不同的魔杖，获得递减的伤害加成
 *    - 第1种魔杖：+35%，第2种：+30%，第3种：+25%...
 *    - 最多计算9种魔杖
 * 
 * 2. 施法加成（动态）：
 *    - 使用魔杖施法时，如果魔杖流派与法术流派匹配，获得额外加成
 *    - 每种流派首次匹配时，添加一层施法加成（+魔杖数量 × 5%）
 *    - 施法加成持续8秒，期间可叠加不同流派的加成
 *    - 例如：拥有3种魔杖，先后使用魔法和虚空流派法术，获得 2 × (3 × 5%) = 30% 加成
 * 
 * 【流派系统】
 * Goety mod定义了多种魔杖流派（SpellType）：
 * - WILD（荒野）、NETHER（下界）、FROST（霜冻）、WIND（风暴）
 * - NECROMANCY（亡灵）、ILLUSION（死灵）等
 * 只有魔杖自身的流派与施放的法术流派一致时才会触发施法加成
 * 
 * 【与 Goety 的集成】
 * - 检测 DarkWand 类型物品
 * - 监听 CastMagicEvent（瞬发）和 CastingMagicEvent（持续施法）
 * - 通过 GoetyEventBridge 桥接事件
 * 
 * 【伤害锁定机制】
 * 装备此饰品后，玩家只能在主手持魔杖时造成伤害：
 * - 徒手攻击 → 拦截
 * - 使用剑/斧等武器 → 拦截
 * - 使用 Irons Spells mod 的法术 → 拦截（防止绕过）
 * 
 * 【配置项】
 * - FIRST_WAND_BONUS_PERCENT: 第一种魔杖的加成
 * - BONUS_DECAY_PER_WAND_PERCENT: 每增加一种魔杖的衰减
 * - CAST_BONUS_PER_WAND_PERCENT: 每种魔杖提供的施法加成
 * - CAST_BONUS_DURATION_TICKS: 施法加成持续时间
 * - MAX_WANDS: 最多计算的魔杖种类数
 * 
 * @author luckgoose
 * @see NineCalamitiesEventHandler 事件处理器
 * @see GoetyEventBridge Goety mod 事件桥接
 */
public class NineDemonsNineCalamitiesRingItem extends Item implements ICurioItem {

    public NineDemonsNineCalamitiesRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Lore 装饰文字
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.lore.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.lore.2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.lore.3")); // 空行
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.oath"));
        tooltip.add(Component.empty());
        
        // 如果按住Shift，显示详细信息
        if (level != null && level.isClientSide && isShiftKeyDownClient()) {
            addDetailedEffects(tooltip);
        } else {
            // 简略版本 - 只显示能力名称
            tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.1"));
            tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.2"));
            tooltip.add(Component.empty());
            // 提示信息 - 仅在未按 Shift 时显示
            tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.hold_shift"));
        }
    }
    
    /**
     * 检测Shift键是否按下（客户端专用）
     * 
     * @return Shift键是否按下
     */
    private static boolean isShiftKeyDownClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
            && ClientUtils.isShiftKeyDown();
    }

    private static Component percentPoint(double value) {
        return ComponentUtils.gold(String.format(Locale.ROOT, "%.2f", value));
    }
    
    /**
     * 添加详细效果描述（按住Shift时显示）
     */
    private void addDetailedEffects(List<Component> tooltip) {
        // 能力1：魔杖共鸣
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.1"));
        
        // 获取真实的玩家数据
        int currentWands = 0;
        double currentBonus = 0.0;
        int maxWands = NineCalamitiesConfig.MAX_WANDS.get();
        
        // 在客户端且有玩家时获取实时数据
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                currentWands = com.luckgoose.goosecurios.event.NineCalamitiesEventHandler.getWandTypeCount(mc.player);
                currentBonus = com.luckgoose.goosecurios.event.NineCalamitiesEventHandler.getBaseBonus(mc.player) * 100.0;
            }
        }
        
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.1.detail1",
                ComponentUtils.gold(currentWands), ComponentUtils.gold(maxWands), ComponentUtils.gold(String.format(Locale.ROOT, "%.2f", currentBonus))));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.1.detail2",
                percentPoint(NineCalamitiesConfig.FIRST_WAND_BONUS_PERCENT.get()),
                percentPoint(NineCalamitiesConfig.BONUS_DECAY_PER_WAND_PERCENT.get()),
                percentPoint(NineCalamitiesConfig.MIN_WAND_BONUS_PERCENT.get())));
        tooltip.add(Component.empty());
        
        // 能力2：施法共振
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.2.detail1",
                ComponentUtils.seconds(NineCalamitiesConfig.CAST_BONUS_DURATION_TICKS.get())));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.2.detail2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.nine_calamities.ability.2.detail3",
                percentPoint(NineCalamitiesConfig.CAST_BONUS_PER_WAND_PERCENT.get())));
    }
}
