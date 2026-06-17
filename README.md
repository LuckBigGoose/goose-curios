# Goose Curios

一个为 Minecraft 1.20.1 Forge 制作的 Curios 饰品模组。

## 简介

添加了三种饰品，分别与 TACZ 和 Goety 模组联动。

### 饰品列表

**承载着邦德的意志（Bond Will）**
- 与 TACZ 联动的特工主题饰品
- 持枪瞄准时隐身
- 脱战后累积伤害加成
- 满值瞄准时触发时停效果
- 可在背包右键打开设置界面

**九魔·九厄（Nine Calamities）**
- 与 Goety 联动的巫法饰品
- 佩戴后只能使用诡厄巫法魔杖战斗
- 快捷栏内每种魔杖提供伤害加成
- 施放对应派系聚晶后获得临时加成

**赛博精神病（Cyber Psychosis）**
- 与 TACZ 联动的枪械饰品
- 改变爆头判定为累积概率触发
- 每次未触发会提高下次概率

## 依赖

必需模组：
- Minecraft 1.20.1
- Forge 47.3.0+
- Curios API 5.14.1+
- Goety 2.5.0+
- TACZ 1.1.4+

依赖链接：
- [Curios API](https://github.com/TheIllusiveC4/Curios/tree/1.20.x)
- [Goety 2](https://github.com/Polarice3/Goety-2)
- [TACZ](https://github.com/MCModderAnchor/TACZ)

## 安装

1. 安装 Forge 1.20.1-47.3.0+
2. 下载并安装所有依赖模组
3. 将本模组放入 mods 文件夹

## 配置

配置文件位于 `.minecraft/config/goose/goose-curios/` 目录。

## 构建

```bash
git clone https://github.com/LuckBigGoose/goose-curios.git
cd goose-curios
./gradlew build
```

**注意**：构建前需要手动下载以下依赖并放入 `libs/` 目录：
- `goety-2.5.36.1-new_texture.jar`
- `tacz-1.20.1-1.1.4-hotfix-all.jar`

## 贡献

欢迎提交 Issue 和 Pull Request。

## 许可证

MIT License

## 作者

LuckGoose
