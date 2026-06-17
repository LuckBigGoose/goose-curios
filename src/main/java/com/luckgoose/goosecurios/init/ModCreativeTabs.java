package com.luckgoose.goosecurios.init;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组创造模式标签页注册类
 * 
 * <p>负责注册创造模式物品栏标签页
 * 
 * @author luckgoose
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GooseCuriosMod.MOD_ID);

    /** Goose Curios 创造模式标签页 */
    public static final RegistryObject<CreativeModeTab> GOOSE_CURIOS_TAB = CREATIVE_MODE_TABS.register("goose_curios_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.NINE_DEMONS_NINE_CALAMITIES.get()))
            .title(Component.translatable("itemGroup.goose_curios"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.NINE_DEMONS_NINE_CALAMITIES.get());
                output.accept(ModItems.CYBER_PSYCHOSIS.get());
                output.accept(ModItems.BOND_WILL.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
