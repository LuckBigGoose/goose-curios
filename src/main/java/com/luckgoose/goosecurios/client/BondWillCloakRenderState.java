package com.luckgoose.goosecurios.client;

import com.luckgoose.goosecurios.init.ModEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class BondWillCloakRenderState {
    public static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    private BondWillCloakRenderState() {
    }

    public static boolean shouldCloak(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.BOND_VANISHING.get());
    }

    public static float handAlpha() {
        return 0.22F;
    }

    public static float red() {
        return 0.55F;
    }

    public static float green() {
        return 0.78F;
    }

    public static float blue() {
        return 0.95F;
    }

    public static ResourceLocation cloakTexture() {
        return WHITE_TEXTURE;
    }

    public static int light() {
        return LightTexture.pack(8, 8);
    }
}

