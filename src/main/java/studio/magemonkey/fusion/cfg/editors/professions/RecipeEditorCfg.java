package studio.magemonkey.fusion.cfg.editors.professions;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeEditorCfg {

    private final YamlParser config;

    public RecipeEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/professions/RecipeEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Recipes"));
    }

    public Map<String, ItemStack> getIcons(CraftingTable table) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if (icon.equalsIgnoreCase("recipeItem")) continue;
            icons.put(icon, getIcon(table, icon));
        }
        return icons;
    }

    public ItemStack getIcon(CraftingTable table, String icon) {
        Material material =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), table.getInventoryName())
                    .replace(MessageUtil.getReplacement("profession"), table.getName())
                    .replace(MessageUtil.getReplacement("masteryUnlock"),
                            String.valueOf(table.getMasteryUnlock()))
                    .replace(MessageUtil.getReplacement("masteryCost"),
                            String.valueOf(table.getMasteryFee()))
                    .replace(MessageUtil.getReplacement("useCategories"),
                            String.valueOf(table.getUseCategories()))));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public ItemStack getRecipeIcon(Recipe recipe) {
        ItemStack result = recipe.getResults().getResultItem().getItemStack();
        String name = config.getString("icons.recipeItem.name", "&9$<recipe.name>")
                .replace(MessageUtil.getReplacement("recipe.name"), recipe.getName());
        List<String> lore = config.getStringList("icons.recipeItem.lore");

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("recipe.lore"))) {
                lore.remove(i);
                if (result == null || result.getItemMeta() == null || result.getItemMeta().getLore() == null) continue;
                int newLines = 1;
                for (String line : result.getItemMeta().getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                            .replace(MessageUtil.getReplacement("recipe.result"),
                                    (result != null && !result.getType().isAir()) && result.hasItemMeta() ? result.getItemMeta()
                                            .getDisplayName() : result.getType().name()))
                    .replace(MessageUtil.getReplacement("conditions.rank"),
                            recipe.getConditions().getPermission() == null ? getUnsetFormat()
                                    : recipe.getConditions().getPermission())
            );
        }

        return ItemBuilder.newItem(result)
                .name(name)
                .lore(lore)
                .build();
    }

    /* SubEditor */
    public String getSubTitle(String recipeName) {
        String title = config.getString("subEditor.title", "&2Recipe Item &a$<recipe.name>");
        title = title.replace(MessageUtil.getReplacement("recipe.name"), recipeName);
        return ChatUT.hexString(title);
    }

    public Map<String, ItemStack> getSubIcons(Recipe recipe) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("subEditor.icons").getKeys(false)) {
            icons.put(icon, getSubIcon(recipe, icon));
        }
        return icons;
    }

    public ItemStack getSubIcon(Recipe recipe, String icon) {
        ItemStack result     = recipe.getResults().getResultItem().getItemStack();
        String    resultName = recipe.getResults().getResultName();
        Material material =
                Material.valueOf(config.getString("subEditor.icons." + icon + ".material", "$<material>")
                        .replace(MessageUtil.getReplacement("material"),
                                recipe.getResults().getResultItem().getItemStack().getType().name())
                        .toUpperCase());
        int          amount      = config.getInt("subEditor.icons." + icon + ".amount", 1);
        int          durability  = config.getInt("subEditor.icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("subEditor.icon." + icon + ".unbreakable", false);
        String       name        = config.getString("subEditor.icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("subEditor.icons." + icon + ".lore");

        ProfessionConditions conditions = recipe.getConditions();

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("result.lore"))) {
                lore.remove(i);
                if (result.getItemMeta() == null || result.getItemMeta().getLore() == null) continue;
                int newLines = 1;
                for (String line : result.getItemMeta().getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("commands"))) {
                lore.remove(i);
                int newLines = 1;
                for (DelayedCommand line : recipe.getResults().getCommands()) {
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.commands.commandPrefix", "&7- &a$<command>")
                                    .replace(MessageUtil.getReplacement("command"),
                                            line.getCmd())
                                    .replace(MessageUtil.getReplacement("delay"),
                                            String.valueOf(line.getDelay()))
                                    .replace(MessageUtil.getReplacement("caster"),
                                            line.getAs().name())
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("ingredients"))) {
                lore.remove(i);
                int newLines = 1;
                for (RecipeItem item : recipe.getConditions().getRequiredItems()) {
                    ItemStack patternItem = item.getItemStack();
                    String itemName = patternItem.hasItemMeta() ? patternItem.getItemMeta().getDisplayName()
                            : patternItem.getType().name();
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.ingredients.ingredientPrefix",
                                            "&7- &2$<ingredient.amount>x &a$<ingredient.name>")
                                    .replace(MessageUtil.getReplacement("ingredient.name"),
                                            itemName)
                                    .replace(MessageUtil.getReplacement("ingredient.amount"),
                                            String.valueOf(item.getItemStack().getAmount())));
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("conditions"))) {
                // TODO Full Translation of Conditions
                lore.remove(i);
                int newLines = 1;
                for (Map.Entry<String, Map<String, Integer>> entry : conditions.getFullConditions().entrySet()) {
                    for (Map.Entry<String, Integer> condition : entry.getValue().entrySet()) {
                        lore.add(i - 1 + newLines,
                                config.getString("subEditor.icons.conditions.conditionPrefix", "&7- &a$<condition>")
                                        .replace(MessageUtil.getReplacement("condition"),
                                                getConditionFormat(entry.getKey(),
                                                        condition.getKey(),
                                                        condition.getValue())));
                        newLines++;
                    }
                }
                i += newLines;
                continue;
            }

            lore.set(i, ChatUT.hexString(lore.get(i)
                            .replace(MessageUtil.getReplacement("name"), recipe.getName())
                            .replace(MessageUtil.getReplacement("results.resultItem"), resultName)
                            .replace(MessageUtil.getReplacement("results.professionExp"),
                                    String.valueOf(recipe.getResults().getProfessionExp()))
                            .replace(MessageUtil.getReplacement("results.vanillaExp"),
                                    String.valueOf(recipe.getResults().getVanillaExp()))
                            .replace(MessageUtil.getReplacement("costs.money"),
                                    String.valueOf(conditions.getMoneyCost()))
                            .replace(MessageUtil.getReplacement("costs.experience"),
                                    String.valueOf(conditions.getExpCost()))
                            .replace(MessageUtil.getReplacement("craftingTime"),
                                    String.valueOf(recipe.getCraftingTime()))
                            .replace(MessageUtil.getReplacement("craftingLimit"),
                                    String.valueOf(recipe.getCraftingLimit()))
                            .replace(MessageUtil.getReplacement("craftingLimitCooldown"),
                                    String.valueOf(recipe.getCraftingLimitCooldown()))
                            .replace(MessageUtil.getReplacement("conditions.professionLevel"),
                                    String.valueOf(recipe.getConditions().getProfessionLevel()))
                            .replace(MessageUtil.getReplacement("conditions.mastery"),
                                    String.valueOf(recipe.getConditions().isMastery()))
                            .replace(MessageUtil.getReplacement("conditions.rank"),
                                    String.valueOf(recipe.getConditions().getPermission()))
                            .replace(MessageUtil.getReplacement("category"),
                                    recipe.getCategory() == null ? "master" : recipe.getCategory()))
                    .replace(MessageUtil.getReplacement("hidings.noPermission"),
                            recipe.getSettings().getHideNoPermission() == null ? getUnsetFormat()
                                    : String.valueOf(recipe.getSettings().getHideNoPermission().booleanValue()))
                    .replace(MessageUtil.getReplacement("hidings.recipeLimitReached"),
                            recipe.getSettings().getHideRecipeLimitReached() == null ? getUnsetFormat()
                                    : String.valueOf(recipe.getSettings().getHideRecipeLimitReached().booleanValue()))
            );
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("subEditor.icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("subEditor.icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(icon.equals("resultItem") ? recipe.getResults().getResultItem().getAmount() : amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    private String getConditionFormat(String condition, String key, int value) {
        return config.getString("subEditor.icons.conditions.conditionFormatting." + condition, key + ": " + value)
                .replace(MessageUtil.getReplacement("condition.name"), key)
                .replace(MessageUtil.getReplacement("condition.amount"), String.valueOf(value));
    }

    private String getUnsetFormat() {
        return config.getString("unset", "&anot set");
    }
}
