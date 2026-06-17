package com.luckgoose.goosecurios.item;

import com.luckgoose.goosecurios.compat.tacz.BondWillState;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.config.BondWillConfig;
import com.luckgoose.goosecurios.network.BondWillSyncPacket;
import com.luckgoose.goosecurios.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Locale;

/**
 * 邦德的意志 - 核心物品类
 * 
 * 【设计理念】
 * 这是一个基于TACZ枪械模组的时间操控类饰品。
 * 核心机制是通过"脱离战斗 + 瞄准状态"触发隐身并积累伤害加成，达到满值后触发短暂的"时间停止"效果。
 * 
 * 【核心机制】
 * 1. 潜行隐身：脱战状态下使用枪械瞄准可进入隐身状态（BondVanishing效果）
 * 2. 伤害累积：隐身期间每秒增加伤害加成（默认5%/秒，上限100%）
 * 3. 时停效果：伤害加成达到100%时进入时停状态，周围48格内的非玩家实体被冻结
 * 4. 冷却惩罚：脱离隐身状态后有冷却期，期间造成的伤害降低90%
 * 
 * 【状态流转】
 * 正常 -> 脱战+瞄准 -> 隐身+蓄力 -> 伤害加成100% -> 时停 -> 造成伤害/移动 -> 冷却惩罚 -> 正常
 * 
 * 【与TACZ枪械模组的集成】
 * - 检测玩家的瞄准进度（getSynAimingProgress）
 * - 拦截枪械伤害事件（EntityHurtByGunEvent）并应用加成
 * - 在时停状态下允许玩家继续开火
 * 
 * @author luckgoose
 * @see BondWillState 状态管理器
 * @see BondWillTaczEvents 事件处理器
 * @see BondWillTimeStopManager 时停效果管理器
 */
public class BondWillItem extends Item implements ICurioItem {

    public BondWillItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    /**
     * 添加物品提示信息
     * 
     * 【显示逻辑】
     * - 普通状态：显示简略效果描述 + "按住Shift查看详情"提示
     * - 按住Shift：显示详细效果描述，包含配置文件中的具体数值
     * 
     * 【提示内容】
     * 1. 潜行隐身：脱战+瞄准触发隐身
     * 2. 伤害累积：隐身时每秒增加伤害加成，显示具体数值
     * 3. 时停效果：伤害加成达到阈值时触发时停
     * 4. 冷却惩罚：未满蓄力时开火的伤害惩罚
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 标题装饰
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.lore.1")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.lore.2")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        
        // 效果标题
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.title"));
        tooltip.add(Component.empty());
        
        // 如果按住Shift，显示详细信息
        if (level != null && level.isClientSide && isShiftKeyDownClient()) {
            addDetailedEffects(tooltip);
        } else {
            // 简略版本
            tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.1"));
            tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.2"));
            tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.3"));
            tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.4"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.settings"));
    }
    
    /**
     * 检测Shift键是否按下（客户端专用）
     * 
     * 【实现细节】
     * - 使用GLFW直接检测键盘状态，同时检测左右Shift
     * - 必须在客户端环境下执行，服务器环境直接返回false
     */
    private static boolean isShiftKeyDownClient() {
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    /**
     * 添加详细效果描述（按住Shift时显示）
     * 
     * 【数值来源】
     * 所有数值从BondWillConfig配置文件读取，方便服务器管理员调整平衡性：
     * - OUT_OF_COMBAT_TICKS: 脱战判定时间
     * - BONUS_PER_SECOND: 每秒伤害加成增长率
     * - MAX_BONUS: 最大伤害加成
     * - AIMING_PROGRESS_THRESHOLD: 触发隐身的瞄准进度阈值
     * - COOLDOWN_DAMAGE_REDUCTION: 冷却期间的伤害削减
     */
    private void addDetailedEffects(List<Component> tooltip) {
        // 第一条：潜行隐身
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.1"));
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.1.detail"));
        tooltip.add(Component.empty());
        
        // 第二条：伤害累积
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.2"));
        int outOfCombatSeconds = BondWillConfig.OUT_OF_COMBAT_TICKS.get() / 20;
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.2.detail",
                outOfCombatSeconds,
                percent(BondWillConfig.BONUS_PER_SECOND.get(), ChatFormatting.RED),
                percent(BondWillConfig.MAX_BONUS.get(), ChatFormatting.YELLOW)));
        tooltip.add(Component.empty());
        
        // 第三条：时停
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.3"));
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.3.detail",
                percent(BondWillConfig.AIMING_PROGRESS_THRESHOLD.get(), ChatFormatting.YELLOW)));
        tooltip.add(Component.empty());
        
        // 第四条：冷却惩罚
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.4"));
        tooltip.add(Component.translatable("tooltip.goose_curios.bond_will.effect.4.detail",
                percent(BondWillConfig.COOLDOWN_DAMAGE_REDUCTION.get(), ChatFormatting.RED)));
    }

    /**
     * 装备饰品时触发
     * 
     * 【功能】
     * 将玩家UUID添加到装备计数器中，允许同时装备多个该饰品
     * （虽然通常饰品槽位有限，但这样设计支持未来扩展）
     */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            BondWillState.addEquipped(player.getUUID());
            sendSettingsPacketToClient(player, stack);
        }
    }

    /**
     * 卸下饰品时触发
     * 
     * 【清理逻辑】
     * 1. 减少装备计数
     * 2. 如果玩家不再装备任何该饰品，清除所有状态：
     *    - 取消隐身效果
     *    - 清空伤害加成
     *    - 清除时停状态
     * 3. 向客户端发送清理包，同步UI显示
     * 
     * 【重要】
     * 必须清理服务端和客户端两边的状态，否则会出现UI残留或状态不同步的问题
     */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            BondWillState.removeEquipped(player.getUUID());
            if (!BondWillState.isEquipped(player.getUUID())) {
                BondWillState.clearActiveState(player);
                // 清理客户端状态
                sendClearPacketToClient(player);
            }
        }
    }
    
    /**
     * 向客户端发送清理包
     * 
     * 【数据包内容】
     * 将所有状态值重置为初始状态：
     * - 进度、加成、最大值：0.0F
     * - 激活状态、装备状态：false
     * - 冷却时间：0
     * 
     * 这确保客户端HUD显示正确清除
     */
    private void sendClearPacketToClient(ServerPlayer player) {
        ModNetwork.CHANNEL.sendTo(
            new BondWillSyncPacket(0.0F, 0.0F, 0.0F, false, false, 0, false, 0.0F, new net.minecraft.nbt.CompoundTag()),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private void sendSettingsPacketToClient(ServerPlayer player, ItemStack stack) {
        ModNetwork.CHANNEL.sendTo(
            new BondWillSyncPacket(0.0F, 0.0F, BondWillConfig.MAX_BONUS.get().floatValue(), false, true, 0, false, 0.0F, BondWillSettings.copySettings(stack)),
            player.connection.connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private static Component percent(double value, ChatFormatting formatting) {
        return Component.literal(String.format(Locale.ROOT, "%.0f", value * 100.0D) + "%")
                .withStyle(formatting);
    }
}
