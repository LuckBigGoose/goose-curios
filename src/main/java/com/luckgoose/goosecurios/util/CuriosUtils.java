package com.luckgoose.goosecurios.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios API工具类
 * 提供通用的Curios物品操作方法
 */
public class CuriosUtils {
    
    /**
     * 统计玩家装备的指定物品数量
     * 
     * @param player 服务端玩家
     * @param item 要统计的物品
     * @return 装备数量
     */
    public static int countEquippedCurios(ServerPlayer player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(handler -> handler.findCurios(item).size())
                .orElse(0);
    }
}
