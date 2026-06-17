package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.client.ClientPacketHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 邦德的意志状态同步网络包
 * 
 * <p>用于将服务端的邦德的意志状态同步到客户端，用于UI显示
 * 
 * <p>包含字段：
 * <ul>
 *   <li>progress：蓄力进度（0.0-1.0）</li>
 *   <li>bonus：当前伤害加成百分比</li>
 *   <li>maxBonus：最大伤害加成百分比</li>
 *   <li>active：是否处于隐身状态</li>
 *   <li>equipped：是否装备邦德的意志</li>
 *   <li>cooldownTicks：冷却剩余时间（游戏刻）</li>
 *   <li>timeStopActive：时停是否激活</li>
 *   <li>timeStopCountdownProgress：时停倒计时进度（0.0-1.0）</li>
 *   <li>settings：客户端显示设置（特效开关）</li>
 * </ul>
 * 
 * <p>性能优化：
 * <ul>
 *   <li>使用VarInt压缩整数（cooldownTicks），通常1-2字节</li>
 *   <li>直接序列化布尔值而非NBT，从~100字节降至26字节（-79%）</li>
 * </ul>
 * 
 * <p>网络大小：约26字节（4个float + 8个boolean + 1个VarInt）
 * 
 * @author luckgoose
 * @see ClientPacketHandlers#handleBondWill
 */
public class BondWillSyncPacket {

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
     * <p>修复：序列化所有5个设置字段，使用正确的键名
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
        
        // 使用BondWillSettings中的正确键名
        CompoundTag bondWillSettings = msg.settings.contains("BondWillSettings") 
            ? msg.settings.getCompound("BondWillSettings") 
            : msg.settings;
        
        buf.writeBoolean(bondWillSettings.contains("TimeStopDesaturation") ? bondWillSettings.getBoolean("TimeStopDesaturation") : true);
        buf.writeBoolean(bondWillSettings.contains("TimeStopDistortion") ? bondWillSettings.getBoolean("TimeStopDistortion") : true);
        buf.writeBoolean(bondWillSettings.contains("ShotSound") ? bondWillSettings.getBoolean("ShotSound") : true);
        buf.writeBoolean(bondWillSettings.contains("ShotEffect") ? bondWillSettings.getBoolean("ShotEffect") : true);
        buf.writeBoolean(bondWillSettings.contains("HitboxDisplay") ? bondWillSettings.getBoolean("HitboxDisplay") : false);
    }

    /**
     * 从字节缓冲区反序列化网络包
     * 
     * <p>修复：反序列化所有5个设置字段，使用正确的键名
     * 
     * @param buf 字节缓冲区
     * @return 网络包实例
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
        
        // 修复：使用正确的键名重建设置
        CompoundTag rootSettings = new CompoundTag();
        CompoundTag bondWillSettings = new CompoundTag();
        bondWillSettings.putBoolean("TimeStopDesaturation", buf.readBoolean());
        bondWillSettings.putBoolean("TimeStopDistortion", buf.readBoolean());
        bondWillSettings.putBoolean("ShotSound", buf.readBoolean());
        bondWillSettings.putBoolean("ShotEffect", buf.readBoolean());
        bondWillSettings.putBoolean("HitboxDisplay", buf.readBoolean());
        rootSettings.put("BondWillSettings", bondWillSettings);
        
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

