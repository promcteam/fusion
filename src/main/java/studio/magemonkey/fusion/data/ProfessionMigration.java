package studio.magemonkey.fusion.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.items.PrefixHelper;
import studio.magemonkey.fusion.Fusion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ProfessionMigration {
    // Update this when there are new migrations available
    private static final String                                         VERSION    = "1.1";
    private static final Map<String, Function<FileConfiguration, Void>> migrations = new HashMap<>();

    private static boolean compareVersions(String version, String compareTo) {
        int aMajor = Integer.parseInt(version.split("\\.")[0]);
        int aMinor = Integer.parseInt(version.split("\\.")[1]);

        int bMajor = Integer.parseInt(compareTo.split("\\.")[0]);
        int bMinor = Integer.parseInt(compareTo.split("\\.")[1]);

        return aMajor < bMajor || (aMajor == bMajor && aMinor < bMinor);
    }

    public static void migrate(FileConfiguration config) {
        String version = config.getString("version", "1.0");
        if (compareVersions(version, VERSION)) {
            Fusion.getInstance()
                    .getLogger()
                    .info("Migrating profession data to version " + VERSION + " for file " + config.getCurrentPath());
            for (Map.Entry<String, Function<FileConfiguration, Void>> entry : migrations.entrySet()) {
                if (compareVersions(version, entry.getKey())) {
                    entry.getValue().apply(config);
                }
            }
            config.set("version", VERSION);
        }
    }

    static {
        migrations.put("1.1", (config) -> {
            // Items that need to be changed are in the `recipes` array of items.
            // icon, recipes[x].costs.items[y], recipes[x].results.item, and categories[x].icon
            // These items are strings that start with `DIVINITY_` and need to be converted to the new format.
            // The new format is `DIVINITY_<module>:<id>[~level:<level>][~material:<material>][:<amount>]`
            // the old format is `DIVINITY_<module>_<id>:<level>` or `DIVINITY_<module>_<id>:<level>:<amount>:<material>`

            String icon = config.getString("icon");
            if (icon != null && icon.startsWith("DIVINITY_")) {
                String newItem = convertLegacyDivinityFormat(icon);
                config.set("icon", newItem);
            }

            List<Map<?, ?>> recipes = config.getMapList("recipes");
            for (Map<?, ?> recipe : recipes) {
                Map<String, Object> costs = (Map<String, Object>) recipe.get("costs");
                if (costs == null) continue;

                List<Object> itemCosts = (List<Object>) costs.get("items");
                if (itemCosts == null) continue;

                for (Object itemCost : itemCosts) {
                    if (itemCost instanceof String oldItem) {
                        if (oldItem.startsWith("DIVINITY_")) {
                            String newItem = convertLegacyDivinityFormat(oldItem);
                            itemCosts.set(itemCosts.indexOf(oldItem), newItem);
                        }
                    }
                }

                Map<String, Object> result     = (Map<String, Object>) recipe.get("results");
                Object              resultData = result.get("item");
                if (resultData instanceof String oldItem) {
                    if (oldItem.startsWith("DIVINITY_")) {
                        String newItem = convertLegacyDivinityFormat(oldItem);
                        result.put("item", newItem);
                    }
                }
            }

            config.set("recipes", recipes);

            List<Map<?, ?>> categories = config.getMapList("categories");
            for (Map<?, ?> category : categories) {
                Map<String, String> cat      = (Map<String, String>) category;
                String              iconName = cat.get("icon");
                if (iconName != null && iconName.startsWith("DIVINITY_")) {
                    String newItem = convertLegacyDivinityFormat(iconName);
                    cat.put("icon", newItem);
                }
            }

            config.set("categories", categories);

            return null;
        });
    }

    static String convertLegacyDivinityFormat(String oldItem) {
        oldItem = PrefixHelper.stripPrefix("DIVINITY", oldItem);
        String[] parts         = oldItem.split(":");
        String[] divinityParts = parts[0].split("_");

        String module = null;
        String id;
        if (divinityParts.length >= 2) {
            module = mapModule(divinityParts[0]);
            id = divinityParts[1];
        } else {
            id = divinityParts[0];
        }

        String level    = null;
        String amount   = null;
        String material = null;

        int index = 1;

        if (module != null && !module.equals("custom_items") && parts.length > index) {
            level = parts[index++];
        }

        if (parts.length > index) {
            amount = parts[index++];
        }

        if (parts.length > index) {
            material = parts[index++];
        }

        StringBuilder builder = new StringBuilder("DIVINITY_");

        if (module != null) {
            builder.append(module).append(":");
        }

        builder.append(id);

        if (level != null) {
            builder.append("~level:").append(level);
        }

        if (material != null) {
            builder.append("~material:").append(material);
        }

        if (amount != null) {
            builder.append(":").append(amount);
        }

        return builder.toString();
    }

    private static String mapModule(@Nullable String module) {
        if (module == null) return null;

        return switch (module.toLowerCase()) {
            case "arrows", "ar" -> "arrows";
            case "consumables", "co" -> "consumables";
            case "customitems", "ci" -> "custom_items";
            case "dismantle", "di" -> "dismantle";
            case "essences", "es" -> "essences";
            case "extractor", "ex" -> "extractor";
            case "fortify", "fo" -> "fortify";
            case "gems", "ge" -> "gems";
            case "identify", "id" -> "identify";
            case "itemgenerator", "itemgen", "it" -> "item_generator";
            case "magicdust", "md" -> "magic_dust";
            case "runes", "ru" -> "runes";
            default -> null;
        };
    }
}
