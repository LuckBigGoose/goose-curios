package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 邦德的意志时停冻结同步数据包
 * 
 * <p>服务端→客户端：同步实体的冻结状态和快照位置
 * 
 * <p>数据内容：
 * <ul>
 *   <li>实体标识：entityId + UUID 双重标识</li>
 *   <li>冻结状态：是否处于时停状态</li>
 *   <li>快照位置：x/y/z 坐标和旋转角度</li>
 * </ul>
 * 
 * <p>用途：在时停期间冻结实体渲染位置，防止客户端预测导致的移动
 * 
 * @author luckgoose
 */
public class BondWillFreezeSyncPacket {
    /** 实体网络ID */
    private final int entityId;
    
    /** 实体UUID（用于跨区块加载时的持久化标识） */
    private final UUID entityUuid;
    
    /** 是否冻结 */
    private final boolean frozen;
    
    /** 快照位置X */
    private final double x;
    
    /** 快照位置Y */
    private final double y;
    
    /** 快照位置Z */
    private final double z;
    
    /** 快照旋转pitch */
    private final float xRot;
    
    /** 快照旋转yaw */
    private final float yRot;

    public BondWillFreezeSyncPacket(int entityId, UUID entityUuid, boolean frozen, double x, double y, double z, float xRot, float yRot) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.frozen = frozen;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
    }

    public static void encode(BondWillFreezeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUUID(msg.entityUuid);
        buf.writeBoolean(msg.frozen);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.xRot);
        buf.writeFloat(msg.yRot);
    }

    public static BondWillFreezeSyncPacket decode(FriendlyByteBuf buf) {
        return new BondWillFreezeSyncPacket(buf.readVarInt(), buf.readUUID(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(BondWillFreezeSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleBondWillFreeze(msg.entityId, msg.entityUuid, msg.frozen, msg.x, msg.y, msg.z, msg.xRot, msg.yRot)));
        ctx.get().setPacketHandled(true);
    }
}

