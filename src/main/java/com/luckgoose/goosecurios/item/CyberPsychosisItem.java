package com.luckgoose.goosecurios.item;

import com.luckgoose.goosecurios.config.CyberPsychosisConfig;
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

import java.util.List;
import java.util.Locale;

/**
 * 赛博精神病 - 核心物品类
 * 
 * 【设计理念】
 * 这是一个与 TACZ 枪械模组集成的"爆头"类饰品。
 * 核心机制是通过"自增积累"来提高下次爆头的概率。
 * 
 * 【核心机制 - 概率累积系统】
 * 1. 初始爆头概率：2%（INITIAL_CHANCE）
 * 2. 每次开火：
 *    - 如果触发爆头 → 概率重置为初始值
 *    - 如果未触发 → 概率增加2%（CHANCE_INCREMENT）
 * 3. 最大概率：50%（MAX_CHANCE）
 * 4. 超时重置：如果10秒（RESET_AFTER_TICKS）未开火，概率重置
 * 
 * 【数学模型】
 * 连续失败次数 n 后的爆头概率：
 * P(n) = min(INITIAL_CHANCE + n × CHANCE_INCREMENT, MAX_CHANCE)
 * 
 * 期望触发次数计算（几何分布）：
 * - 2%概率：平均50次触发1次
 * - 4%概率：平均25次触发1次
 * - 50%概率：平均2次触发1次
 * 
 * 【与 TACZ 枪械模组的集成】
 * - 监听 EntityHurtByGunEvent.Pre 事件
 * - 直接修改 event.setHeadshot() 标志
 * - 优先级设置为 LOW，避免与其他爆头修改mod冲突
 * 
 * 
 * @author luckgoose
 * @see CyberPsychosisState 状态管理器
 * @see CyberPsychosisTaczEvents 事件处理器
 */
public class CyberPsychosisItem extends Item implements ICurioItem {

    public CyberPsychosisItem(Properties properties) {
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
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.lore.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.lore.2"));
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.lore.3")); // 空行
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.effect.1"));
        tooltip.add(Component.empty());
        
        // 如果按住Shift，显示详细信息
        if (level != null && level.isClientSide && isShiftKeyDownClient()) {
            addDetailedEffects(tooltip);
        } else {
            // 提示信息 - 仅在未按 Shift 时显示
            tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.hold_shift"));
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
    
    /**
     * 添加详细效果描述（按住Shift时显示）
     */
    private void addDetailedEffects(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.effect.2", 
                percentValue(CyberPsychosisConfig.INITIAL_CHANCE.get())));
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.effect.2.detail1", 
                percentValue(CyberPsychosisConfig.CHANCE_INCREMENT.get()),
                percentValue(CyberPsychosisConfig.MAX_CHANCE.get())));
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.effect.2.detail2", 
                percentValue(CyberPsychosisConfig.INITIAL_CHANCE.get())));
        tooltip.add(Component.translatable("tooltip.goose_curios.cyber_psychosis.effect.3", 
                ComponentUtils.seconds(CyberPsychosisConfig.RESET_AFTER_TICKS.get())));
    }

    private static Component percentValue(double value) {
        return ComponentUtils.gold(String.format(Locale.ROOT, "%.0f", value * 100.0D));
    }
}
