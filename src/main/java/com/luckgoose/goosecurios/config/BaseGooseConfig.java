package com.luckgoose.goosecurios.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置基类
 * 
 * 提供配置文件的通用构建方法
 */
public abstract class BaseGooseConfig {

    /**
     * 创建配置构建器并推入配置节
     * 
     * @param section 配置节名称
     * @return 配置构建器
     */
    protected static ForgeConfigSpec.Builder createBuilder(String section) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(section);
        return builder;
    }

    /**
     * 弹出配置节并构建配置规范
     * 
     * @param builder 配置构建器
     * @return 配置规范
     */
    protected static ForgeConfigSpec build(ForgeConfigSpec.Builder builder) {
        builder.pop();
        return builder.build();
    }

    /**
     * 添加配置注释的辅助方法
     * 
     * @param builder 配置构建器
     * @param comments 注释内容（可变参数）
     */
    protected static void addComment(ForgeConfigSpec.Builder builder, String... comments) {
        for (String comment : comments) {
            builder.comment(comment);
        }
    }
}
