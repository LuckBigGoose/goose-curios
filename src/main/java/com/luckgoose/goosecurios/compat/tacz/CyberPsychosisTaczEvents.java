package com.luckgoose.goosecurios.compat.tacz;

import com.luckgoose.goosecurios.init.ModItems;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.UUID;

/**
 * 赛博精神病物品的TACZ枪械模组兼容事件处理器
 * 
 * <p>实现核心游戏机制：
 * <ul>
 *   <li>暴击触发系统：基于概率随机触发爆头效果</li>
 *   <li>概率递增机制：未触发时增加下次触发概率</li>
 *   <li>概率重置机制：触发后或超时后重置概率</li>
 * </ul>
 * 
 * @author luckgoose
 * @see CyberPsychosisState
 */
public class CyberPsychosisTaczEvents {

    /**
     * 处理枪械伤害事件，实现概率爆头机制
     * 
     * <p>逻辑流程：
     * <ol>
     *   <li>检查并重置过期的触发概率</li>
     *   <li>强制取消原始爆头判定</li>
     *   <li>根据当前概率随机决定是否触发爆头</li>
     *   <li>触发成功：设置爆头并重置概率</li>
     *   <li>触发失败：增加下次触发概率</li>
     * </ol>
     * 
     * <p>注意：使用LOW优先级避免与其他mod的爆头逻辑冲突
     * 
     * @param event 枪械伤害事件
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (!(event.getAttacker() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        if (!CyberPsychosisState.isEquipped(uuid)) {
            return;
        }

        int currentTick = player.server.getTickCount();
        CyberPsychosisState.resetChanceIfExpired(uuid, currentTick);
        CyberPsychosisState.updateLastDamageTick(uuid, currentTick);
        
        // 强制取消原始爆头，使用自己的概率系统
        event.setHeadshot(false);

        double chance = CyberPsychosisState.getChance(uuid);
        if (player.getRandom().nextDouble() < chance) {
            // 触发爆头，重置概率
            event.setHeadshot(true);
            CyberPsychosisState.resetChance(uuid);
        } else {
            // 未触发，增加下次概率
            CyberPsychosisState.increaseChance(uuid);
        }
    }

    /**
     * 处理饰品更换事件，更新装备计数
     * 
     * <p>修复：使用原子操作避免竞态条件
     */
    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean fromCyberPsychosis = event.getFrom().is(ModItems.CYBER_PSYCHOSIS.get());
        boolean toCyberPsychosis = event.getTo().is(ModItems.CYBER_PSYCHOSIS.get());
        if (!fromCyberPsychosis && !toCyberPsychosis) {
            return;
        }

        UUID uuid = player.getUUID();
        // 修复：先检查再操作，避免不必要的状态修改
        if (fromCyberPsychosis && !toCyberPsychosis) {
            CyberPsychosisState.removeEquipped(uuid);
        } else if (!fromCyberPsychosis && toCyberPsychosis) {
            CyberPsychosisState.addEquipped(uuid);
        }
        // 如果是替换（from和to都是赛博精神病），无需操作
    }

    /** 处理玩家登录事件，初始化装备状态 */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CyberPsychosisState.setEquippedCount(player.getUUID(), countEquipped(player));
        }
    }

    /** 处理玩家登出事件，清理所有状态数据 */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CyberPsychosisState.clear(player.getUUID());
        }
    }

    /** 统计玩家装备的赛博精神病数量 */
    private static int countEquipped(ServerPlayer player) {
        return com.luckgoose.goosecurios.util.CuriosUtils.countEquippedCurios(player, ModItems.CYBER_PSYCHOSIS.get());
    }
}

