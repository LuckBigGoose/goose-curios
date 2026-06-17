package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.GooseCuriosMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 网络通道管理器
 * 
 * <p>负责注册所有客户端-服务器通信的数据包
 * 
 * <p>已注册的数据包：
 * <ul>
 *   <li>ShortStatusMessagePacket：短消息通知（S→C）</li>
 *   <li>NineCalamitiesCastBonusSyncPacket：九魔·九厄施法加成同步（S→C）</li>
 *   <li>BondWillSyncPacket：邦德的意志状态同步（S→C）</li>
 *   <li>BondWillFreezeSyncPacket：邦德的意志冻结同步（S→C）</li>
 *   <li>BondWillImpactPacket：邦德的意志冲击特效（S→C）</li>
 *   <li>BondWillSettingsUpdatePacket：邦德的意志设置更新（C→S）</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final Logger LOGGER = LoggerFactory.getLogger(ModNetwork.class);

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GooseCuriosMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有网络数据包
     * 
     * 【异常处理】
     * 如果注册失败，会记录错误日志并抛出运行时异常，
     * 防止模组在网络通信不完整的状态下运行
     */
    public static void register() {
        int id = 0;
        try {
            CHANNEL.registerMessage(id++, ShortStatusMessagePacket.class,
                    ShortStatusMessagePacket::encode,
                    ShortStatusMessagePacket::decode,
                    ShortStatusMessagePacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(id++, NineCalamitiesCastBonusSyncPacket.class,
                    NineCalamitiesCastBonusSyncPacket::encode,
                    NineCalamitiesCastBonusSyncPacket::decode,
                    NineCalamitiesCastBonusSyncPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(id++, BondWillSyncPacket.class,
                    BondWillSyncPacket::encode,
                    BondWillSyncPacket::decode,
                    BondWillSyncPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(id++, BondWillFreezeSyncPacket.class,
                    BondWillFreezeSyncPacket::encode,
                    BondWillFreezeSyncPacket::decode,
                    BondWillFreezeSyncPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(id++, BondWillImpactPacket.class,
                    BondWillImpactPacket::encode,
                    BondWillImpactPacket::decode,
                    BondWillImpactPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(id++, BondWillSettingsUpdatePacket.class,
                    BondWillSettingsUpdatePacket::encode,
                    BondWillSettingsUpdatePacket::decode,
                    BondWillSettingsUpdatePacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER));
            
            LOGGER.info("Successfully registered {} network packets", id);
        } catch (Exception e) {
            LOGGER.error("Failed to register network packets", e);
            throw new RuntimeException("Network packet registration failed", e);
        }
    }
}
