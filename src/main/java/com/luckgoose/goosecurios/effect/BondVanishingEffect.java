package com.luckgoose.goosecurios.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 邦德的意志隐身效果
 * 
 * <p>由邦德的意志饰品在满足条件时施加的隐身效果
 * 
 * <p>触发条件：
 * <ul>
 *   <li>脱离战斗（100 tick 未造成有效枪械伤害）</li>
 *   <li>持枪瞄准（瞄准进度 ≥ 90%）</li>
 *   <li>不移动或缓慢移动</li>
 * </ul>
 * 
 * <p>效果机制：通过 Mixin 修改玩家可见性（BondVanishingVisibilityMixin）
 * 
 * @author luckgoose
 */
public class BondVanishingEffect extends MobEffect {

    public BondVanishingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x111111);
    }
}

