package com.luckgoose.goosecurios.client;

/**
 * 邦德的意志时停客户端特效管理器
 * 
 * <p>协调时停状态下的所有客户端视觉效果
 * 
 * <p>管理的特效：
 * <ul>
 *   <li>灰度后处理：降低饱和度、模糊、扭曲</li>
 * </ul>
 * 
 * @author luckgoose
 */
public final class BondWillTimeStopClientEffects {
    private BondWillTimeStopClientEffects() {
    }

    public static void tick() {
        BondWillGrayPostEffect.tick(BondWillClientDisplay.isTimeStopActive());
    }

    public static boolean areLocalVisualEffectsEnabled() {
        return BondWillClientDisplay.isTimeStopActive();
    }
}

