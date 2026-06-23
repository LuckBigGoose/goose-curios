package com.luckgoose.goosecurios.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * lovin的愤怒 JSON 配置。
 */
public final class LovinsWrathConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LovinsWrathConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "goose/goose-curios";
    private static final String CONFIG_FILE = "lovins-wrath.json";
    private static final String ALL_ATTRIBUTES_LABEL_KEY = "tooltip.goose_curios.lovins_wrath.growth.all_attributes.label";
    private static final int DEFAULT_TARGET_POINTS = 1;

    private static final String DEFAULT_JSON = """
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
                  },
                  {
                    "id": "minecraft:generic.armor_toughness",
                    "label": "盔甲韧性",
                    "base_percent": 1000.0,
                    "per_wrath_percent": 500.0,
                    "max_percent": -1,
                    "operation": "multiply_base"
                  },
                  {
                    "id": "minecraft:generic.max_health",
                    "label": "最大生命值",
                    "base_percent": 1000.0,
                    "per_wrath_percent": 500.0,
                    "max_percent": -1,
                    "operation": "multiply_base"
                  },
                  {
                    "id": "minecraft:generic.knockback_resistance",
                    "label": "击退抗性",
                    "base_percent": 1000.0,
                    "per_wrath_percent": 500.0,
                    "max_percent": -1,
                    "operation": "multiply_base"
                  }
                ]
              }
            }
            """;

    private static volatile ConfigData cachedConfig;
    private static volatile long cachedLastModified = Long.MIN_VALUE;

    private LovinsWrathConfig() {
    }

    public static void load() {
        config();
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR).resolve(CONFIG_FILE);
    }

    public static int initialWrath() {
        return config().initial_wrath;
    }

    public static boolean isAllowedCurio(ItemStack stack) {
        return !stack.isEmpty() && config().allowedCurios.matches(stack);
    }

    public static boolean isAllowedWeapon(ItemStack stack) {
        return !stack.isEmpty() && config().allowedWeapons.matches(stack);
    }

    public static boolean isAllowedArmor(ItemStack stack) {
        return !stack.isEmpty() && config().allowedArmor.matches(stack);
    }

    public static boolean isGrowthTarget(EntityType<?> entityType) {
        return config().growthTargets.matches(entityType);
    }

    public static int getGrowthPoints(EntityType<?> entityType) {
        return config().growthTargets.points(entityType);
    }

    public static AttributeBonus attributeBonus() {
        return config().attribute_bonus;
    }

    public static List<ConfiguredAttribute> attributes() {
        return attributeBonus().attributes();
    }

    private static ConfigData config() {
        Path path = configPath();
        long modified = lastModified(path);
        ConfigData current = cachedConfig;
        if (current != null && modified == cachedLastModified) {
            return current;
        }

        synchronized (LovinsWrathConfig.class) {
            modified = lastModified(path);
            if (cachedConfig != null && modified == cachedLastModified) {
                return cachedConfig;
            }

            ConfigData loaded = readConfig(path);
            loaded.prepare();
            cachedConfig = loaded;
            cachedLastModified = lastModified(path);
            return loaded;
        }
    }

    private static ConfigData readConfig(Path path) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_JSON, StandardCharsets.UTF_8);
            }

            ConfigData parsed = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), ConfigData.class);
            return parsed == null ? ConfigData.defaults() : parsed;
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Failed to read Lovin's Wrath config, using defaults: {}", path, e);
            return ConfigData.defaults();
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : Long.MIN_VALUE;
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResourceLocation parseId(String entry) {
        String id = trimToEmpty(entry);
        if (id.startsWith("#")) {
            id = id.substring(1).trim();
        }
        return id.isEmpty() ? null : ResourceLocation.tryParse(id);
    }

    private static boolean isTagEntry(String entry) {
        return trimToEmpty(entry).startsWith("#");
    }

    private static AttributeModifier.Operation operationValue(String value) {
        return switch (trimToEmpty(value).toLowerCase(Locale.ROOT)) {
            case "multiply_total", "multiply" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.MULTIPLY_BASE;
        };
    }

    private static final class ConfigData {
        private int initial_wrath = 0;
        private List<String> allowed_curios = new ArrayList<>();
        private List<String> allowed_weapons = defaultAllowedWeapons();
        private List<String> allowed_armor = defaultAllowedArmor();
        private List<String> growth_targets = new ArrayList<>();
        private AttributeBonus attribute_bonus = AttributeBonus.defaults();

        private transient ItemMatcher allowedCurios = ItemMatcher.empty();
        private transient ItemMatcher allowedWeapons = ItemMatcher.empty();
        private transient ItemMatcher allowedArmor = ItemMatcher.empty();
        private transient GrowthMatcher growthTargets = GrowthMatcher.empty();

        private static ConfigData defaults() {
            return GSON.fromJson(DEFAULT_JSON, ConfigData.class);
        }

        private void prepare() {
            initial_wrath = Math.max(0, initial_wrath);
            allowed_curios = allowed_curios == null ? new ArrayList<>() : allowed_curios;
            allowed_weapons = allowed_weapons == null ? new ArrayList<>() : allowed_weapons;
            allowed_armor = allowed_armor == null ? new ArrayList<>() : allowed_armor;
            growth_targets = growth_targets == null ? new ArrayList<>() : growth_targets;
            attribute_bonus = attribute_bonus == null ? AttributeBonus.defaults() : attribute_bonus;

            allowedCurios = ItemMatcher.parse(allowed_curios);
            allowedWeapons = ItemMatcher.parse(allowed_weapons);
            allowedArmor = ItemMatcher.parse(allowed_armor);
            growthTargets = GrowthMatcher.parse(growth_targets);
            attribute_bonus.prepare();
        }

        private static ArrayList<String> defaultAllowedWeapons() {
            return new ArrayList<>(List.of("minecraft:wooden_sword"));
        }

        private static ArrayList<String> defaultAllowedArmor() {
            return new ArrayList<>(List.of(
                    "minecraft:leather_helmet",
                    "minecraft:leather_chestplate",
                    "minecraft:leather_leggings",
                    "minecraft:leather_boots"));
        }
    }

    public static final class AttributeBonus {
        private String mode = "all";
        private double base_percent = 1000.0D;
        private double per_wrath_percent = 500.0D;
        private double max_percent = -1.0D;
        private String operation = "multiply_base";
        private List<String> blacklist = new ArrayList<>();
        private List<AttributeEntry> custom = new ArrayList<>();

        private transient Mode parsedMode = Mode.ALL;
        private transient AttributeScale allScale = AttributeScale.defaults();
        private transient List<ConfiguredAttribute> attributes = List.of();

        private static AttributeBonus defaults() {
            AttributeBonus defaults = new AttributeBonus();
            defaults.blacklist.add("forge:entity_gravity");
            defaults.blacklist.add("minecraft:generic.movement_speed");
            defaults.custom.add(new AttributeEntry("minecraft:generic.armor", "护甲"));
            defaults.custom.add(new AttributeEntry("minecraft:generic.armor_toughness", "盔甲韧性"));
            defaults.custom.add(new AttributeEntry("minecraft:generic.max_health", "最大生命值"));
            defaults.custom.add(new AttributeEntry("minecraft:generic.knockback_resistance", "击退抗性"));
            return defaults;
        }

        private void prepare() {
            parsedMode = Mode.parse(mode);
            blacklist = blacklist == null ? new ArrayList<>() : blacklist;
            custom = custom == null ? new ArrayList<>() : custom;

            allScale = new AttributeScale(
                    Math.max(0.0D, sanitize(base_percent)),
                    Math.max(0.0D, sanitize(per_wrath_percent)),
                    sanitize(max_percent),
                    operationValue(operation));

            ImmutableList.Builder<ConfiguredAttribute> builder = ImmutableList.builder();
            if (parsedMode == Mode.ALL) {
                Set<ResourceLocation> blacklistedIds = parseBlacklist(blacklist);
                for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
                    ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
                    if (id == null || blacklistedIds.contains(id)) {
                        continue;
                    }
                    builder.add(ConfiguredAttribute.of(
                            id,
                            attribute,
                            Component.translatable(ALL_ATTRIBUTES_LABEL_KEY),
                            allScale,
                            UUID.nameUUIDFromBytes(("goose_curios:lovins_wrath:all:" + id + ":" + allScale.operation().name().toLowerCase(Locale.ROOT))
                                    .getBytes(StandardCharsets.UTF_8))));
                }
            } else {
                for (AttributeEntry entry : custom) {
                    ConfiguredAttribute configured = entry == null ? null : entry.resolve();
                    if (configured != null) {
                        builder.add(configured);
                    }
                }
            }
            attributes = builder.build();
        }

        private static Set<ResourceLocation> parseBlacklist(List<String> entries) {
            ImmutableSet.Builder<ResourceLocation> builder = ImmutableSet.builder();
            for (String entry : entries) {
                ResourceLocation id = parseId(entry);
                if (id != null) {
                    builder.add(id);
                }
            }
            return builder.build();
        }

        public Mode mode() {
            return parsedMode;
        }

        public AttributeScale allScale() {
            return allScale;
        }

        public List<ConfiguredAttribute> attributes() {
            return attributes;
        }
    }

    public enum Mode {
        ALL,
        CUSTOM;

        private static Mode parse(String value) {
            return "custom".equals(trimToEmpty(value).toLowerCase(Locale.ROOT)) ? CUSTOM : ALL;
        }
    }

    public record AttributeScale(double basePercent,
                                 double perWrathPercent,
                                 double maxPercent,
                                 AttributeModifier.Operation operation) {
        private static AttributeScale defaults() {
            return new AttributeScale(1000.0D, 500.0D, -1.0D, AttributeModifier.Operation.MULTIPLY_BASE);
        }

        public boolean hasLimit() {
            return maxPercent >= 0.0D;
        }

        public double limit() {
            return Math.max(basePercent, maxPercent);
        }

        public double value(int wrath) {
            double value = basePercent + Math.max(0, wrath) * perWrathPercent;
            return hasLimit() ? Math.min(value, limit()) : value;
        }

        public String format(double value) {
            return String.format(Locale.ROOT, "%.2f%%", value);
        }

        public String formatLimit() {
            return hasLimit() ? format(limit()) : "∞";
        }
    }

    private static final class AttributeEntry {
        private String id = "";
        private String label = "";
        private double base_percent = 1000.0D;
        private double per_wrath_percent = 500.0D;
        private double max_percent = -1.0D;
        private String operation = "multiply_base";

        private AttributeEntry() {
        }

        private AttributeEntry(String id, String label) {
            this.id = id;
            this.label = label;
        }

        private ConfiguredAttribute resolve() {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(trimToEmpty(id));
            if (resourceLocation == null) {
                return null;
            }

            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(resourceLocation);
            if (attribute == null) {
                return null;
            }

            AttributeScale scale = new AttributeScale(
                    Math.max(0.0D, sanitize(base_percent)),
                    Math.max(0.0D, sanitize(per_wrath_percent)),
                    sanitize(max_percent),
                    operationValue(operation));
            return ConfiguredAttribute.of(
                    resourceLocation,
                    attribute,
                    labelComponent(attribute),
                    scale,
                    UUID.nameUUIDFromBytes(("goose_curios:lovins_wrath:custom:" + resourceLocation + ":" + scale.operation().name().toLowerCase(Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8)));
        }

        private Component labelComponent(Attribute attribute) {
            String value = trimToEmpty(label);
            return value.isEmpty() ? Component.translatable(attribute.getDescriptionId()) : Component.literal(value);
        }
    }

    public record ConfiguredAttribute(ResourceLocation id,
                                      Attribute attribute,
                                      Component label,
                                      AttributeScale scale,
                                      UUID uuid) {
        private static ConfiguredAttribute of(ResourceLocation id, Attribute attribute, Component label, AttributeScale scale, UUID uuid) {
            return new ConfiguredAttribute(id, attribute, label, scale, uuid);
        }

        public double displayValue(int wrath) {
            return scale.value(wrath);
        }

        public AttributeModifier modifier(int wrath) {
            return new AttributeModifier(uuid, "Lovin " + id, displayValue(wrath) / 100.0D, scale.operation());
        }

        public String format(double value) {
            return scale.format(value);
        }

        public String formatLimit() {
            return scale.formatLimit();
        }
    }

    private record ItemMatcher(Set<ResourceLocation> ids, Set<TagKey<Item>> tags) {
        private static ItemMatcher empty() {
            return new ItemMatcher(Set.of(), Set.of());
        }

        private static ItemMatcher parse(List<String> entries) {
            Set<ResourceLocation> ids = new HashSet<>();
            Set<TagKey<Item>> tags = new HashSet<>();
            for (String entry : entries) {
                ResourceLocation id = parseId(entry);
                if (id == null) {
                    continue;
                }
                if (isTagEntry(entry)) {
                    tags.add(TagKey.create(Registries.ITEM, id));
                } else {
                    ids.add(id);
                }
            }
            return new ItemMatcher(Set.copyOf(ids), Set.copyOf(tags));
        }

        private boolean matches(ItemStack stack) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ids.contains(id)) {
                return true;
            }
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record GrowthMatcher(Map<ResourceLocation, Integer> ids, Map<TagKey<EntityType<?>>, Integer> tags) {
        private static GrowthMatcher empty() {
            return new GrowthMatcher(Map.of(), Map.of());
        }

        private static GrowthMatcher parse(List<String> entries) {
            java.util.HashMap<ResourceLocation, Integer> ids = new java.util.HashMap<>();
            java.util.HashMap<TagKey<EntityType<?>>, Integer> tags = new java.util.HashMap<>();
            for (String entry : entries) {
                GrowthTarget target = GrowthTarget.parse(entry);
                if (target == null) {
                    continue;
                }
                if (target.tag()) {
                    tags.put(TagKey.create(Registries.ENTITY_TYPE, target.id()), target.points());
                } else {
                    ids.put(target.id(), target.points());
                }
            }
            return new GrowthMatcher(Map.copyOf(ids), Map.copyOf(tags));
        }

        private boolean matches(EntityType<?> entityType) {
            return points(entityType) > 0;
        }

        private int points(EntityType<?> entityType) {
            ResourceLocation id = EntityType.getKey(entityType);
            Integer exact = ids.get(id);
            if (exact != null) {
                return exact;
            }
            for (Map.Entry<TagKey<EntityType<?>>, Integer> entry : tags.entrySet()) {
                if (entityType.is(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return 0;
        }
    }

    private record GrowthTarget(ResourceLocation id, boolean tag, int points) {
        private static GrowthTarget parse(String entry) {
            String value = trimToEmpty(entry);
            if (value.isEmpty()) {
                return null;
            }

            int split = value.indexOf('=');
            String idPart = split >= 0 ? value.substring(0, split).trim() : value;
            int points = DEFAULT_TARGET_POINTS;
            if (split >= 0 && split < value.length() - 1) {
                try {
                    points = Math.max(0, Integer.parseInt(value.substring(split + 1).trim()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            ResourceLocation id = parseId(idPart);
            return id == null ? null : new GrowthTarget(id, isTagEntry(idPart), points);
        }
    }
}
