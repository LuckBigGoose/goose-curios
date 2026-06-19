package com.luckgoose.goosecurios;

import com.luckgoose.goosecurios.compat.tacz.BondWillTaczEvents;
import com.luckgoose.goosecurios.compat.tacz.CyberPsychosisTaczEvents;
import com.luckgoose.goosecurios.config.BondWillConfig;
import com.luckgoose.goosecurios.config.CyberPsychosisConfig;
import com.luckgoose.goosecurios.config.GooseClientConfig;
import com.luckgoose.goosecurios.config.NineCalamitiesConfig;
import com.luckgoose.goosecurios.event.GoetyEventBridge;
import com.luckgoose.goosecurios.event.NineCalamitiesEventHandler;
import com.luckgoose.goosecurios.init.ModCreativeTabs;
import com.luckgoose.goosecurios.init.ModEffects;
import com.luckgoose.goosecurios.init.ModItems;
import com.luckgoose.goosecurios.init.ModSounds;
import com.luckgoose.goosecurios.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goose Curios - 主模组类
 * 
 * - 邦德的意志（Bond Will）：TACZ附属，时停、隐身、增伤
 * - 九魔·九厄（Nine Calamities）：Goety附属，根据魔杖和施法获得加成
 * - 赛博精神病（Cyber Psychosis）：TACZ附属，移除原版的爆头，累计叠加触发爆头
 * 
 * @author luckgoose
 * @version 1.0.0
 */
@Mod(GooseCuriosMod.MOD_ID)
public class GooseCuriosMod {

    public static final String MOD_ID = "goose_curios";
    
    /**
     * 日志记录器
     * 用于记录模组初始化、错误和调试信息
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GooseCuriosMod.class);

    /**
     * 构造函数：初始化模组
     * 
     * 执行顺序：注册器 → 网络 → 配置 → 事件
     */
    public GooseCuriosMod() {
        LOGGER.info("Initializing Goose Curios Mod v1.0.0");
        
        try {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            ModItems.register(modBus);
            ModEffects.register(modBus);
            ModSounds.register(modBus);
            ModCreativeTabs.register(modBus);
            LOGGER.debug("Registered deferred registries");
            
            ModNetwork.register();
            LOGGER.debug("Registered network channel");

            registerConfigs();
            LOGGER.debug("Registered configuration files");

            registerEventHandlers();
            LOGGER.info("Goose Curios Mod initialization completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Goose Curios Mod", e);
            throw new RuntimeException("Goose Curios Mod initialization failed", e);
        }
    }
    
    /**
     * 注册配置文件
     */
    private void registerConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, 
                NineCalamitiesConfig.SPEC, "goose/goose-curios/nine-calamities.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, 
                CyberPsychosisConfig.SPEC, "goose/goose-curios/cyber-psychosis.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, 
                BondWillConfig.SPEC, "goose/goose-curios/bond-will.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, 
                GooseClientConfig.SPEC, "goose/goose-curios/client.toml");
    }
    
    /**
     * 注册事件处理器
     */
    private void registerEventHandlers() {
        NineCalamitiesEventHandler nineCalamitiesEventHandler = new NineCalamitiesEventHandler();
        MinecraftForge.EVENT_BUS.register(nineCalamitiesEventHandler);
        GoetyEventBridge.register(nineCalamitiesEventHandler);
        
        MinecraftForge.EVENT_BUS.register(new CyberPsychosisTaczEvents());
        MinecraftForge.EVENT_BUS.register(new BondWillTaczEvents());
        
        LOGGER.debug("Registered event handlers for all curio items");
    }
}
