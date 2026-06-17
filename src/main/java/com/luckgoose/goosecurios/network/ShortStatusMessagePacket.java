package com.luckgoose.goosecurios.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 短消息通知数据包
 * 
 * <p>服务端→客户端：在屏幕上显示短暂的状态消息
 * 
 * <p>用途：通知玩家饰品相关的状态变化（装备、卸载、特殊事件等）
 * 
 * @author luckgoose
 */
public class ShortStatusMessagePacket {

    /** 消息内容 */
    private final Component message;
    
    /** 显示持续时间（tick） */
    private final int durationTicks;

    public ShortStatusMessagePacket(Component message, int durationTicks) {
        this.message = message;
        this.durationTicks = durationTicks;
    }

    public static void encode(ShortStatusMessagePacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.message);
        buf.writeVarInt(msg.durationTicks);
    }

    public static ShortStatusMessagePacket decode(FriendlyByteBuf buf) {
        return new ShortStatusMessagePacket(buf.readComponent(), buf.readVarInt());
    }

    public static void handle(ShortStatusMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlerInvoker.invoke("handleShortStatusMessage", new Class<?>[]{Component.class, int.class}, msg.message, msg.durationTicks)));
        ctx.get().setPacketHandled(true);
    }
}

