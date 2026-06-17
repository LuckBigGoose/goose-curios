package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端Forge事件处理器
 * 
 * <p>统一管理所有客户端事件的入口
 * 
 * <p>处理的事件：
 * <ul>
 *   <li>ClientTick：客户端tick更新（特效状态、冻结状态等）</li>
 *   <li>RenderGui：GUI渲染（HUD、状态消息、进度条等）</li>
 *   <li>RenderLevel：世界渲染（冲击波、爆头准星等）</li>
 *   <li>ItemTooltip：物品提示（九魔·九厄详情）</li>
 * </ul>
 * 
 * @author luckgoose
 */
@Mod.EventBusSubscriber(modid = GooseCuriosMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ShortStatusMessageClient.tick();
        BondWillImpactEffects.tick();
        BondWillTimeStopClientEffects.tick();
        ClientBondWillFreezeState.tick();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        BondWillImpactEffects.updateFrame(event.getPartialTick());
        BondWillGrayPostEffect.updateFrame(event.getPartialTick());
        ShortStatusMessageClient.render(event.getGuiGraphics());
        NineCalamitiesClientDisplay.renderHud(event);
        BondWillClientDisplay.render(event);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        BondWillImpactEffects.renderWorld(event);
        TaczClientCompat.renderHeadshot(event);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        NineCalamitiesClientDisplay.addTooltip(event);
    }
}
