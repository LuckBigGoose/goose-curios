package com.luckgoose.goosecurios.init;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组音效注册类
 * 
 * <p>负责注册所有自定义音效
 * 
 * @author luckgoose
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GooseCuriosMod.MOD_ID);

    /** 邦德的意志射击冲击音效 */
    public static final RegistryObject<SoundEvent> BOND_WILL_BOOM = SOUND_EVENTS.register("bond_will_boom",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GooseCuriosMod.MOD_ID, "bond_will_boom")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
