package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.client.ClientPacketHandlers;
import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 邦德的意志状态同步网络包
 *
 * 用于将服务端状态同步到客户端，用于UI显示
 *
 * @author luckgoose
 */
public class BondWillSyncPacket {

    private static final Logger LOGGER = LoggerFactory.getLogger(BondWillSyncPacket.class);

    /** 蓄力进度：0.0（未蓄力）到1.0（满蓄力） */
    private final float progress;

    /** 当前伤害加成百分比（例如0.5表示50%加成） */
    private final float bonus;

    /** 最大伤害加成百分比 */
    private final float maxBonus;

    /** 是否处于隐身状态 */
    private final boolean active;

    /** 是否装备邦德的意志 */
    private final boolean equipped;

    /** 冷却剩余时间（游戏刻） */
    private final int cooldownTicks;

    /** 时停是否激活 */
    private final boolean timeStopActive;

    /** 时停倒计时进度：0.0（刚开始）到1.0（即将结束） */
    private final float timeStopCountdownProgress;

    /** 客户端显示设置：特效开关（enableTimeStop, enableGrayScale, enableShotEffect, enableShotSound） */
    private final CompoundTag settings;

    /**
     * 简化构造函数：只包含基本字段
     *
     * @param progress 蓄力进度
     * @param bonus 伤害加成
     * @param maxBonus 最大加成
     * @param active 是否隐身
     */
    public BondWillSyncPacket(float progress, float bonus, float maxBonus, boolean active) {
        this(progress, bonus, maxBonus, active, active, 0);
    }

    /**
     * 构造函数：包含装备状态和冷却
     *
     * @param progress 蓄力进度
     * @param bonus 伤害加成
     * @param maxBonus 最大加成
     * @param active 是否隐身
     * @param equipped 是否装备
     * @param cooldownTicks 冷却剩余时间
     */
    public BondWillSyncPacket(float progress, float bonus, float maxBonus, boolean active, boolean equipped, int cooldownTicks) {
        this(progress, bonus, maxBonus, active, equipped, cooldownTicks, false, 0.0F);
    }

    /**
     * 构造函数：包含时停状态
     *
     * @param progress 蓄力进度
     * @param bonus 伤害加成
     * @param maxBonus 最大加成
     * @param active 是否隐身
     * @param equipped 是否装备
     * @param cooldownTicks 冷却剩余时间
     * @param timeStopActive 时停是否激活
     * @param timeStopCountdownProgress 时停倒计时进度
     */
    public BondWillSyncPacket(float progress, float bonus, float maxBonus, boolean active, boolean equipped, int cooldownTicks, boolean timeStopActive, float timeStopCountdownProgress) {
        this(progress, bonus, maxBonus, active, equipped, cooldownTicks, timeStopActive, timeStopCountdownProgress, new CompoundTag());
    }

    /**
     * 完整构造函数：包含所有字段
     *
     * @param progress 蓄力进度
     * @param bonus 伤害加成
     * @param maxBonus 最大加成
     * @param active 是否隐身
     * @param equipped 是否装备
     * @param cooldownTicks 冷却剩余时间
     * @param timeStopActive 时停是否激活
     * @param timeStopCountdownProgress 时停倒计时进度
     * @param settings 客户端显示设置
     */
    public BondWillSyncPacket(float progress, float bonus, float maxBonus, boolean active, boolean equipped, int cooldownTicks, boolean timeStopActive, float timeStopCountdownProgress, CompoundTag settings) {
        this.progress = progress;
        this.bonus = bonus;
        this.maxBonus = maxBonus;
        this.active = active;
        this.equipped = equipped;
        this.cooldownTicks = cooldownTicks;
        this.timeStopActive = timeStopActive;
        this.timeStopCountdownProgress = timeStopCountdownProgress;
        this.settings = settings.copy();
    }

    /**
     * 序列化网络包到字节缓冲区
     *
     * <p>性能优化：
     * <ul>
     *   <li>使用VarInt压缩整数：cooldownTicks通常&lt;200，只需1-2字节</li>
     *   <li>直接序列化布尔值：避免NBT的标签开销</li>
     * </ul>
     *
     * <p>G2 修复：设置字段键名复用 {@link BondWillSettings} 常量，避免硬编码重复
     *
     * @param msg 网络包
     * @param buf 字节缓冲区
     */
    public static void encode(BondWillSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.progress);
        buf.writeFloat(msg.bonus);
        buf.writeFloat(msg.maxBonus);
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.equipped);
        buf.writeVarInt(msg.cooldownTicks);
        buf.writeBoolean(msg.timeStopActive);
        buf.writeFloat(msg.timeStopCountdownProgress);

        CompoundTag bondWillSettings = msg.settings.contains(BondWillSettings.ROOT)
            ? msg.settings.getCompound(BondWillSettings.ROOT)
            : msg.settings;

        buf.writeBoolean(bondWillSettings.contains(BondWillSettings.TIME_STOP_DESATURATION) ? bondWillSettings.getBoolean(BondWillSettings.TIME_STOP_DESATURATION) : true);
        buf.writeBoolean(bondWillSettings.contains(BondWillSettings.TIME_STOP_DISTORTION) ? bondWillSettings.getBoolean(BondWillSettings.TIME_STOP_DISTORTION) : true);
        buf.writeBoolean(bondWillSettings.contains(BondWillSettings.SHOT_SOUND) ? bondWillSettings.getBoolean(BondWillSettings.SHOT_SOUND) : true);
        buf.writeBoolean(bondWillSettings.contains(BondWillSettings.SHOT_EFFECT) ? bondWillSettings.getBoolean(BondWillSettings.SHOT_EFFECT) : true);
        buf.writeBoolean(bondWillSettings.contains(BondWillSettings.HITBOX_DISPLAY) ? bondWillSettings.getBoolean(BondWillSettings.HITBOX_DISPLAY) : false);
    }

    /**
     * 从网络缓冲区解码数据包
     * 
     * 读取顺序必须与encode()严格一致
     * 
     * @param buf 网络缓冲区
     * @return 解码后的数据包
     */
    public static BondWillSyncPacket decode(FriendlyByteBuf buf) {
        float progress = buf.readFloat();
        float bonus = buf.readFloat();
        float maxBonus = buf.readFloat();
        boolean active = buf.readBoolean();
        boolean equipped = buf.readBoolean();
        int cooldownTicks = buf.readVarInt();
        boolean timeStopActive = buf.readBoolean();
        float timeStopCountdownProgress = buf.readFloat();
        
        // 读取settings：与encode()中的5个布尔值对应
        CompoundTag rootSettings = new CompoundTag();
        CompoundTag bondWillSettings = new CompoundTag();
        
        bondWillSettings.putBoolean(BondWillSettings.TIME_STOP_DESATURATION, buf.readBoolean());
        bondWillSettings.putBoolean(BondWillSettings.TIME_STOP_DISTORTION, buf.readBoolean());
        bondWillSettings.putBoolean(BondWillSettings.SHOT_SOUND, buf.readBoolean());
        bondWillSettings.putBoolean(BondWillSettings.SHOT_EFFECT, buf.readBoolean());
        bondWillSettings.putBoolean(BondWillSettings.HITBOX_DISPLAY, buf.readBoolean());
        
        rootSettings.put(BondWillSettings.ROOT, bondWillSettings);
        
        return new BondWillSyncPacket(progress, bonus, maxBonus, active, equipped, cooldownTicks, timeStopActive, timeStopCountdownProgress, rootSettings);
    }

    /**
     * 处理网络包：在客户端执行
     *
     * @param msg 网络包
     * @param ctx 网络上下文
     */
    public static void handle(BondWillSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleBondWill(msg.progress, msg.bonus, msg.maxBonus, msg.active, msg.equipped, msg.cooldownTicks, msg.timeStopActive, msg.timeStopCountdownProgress, msg.settings)));
        ctx.get().setPacketHandled(true);
    }
}
