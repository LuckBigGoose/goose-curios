package com.luckgoose.goosecurios.init;

import com.luckgoose.goosecurios.GooseCuriosMod;
import com.luckgoose.goosecurios.effect.BondVanishingEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组药水效果注册类
 * 
 * <p>负责注册所有自定义药水效果
 * 
 * @author luckgoose
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GooseCuriosMod.MOD_ID);

    /** 邦德的意志隐身效果 */
    public static final RegistryObject<MobEffect> BOND_VANISHING = EFFECTS.register("bond_vanishing", BondVanishingEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}

