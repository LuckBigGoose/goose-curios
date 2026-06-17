package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.compat.tacz.BondWillHeadshotRenderer;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLevelStageEvent;

final class TaczClientCompat {

    private TaczClientCompat() {
    }

    static boolean mainhandHoldGun(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return IGun.mainhandHoldGun(entity);
    }

    static void renderHeadshot(RenderLevelStageEvent event) {
        BondWillHeadshotRenderer.render(event);
    }
}

