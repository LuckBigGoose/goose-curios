package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 邦德的意志射击冲击特效触发数据包
 * 
 * <p>服务端→客户端：通知客户端播放射击冲击特效
 * 
 * <p>触发时机：
 * <ul>
 *   <li>TACZ枪械命中实体时</li>
 *   <li>邦德的意志饰品激活状态</li>
 *   <li>客户端特效配置已启用</li>
 * </ul>
 * 
 * <p>特效内容：
 * <ul>
 *   <li>视觉：后处理着色器扭曲 + 3D冲击波圆环</li>
 *   <li>音效：爆裂音效（BOND_WILL_BOOM）</li>
 * </ul>
 * 
 * @author luckgoose
 */
public class BondWillImpactPacket {
    /** 被命中的实体ID（-1表示本地玩家） */
    private final int entityId;
    
    /** 蓄力进度（0.0-1.0，影响特效强度） */
    private final float progress;

    public BondWillImpactPacket(int entityId, float progress) {
        this.entityId = entityId;
        this.progress = progress;
    }

    public static void encode(BondWillImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.progress);
    }

    public static BondWillImpactPacket decode(FriendlyByteBuf buf) {
        return new BondWillImpactPacket(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(BondWillImpactPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleBondWillImpact(msg.entityId, msg.progress)));
        ctx.get().setPacketHandled(true);
    }
}

