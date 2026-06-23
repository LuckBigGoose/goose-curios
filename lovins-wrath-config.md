# Lovin 的愤怒 JSON 配置说明

运行游戏后会生成配置文件：
`config/goose/goose-curios/lovins-wrath.json`

修改后重启游戏或服务器生效。如果这个文件已经生成过，更新模组不会自动覆盖旧配置；想使用新的默认值，需要手动修改或删除旧配置让它重新生成。

## 完整示例

```json
{
  "initial_wrath": 0,

  "allowed_curios": [],

  "allowed_weapons": [
    "minecraft:wooden_sword"
  ],

  "allowed_armor": [
    "minecraft:leather_helmet",
    "minecraft:leather_chestplate",
    "minecraft:leather_leggings",
    "minecraft:leather_boots"
  ],

  "growth_targets": [
    "minecraft:zombie",
    "minecraft:skeleton",
    "minecraft:slime",
    "minecraft:ender_dragon=25",
    "minecraft:wither=25",
    "minecraft:warden=25"
  ],

  "attribute_bonus": {
    "mode": "all",
    "base_percent": 1000.0,
    "per_wrath_percent": 500.0,
    "max_percent": -1,
    "operation": "multiply_base",
    "blacklist": [
      "forge:entity_gravity",
      "minecraft:generic.movement_speed"
    ],
    "custom": [
      {
        "id": "minecraft:generic.armor",
        "label": "护甲",
        "base_percent": 1000.0,
        "per_wrath_percent": 500.0,
        "max_percent": -1,
        "operation": "multiply_base"
      }
    ]
  }
}
```

## 字段说明

`initial_wrath`：初始怒意值。

`allowed_curios`：允许佩戴的饰品白名单。可填写物品 ID，也可填写物品标签，例如 `"#forge:gems"`。

`allowed_weapons`：允许造成伤害的主手武器白名单。默认只允许 `minecraft:wooden_sword`。

`allowed_armor`：允许穿戴的护甲白名单。默认是四件皮甲。可填写物品 ID，也可填写物品标签。

`growth_targets`：击杀后提供怒意的实体。写 `实体ID=怒意` 可以指定数值；不写 `=怒意` 时默认给 1 点。也支持实体标签，例如 `"#minecraft:skeletons=2"`。

`attribute_bonus.mode`：

- `all`：全属性模式，自动给所有已注册属性同一套百分比加成，并使用 `blacklist` 排除不想加成的属性。
- `custom`：自定义属性模式，只给 `custom` 里列出的属性加成。

`base_percent`：基础百分比。默认 `1000.0`，也就是 1000%。

`per_wrath_percent`：每点怒意增加的百分比。默认 `500.0`，也就是每点怒意增加 500%。

`max_percent`：最高百分比。填 `-1` 表示无上限。

`operation`：属性计算方式。可写 `multiply_base` 或 `multiply_total`，通常推荐 `multiply_base`。

`blacklist`：仅 `mode` 为 `all` 时使用，用于排除指定属性 ID。

`custom`：仅 `mode` 为 `custom` 时使用。每项都需要 `id`；`label` 可以不填，不填时使用属性自身的翻译名。
