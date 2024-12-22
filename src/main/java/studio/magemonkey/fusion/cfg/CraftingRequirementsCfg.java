package studio.magemonkey.fusion.cfg;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.*;

public class CraftingRequirementsCfg {

    private static YamlParser config;

    public static void init() {
        config = YamlParser.loadOrExtract(Fusion.getInstance(), "lang/CraftingRequirements.yml");
    }

    private static String getFulfilled() {
        return ChatUT.hexString(config.getString("fulfilled", "&a✓"));
    }

    private static String getUnfulfilled() {
        return ChatUT.hexString(config.getString("unfulfilled", "&c✗"));
    }

    /* Recipe-related crafting requirements */
    public static String getCraftingRequirementLine(String path) {
        return ChatUT.hexString(config.getString(path + ".requirementLine", "&7Crafting Requirements"));
    }

    public static String getBossBarTitle(ItemStack item) {
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName()
                : ChatUT.serialize(Component.translatable(item.getTranslationKey()));
        return ChatUT.hexString(config.getString("recipes.bossbar", "&5Crafting $<item>...")
                .replace(MessageUtil.getReplacement("item"), itemName));
    }

    public static String getLearned(String path, boolean fulfilled) {
        return ChatUT.hexString(config.getString(path + ".learned." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aLearned" : "&cNot Learned")));
    }

    public static String getMoney(String path, double money, double required) {
        boolean fulfilled = money >= required;
        String line = config.getString(path + ".money." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eMoney: &7(&a$$<money>&8/&7$$<required>&7)"
                        : "&6- &eMoney: &7(&c$$<money>&8/&7$$<required>&7)");
        line = line.replace("$<money>", String.valueOf(money)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getExp(String path, int experience, int required) {
        boolean fulfilled = experience >= required;
        String line = config.getString(path + ".exp." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eExperience: &7(&a<exp>&8/&7<required>&7)"
                        : "&6- &eExperience: &7(&c<exp>&8/&7<required>&7)");
        line = line.replace("$<exp>", String.valueOf(experience)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getProfessionLevel(String path, int level, int required) {
        boolean fulfilled = level >= required;
        String line = config.getString(path + ".professionLevel." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eLevel Needed: &7(&a<level>&8/&7<required>&7)"
                        : "&6- &eLevel Needed: &7(&c<level>&8/&7<required>&7)");
        line = line.replace("$<level>", String.valueOf(level)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getMastery(String path, boolean mastery, boolean required) {
        boolean fulfilled = mastery == required;
        String line = config.getString(path + ".mastery." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eMastery: &7(&a<mastery>&8/&7<required>&7)"
                        : "&6- &eMastery: &7(&c<mastery>&8/&7<required>&7)");
        line = line.replace("$<mastery>", String.valueOf(mastery)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getRank(String path, String rank) {
        return ChatUT.hexString(config.getString(path + ".rank." + rank,
                "&c" + rank + " rank needed to craft this item!"));
    }

    public static String getConditionLine(String path, String condition, String key, int level, int required) {
        boolean fulfilled = level >= required;
        String line = config.getString(path + ".conditions." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- e$<condition>&8: &7(&a<level>&8/&7<required>&7)"
                        : "&6- &e$<condition>&8: &7(&c$<level>&8/&7$<condition.level>&7)");
        line = line.replace("$<level>", String.valueOf(level)).replace("$<required>", String.valueOf(required));
        line = line.replace("$<condition>", getConditionVariation(path, condition, key));
        return ChatUT.hexString(line);
    }

    private static String getConditionVariation(String path, String condition, String key) {
        return config.getString(path + ".conditions.variations." + condition, "$<name>").replace("$<name>", key);
    }

    public static String getIngredientLine(String path, RecipeItem item, int amount, int required) {
        boolean fulfilled = amount >= required;
        String line = config.getString(path + ".ingredients." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &e$<item>&8: &7(&a<amount>&8/&7<required>&7)"
                        : "&6- &e$<item>&8: &7(&c<amount>&8/&7<required>&7)");
        line = line.replace("$<amount>", String.valueOf(amount)).replace("$<required>", String.valueOf(required));


        ItemStack _item    = item.getItemStack();
        String    itemName = VersionManager.getCompat().getItemName(_item);

        if (_item.hasItemMeta()) {
            line = getCustomHighlight(path) + line;
        }
        line = line.replace("$<item>", itemName);
        return ChatUT.hexString(line);
    }

    private static String getCustomHighlight(String path) {
        return config.getString(path + ".ingredients.highlightCustomItem", "");
    }

    public static boolean hasExtensionEnabled(String path) {
        return config.getBoolean(path + ".ingredients.extensions.enabled", true);
    }

    public static boolean hasOnlyVanillaExtension(String path) {
        return config.getBoolean(path + ".ingredients.extensions.onlyVanilla", false);
    }

    public static List<String> getExtensionLoreLine(String path, List<String> provided, List<String> required) {
        List<String> entries = new ArrayList<>();
        if (!config.getBoolean(path + ".ingredients.extensions.lore.enabled")) return entries;
        if (required.isEmpty()) return entries;
        for (String lore : required) {
            boolean fulfilled = provided.contains(lore);
            String line =
                    config.getString(path + ".ingredients.extensions.lore." + (fulfilled ? "true" : "false"),
                            "  $<lore>");
            line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
            line = line.replace("$<lore>", ChatUT.serialize(Component.text(lore)));
            entries.add(ChatUT.hexString(line));
        }
        for (String lore : provided) {
            if (!required.contains(lore)) {
                String line = config.getString(path + ".ingredients.extensions.lore.false",
                        "  $<lore> &7($<unfulfilled>&7)");
                line = line.replace("$<unfulfilled>", getUnfulfilled());
                line = line.replace("$<lore>", ChatUT.serialize(Component.text(lore)));
                entries.add(ChatUT.hexString(line));
            }
        }
        return entries;
    }

    public static List<String> getExtensionEnchantmentLine(String path,
                                                           Map<Enchantment, Integer> provided,
                                                           Map<Enchantment, Integer> required) {
        List<String> entries = new ArrayList<>();
        if (!config.getBoolean(path + ".ingredients.extensions.enchantments.enabled")) return entries;
        if (required.isEmpty()) return entries;
        for (Map.Entry<Enchantment, Integer> entry : required.entrySet()) {
            boolean fulfilled = provided.containsKey(entry.getKey()) && Objects.equals(provided.get(entry.getKey()),
                    entry.getValue());
            String line =
                    config.getString(path + ".ingredients.extensions.enchantments." + (fulfilled ? "true" : "false"),
                            fulfilled ? "  &8• &7Enchant &9$<enchantment> &7(&a$<required>&7) &7($<fulfilled>&7)"
                                    : "  &8• &7Enchant &9$<enchantment> &7(&a$<required>&7) &7($<unfulfilled>&7)");
            line = line.replace("$<required>", String.valueOf(entry.getValue()));
            line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
            line = line.replace("$<enchantment>",
                    ChatUT.serialize(Component.translatable(entry.getKey().getTranslationKey())));
            entries.add(ChatUT.hexString(line));
        }
        for (Map.Entry<Enchantment, Integer> entry : provided.entrySet()) {
            if (!required.containsKey(entry.getKey())) {
                String line = config.getString(path + ".ingredients.extensions.enchantments.false",
                        "  &8• &7Enchant &9$<enchantment> &7(&a$<required>&7) &7($<unfulfilled>&7)");
                line = line.replace("$<required>", "0");
                line = line.replace("$<unfulfilled>", getUnfulfilled());
                line = line.replace("$<enchantment>",
                        ChatUT.serialize(Component.translatable(entry.getKey().getTranslationKey())));
                entries.add(ChatUT.hexString(line));
            }
        }
        return entries;
    }

    public static List<String> getExtensionFlagLine(String path, Set<ItemFlag> provided, Set<ItemFlag> required) {
        List<String> entries = new ArrayList<>();
        if (!config.getBoolean(path + ".ingredients.extensions.flags.enabled")) return entries;
        if (required.isEmpty()) return entries;
        for (ItemFlag flag : required) {
            boolean fulfilled = provided.contains(flag);
            String line = config.getString(path + ".ingredients.extensions.flags." + (fulfilled ? "true" : "false"),
                    fulfilled ? "  &8• &7Flag &9$<flag> &7(&a$<required>&7) &7($<fulfilled>&7)"
                            : "  &8• &7Flag &9$<flag> &7(&a$<required>&7) &7($<unfulfilled>&7)");
            line = line.replace("$<required>", getFulfilled());
            line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
            line = line.replace("$<flag>", flag.toString().toLowerCase());
            entries.add(ChatUT.hexString(line));
        }
        return entries;
    }

    public static String getExtensionUnbreakableLine(String path, boolean provided, boolean required) {
        if (!config.getBoolean(path + ".ingredients.extensions.unbreakable.enabled")) return null;
        boolean fulfilled = provided == required;
        String line = config.getString(path + ".ingredients.extensions.unbreakable." + (fulfilled ? "true" : "false"),
                fulfilled ? "  &8• &7Unbreakable &7(&a$<required>&7) &7($<fulfilled>&7)"
                        : "  &8• &7Unbreakable &7(&a$<required>&7) &7($<unfulfilled>&7)");
        line = line.replace("$<required>", String.valueOf(required));
        line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
        return ChatUT.hexString(line);
    }

    public static String getExtensionDurabilityLine(String path, int provided, int required) {
        if (!config.getBoolean(path + ".ingredients.extensions.durability.enabled")) return null;
        boolean fulfilled = provided == required;
        String line = config.getString(path + ".ingredients.extensions.durability." + (fulfilled ? "true" : "false"),
                fulfilled ? "  &8• &7Durability &7(&a$<required>&7) &7($<fulfilled>&7)"
                        : "  &8• &7Durability &7(&a$<required>&7) &7($<unfulfilled>&7)");
        line = line.replace("$<required>", String.valueOf(required));
        line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
        return ChatUT.hexString(line);
    }

    public static String getExtensionCustomModelDataLine(String path, int provided, int required) {
        if (!config.getBoolean(path + ".ingredients.extensions.customModelData.enabled")) return null;
        boolean fulfilled = provided == required;
        String line =
                config.getString(path + ".ingredients.extensions.customModelData." + (fulfilled ? "true" : "false"),
                        fulfilled ? "  &8• &7Custom Model &7(&a$<required>&7) &7($<fulfilled>&7)"
                                : "  &8• &7Custom Model &7(&a$<required>&7) &7($<unfulfilled>&7)");
        line = line.replace("$<required>", String.valueOf(required));
        line = line.replace("$<fulfilled>", getFulfilled()).replace("$<unfulfilled>", getUnfulfilled());
        return ChatUT.hexString(line);
    }


    /* Recipe-related crafting requirements */
    public static String getCanCraft(boolean fulfilled) {
        return ChatUT.hexString(config.getString("recipes.canCraft." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aYou can craft this item." : "&cYou can't craft this item.")));
    }

    public static String getLimit(String path, int limit, int maxLimit) {
        boolean fulfilled = limit < maxLimit;
        String line = config.getString(path + ".limit." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eLimit: &7(&a<limit>&8/&7<required>&7)"
                        : "&6- &eLimit: &7(&c<limit>&8/&7<required>&7)");
        line = line.replace("$<limit>", String.valueOf(limit)).replace("$<required>", String.valueOf(maxLimit));
        return ChatUT.hexString(line);
    }

    /* Profession-related crafting requirements */
    public static String getCanJoin(boolean fulfilled) {
        return ChatUT.hexString(config.getString("professions.canJoin." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aYou can join this profession." : "&cYou can't join this profession.")));
    }
}
