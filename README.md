# Goose Curios

<div align="center">

**一个为 Minecraft 1.20.1 制作的 Curios 饰品模组**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.3.0-orange.svg)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## 📖 简介

Goose Curios 为 Minecraft 添加了三种强大的饰品，每个饰品都与特定的模组联动，提供独特的游戏机制和视觉效果。

### 三大饰品

#### 🕰️ 承载着邦德的意志（Bond Will）
> *"袖扣合拢，保险解除。优雅不是礼节，是开火前的沉默。"*

**TACZ 联动饰品**，特工主题的时停与隐身系统：
- **隐匿潜行**：持枪瞄准时获得隐身效果，敌人无法察觉
- **伤害积蓄**：脱战后逐渐累积伤害加成
- **时停瞄准**：满值瞄准时触发时间停止效果，锁定目标
- **战斗惩罚**：进入战斗后伤害降低，鼓励一击必杀
- **可自定义特效**：支持背包右键打开设置界面，调整时停失色、画面扭曲、音效等

#### 🔮 九魔·九厄（Nine Calamities）
> *"一杖起祸乱，一咒覆山河，世间万般术，皆俯首入我魔。"*

**Goety 联动饰品**，诡厄巫法专精系统：
- **魔杖专精**：佩戴后只能使用 Goety 的诡厄巫法魔杖战斗
- **魔杖加成**：快捷栏内每种魔杖提供递减的伤害加成
- **施法共鸣**：施放对应派系的聚晶后获得临时伤害加成
- **层数叠加**：不同派系的施法加成可独立叠加
- **HUD 显示**：实时显示当前有效魔杖数量和加成详情

#### 🎯 赛博精神病（Cyber Psychosis）
> *"子非枪，安知枪之所指？子非颅，安知颅之所在？"*

**TACZ 联动饰品**，概率爆头系统：
- **重新定义爆头**：移除原版位置判定，改为累积概率触发
- **概率累积**：每次未触发爆头会提高下次概率
- **概率重置**：触发爆头后概率归零
- **冷却机制**：一定时间不开枪后概率重置

## 🔧 依赖模组

### 必需依赖
- [Minecraft](https://www.minecraft.net/) 1.20.1
- [Forge](https://files.minecraftforge.net/) 47.3.0+
- [Curios API](https://github.com/TheIllusiveC4/Curios/tree/1.20.x) 5.14.1+
- [Goety 2](https://github.com/Polarice3/Goety-2) 2.5.0+
- [TACZ](https://github.com/MCModderAnchor/TACZ) 1.1.4+

### 可选依赖
- Mixin 0.8.5（已包含在 Forge 中）

## 📥 安装

1. 确保已安装 Forge 1.20.1-47.3.0 或更高版本
2. 下载并安装所有必需的依赖模组
3. 将 `goose-curios-x.x.x.jar` 放入 `mods` 文件夹
4. 启动游戏

## ⚙️ 配置

模组配置文件位于 `.minecraft/config/goose/goose-curios/` 目录下：

- `bond-will.toml` - 邦德的意志配置
- `nine-calamities.toml` - 九魔·九厄配置  
- `cyber-psychosis.toml` - 赛博精神病配置
- `client.toml` - 客户端配置

部分配置支持游戏内调整（如邦德的意志的特效开关）。

## 🎮 使用方法

1. 在创造模式物品栏的"大鹅饰品"分类中找到三种饰品
2. 打开 Curios 界面（默认快捷键 `G`）
3. 将饰品放入对应的饰品槽位
4. 查看物品描述了解详细效果
5. 部分饰品支持右键打开设置（如邦德的意志）

## 🛠️ 开发构建

### 环境要求
- JDK 17
- Gradle 8.8+

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/LuckBigGoose/goose-curios.git
cd goose-curios

# 构建项目
./gradlew build

# 输出文件位于 build/libs/
```

**注意**：由于依赖库文件较大且可能涉及版权问题，仓库中未包含 `libs/` 目录下的 jar 文件。构建前需要手动下载以下文件并放入 `libs/` 目录：
- `goety-2.5.36.1-new_texture.jar` - [从 Goety 2 仓库获取](https://github.com/Polarice3/Goety-2)
- `tacz-1.20.1-1.1.4-hotfix-all.jar` - [从 TACZ 仓库获取](https://github.com/MCModderAnchor/TACZ)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

在提交 PR 之前，请确保：
- 代码符合项目现有的代码风格
- 新增功能有清晰的注释说明
- 测试过功能在游戏中正常运行

## 📝 更新日志

### v1.0.0 (2026)
- 首次发布
- 添加承载着邦德的意志饰品
- 添加九魔·九厄饰品
- 添加赛博精神病饰品

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 👤 作者

**LuckGoose**

## 🙏 致谢

- [Curios API](https://github.com/TheIllusiveC4/Curios) - 提供饰品系统
- [Goety 2](https://github.com/Polarice3/Goety-2) - 提供诡厄巫法系统
- [TACZ](https://github.com/MCModderAnchor/TACZ) - 提供枪械系统

---

<div align="center">

**如果你喜欢这个模组，欢迎给个 ⭐ Star！**

</div>
