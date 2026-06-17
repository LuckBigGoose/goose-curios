package com.luckgoose.goosecurios.event;

import com.Polarice3.Goety.common.events.spell.CastMagicEvent;
import com.Polarice3.Goety.common.events.spell.CastingMagicEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Goety Mod 事件桥接器
 * 
 * <p>负责注册 Goety 模组的法术施放事件监听器
 * 
 * <p>监听的事件：
 * <ul>
 *   <li>CastMagicEvent：瞬发法术施放完成时触发（如火球术）</li>
 *   <li>CastingMagicEvent：持续施法过程中每tick触发（如召唤法术）</li>
 * </ul>
 * 
 * <p>用途：为九魔·九厄饰品提供施法加成机制
 * 
 * @author luckgoose
 */
public class GoetyEventBridge {

    /**
     * 注册 Goety 事件监听器
     * 
     * @param handler 九魔·九厄事件处理器
     */
    public static void register(NineCalamitiesEventHandler handler) {
        // 瞬发法术：施法完成时触发
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, CastMagicEvent.class, handler::handleCastMagic);
        
        // 持续施法：施法过程中每tick触发
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, CastingMagicEvent.class, handler::handleCastingMagic);
    }
}

