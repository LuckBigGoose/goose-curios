package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 九魔·九厄施法加成同步数据包
 * 
 * <p>服务端→客户端：同步施法加成状态（流派列表和过期时间）
 * 
 * <p>数据内容：
 * <ul>
 *   <li>until：施法加成过期的游戏时间戳</li>
 *   <li>types：已激活的法术流派列表（最多16个）</li>
 *   <li>version：版本号，防止过期数据覆盖新数据</li>
 * </ul>
 * 
 * <p>安全限制：
 * <ul>
 *   <li>最多传输16个流派类型</li>
 *   <li>单个流派名称最长64字符</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class NineCalamitiesCastBonusSyncPacket {

    /** 最大传输的流派类型数量 */
    private static final int MAX_TYPES = 16;
    
    /** 单个流派名称最大长度 */
    private static final int MAX_TYPE_LENGTH = 64;

    /** 施法加成过期时间（游戏时间戳） */
    private final long until;
    
    /** 已激活的法术流派列表 */
    private final List<String> types;
    
    /** 版本号（用于防止旧数据覆盖） */
    private final long version;

    public NineCalamitiesCastBonusSyncPacket(long until, List<String> types, long version) {
        this.until = until;
        this.types = types;
        this.version = version;
    }

    public static void encode(NineCalamitiesCastBonusSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.until);
        List<String> types = msg.types.size() > MAX_TYPES ? msg.types.subList(0, MAX_TYPES) : msg.types;
        buf.writeVarInt(types.size());
        for (String type : types) {
            buf.writeUtf(type, MAX_TYPE_LENGTH);
        }
        buf.writeLong(msg.version);
    }

    public static NineCalamitiesCastBonusSyncPacket decode(FriendlyByteBuf buf) {
        long until = buf.readLong();
        int size = buf.readVarInt();
        List<String> types = new ArrayList<>(Math.min(size, MAX_TYPES));
        for (int i = 0; i < size; i++) {
            String type = buf.readUtf(MAX_TYPE_LENGTH);
            if (i < MAX_TYPES) {
                types.add(type);
            }
        }
        long version = buf.readLong();
        return new NineCalamitiesCastBonusSyncPacket(until, types, version);
    }

    public static void handle(NineCalamitiesCastBonusSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleNineCalamitiesCastBonus(msg.until, msg.types, msg.version)));
        ctx.get().setPacketHandled(true);
    }
}

