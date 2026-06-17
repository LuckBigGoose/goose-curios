package com.luckgoose.goosecurios.network;

import com.luckgoose.goosecurios.compat.tacz.bondwill.BondWillSettings;
import com.luckgoose.goosecurios.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Set;
import java.util.function.Supplier;

public class BondWillSettingsUpdatePacket {
    
    /** 允许的设置键白名单，防止客户端注入非法数据 */
    private static final Set<String> ALLOWED_KEYS = Set.of(
        "TimeStopDesaturation",
        "TimeStopDistortion", 
        "ShotSound",
        "ShotEffect",
        "HitboxDisplay"
    );
    
    private final CompoundTag settings;

    public BondWillSettingsUpdatePacket(CompoundTag settings) {
        this.settings = settings.copy();
    }

    public static void encode(BondWillSettingsUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.settings);
    }

    public static BondWillSettingsUpdatePacket decode(FriendlyByteBuf buf) {
        CompoundTag settings = buf.readNbt();
        return new BondWillSettingsUpdatePacket(settings == null ? new CompoundTag() : settings);
    }

    public static void handle(BondWillSettingsUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) {
            return;
        }
        
        ctx.get().enqueueWork(() -> {
            // 安全验证:只允许白名单中的键,防止客户端注入恶意数据
            CompoundTag sanitizedSettings = sanitizeSettings(msg.settings);
            applyToHeldOrEquipped(player, sanitizedSettings);
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * 验证并清理客户端发送的设置，只保留白名单中的键
     * 
     * @param input 客户端发送的原始设置
     * @return 清理后的安全设置
     */
    private static CompoundTag sanitizeSettings(CompoundTag input) {
        CompoundTag sanitized = new CompoundTag();
        for (String key : ALLOWED_KEYS) {
            if (input.contains(key)) {
                // 只允许布尔值类型
                sanitized.putBoolean(key, input.getBoolean(key));
            }
        }
        return sanitized;
    }

    private static void applyToHeldOrEquipped(ServerPlayer player, CompoundTag settings) {
        ItemStack carried = player.containerMenu.getCarried();
        if (applyIfBondWill(carried, settings)) {
            player.containerMenu.broadcastChanges();
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (applyIfBondWill(stack, settings)) {
                player.containerMenu.broadcastChanges();
                return;
            }
        }
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(handler -> handler.findCurios(ModItems.BOND_WILL.get()).stream().findFirst().ifPresent(slotResult -> {
            BondWillSettings.applySettings(slotResult.stack(), settings);
            player.containerMenu.broadcastChanges();
        }));
    }

    private static boolean applyIfBondWill(ItemStack stack, CompoundTag settings) {
        if (stack.isEmpty() || !stack.is(ModItems.BOND_WILL.get())) {
            return false;
        }
        BondWillSettings.applySettings(stack, settings);
        return true;
    }
}
