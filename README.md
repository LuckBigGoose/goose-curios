# Goose Curios

Goose Curios 是一个面向 Minecraft 1.20.1 Forge 的 Curios 饰品模组，提供与 TACZ、Goety 联动的特殊饰品机制。

## 饰品

### 承载着邦德的意志

与 TACZ 联动的饰品。持枪瞄准时可进入隐匿表现，脱战后累积伤害加成，并可在满足条件时触发时停相关效果。该饰品支持在背包中右键打开设置界面。

### 九魔·九厄

与 Goety 联动的饰品。佩戴后围绕诡厄巫法魔杖构建战斗收益，快捷栏内不同魔杖可提供伤害加成，施放对应派系聚晶后可获得临时强化。

### 赛博精神病

与 TACZ 联动的饰品。将爆头收益调整为累积概率触发机制，未触发时提高后续触发概率。

## 依赖

- Minecraft 1.20.1
- Forge 47.3.0+
- Curios API 5.x
- Goety 2.5.0+
- TACZ 1.1.4+

## 配置

配置文件位于 `.minecraft/config/goose/goose-curios/`。客户端表现、饰品数值与联动行为可按配置项调整。

## 构建

```bash
git clone https://github.com/LuckBigGoose/goose-curios.git
cd goose-curios
./gradlew build
```

Windows 可使用：

```powershell
.\gradlew.bat build
```

构建前需要将以下依赖放入 `libs/` 目录：

```text
goety-2.5.36.1-new_texture.jar
tacz-1.20.1-1.1.4-hotfix-all.jar
```

## 反馈与贡献

欢迎通过 Issue 反馈问题、兼容性异常或平衡性建议。提交 Pull Request 时，请说明修改范围、测试环境和验证结果。

## 许可证

本项目基于 MIT License 开源。
