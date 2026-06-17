package com.luckgoose.goosecurios.mixin.client;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * PostChain访问器
 * 
 * <p>访问 PostChain 的私有字段 passes，用于动态修改后处理特效参数
 * 
 * @author luckgoose
 */
@Mixin(PostChain.class)
public interface PostChainAccessor {
    @Accessor("passes")
    List<PostPass> goose_curios$getPasses();
}

