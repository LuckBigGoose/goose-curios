package com.luckgoose.goosecurios.init;

import com.luckgoose.goosecurios.GooseCuriosMod;
import com.luckgoose.goosecurios.item.BondWillItem;
import com.luckgoose.goosecurios.item.CyberPsychosisItem;
import com.luckgoose.goosecurios.item.NineDemonsNineCalamitiesRingItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组物品注册类
 * 
 * <p>负责注册所有饰品物品
 * 
 * @author luckgoose
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GooseCuriosMod.MOD_ID);

    /** 九魔·九厄戒指 */
    public static final RegistryObject<Item> NINE_DEMONS_NINE_CALAMITIES = ITEMS.register("nine_demons_nine_calamities",
            () -> new NineDemonsNineCalamitiesRingItem(new Item.Properties().stacksTo(1)));

    /** 赛博精神病戒指 */
    public static final RegistryObject<Item> CYBER_PSYCHOSIS = ITEMS.register("cyber_psychosis",
            () -> new CyberPsychosisItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** 邦德的意志戒指 */
    public static final RegistryObject<Item> BOND_WILL = ITEMS.register("bond_will",
            () -> new BondWillItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
