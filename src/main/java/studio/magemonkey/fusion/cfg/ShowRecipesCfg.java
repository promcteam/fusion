package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ShowRecipesCfg {

    private static FileConfiguration config;
    private static File file;


    @Getter
    @Setter
    private static String name = ChatColor.DARK_AQUA + "Browse";
    @Getter
    @Setter
    private static ItemStack fillItem;
    @Getter
    @Setter
    private static InventoryPattern pattern;

    public static void load() {
        try {
            if (file == null || !file.exists()) {
                config = new YamlConfiguration();
                file = new File(Fusion.getInstance().getDataFolder(), "lang/ShowRecipes.yml");
                file.getParentFile().mkdirs();
                setDefaults();
                if (!file.exists()) {
                    file.createNewFile();
                    config.options().copyDefaults(true);
                    config.save(file);
                    Fusion.getInstance().log.info("Created default browse.yml");
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
            setDefaults();
            readData();
            Fusion.getInstance().log.info("Successfully loaded browse.yml data");
        } catch (IOException e) {
            Fusion.getInstance().log.severe("Could not load browse.yml data: " + e.getMessage());
        }
    }

    private static void setDefaults() {
        // Show Recipe: GUI Mechanic
        config.addDefault("name", "&8Recipes for ingredient: &a&7<ingredient>");
        HashMap<Character, ItemStack> showItems = new HashMap<>();
        showItems.put('0', ItemBuilder.newItem(Material.BIRCH_SIGN)
                .name("&8Possible Recipes")
                .insertLoreLine(0, ChatColor.GRAY + "Your ingredient can be used")
                .insertLoreLine(1, ChatColor.GRAY + "in the following recipes.")
                .build());
        showItems.put('1', ItemBuilder.newItem(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        showItems.put('<', ItemBuilder.newItem(Material.ARROW).name("&7Previous Page").build());
        showItems.put('>', ItemBuilder.newItem(Material.ARROW).name("&7Next Page").build());
        showItems.put('o', ItemBuilder.newItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build());
        InventoryPattern showPattern =
                new InventoryPattern(new String[]{"111101111", "ooooooooo", "ooooooooo", "ooooooooo", "111111111"}, showItems);
        config.addDefault("pattern", showPattern.serialize());

        HashMap<String, Object> recipeItem = new HashMap<>();
        recipeItem.put("material", "$<material>");
        recipeItem.put("name", "&7<name>");
        recipeItem.put("lore", new String[]{"&8[&a$<amount>x &7<ingredient>&8]", "&7Click to navigate to recipe"});
        config.addDefault("recipeItem", recipeItem);
        config.addDefault("fillItem", ItemBuilder.newItem(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

    }

    private static void readData() {
        name = config.getString("name");
        fillItem = config.getItemStack("fillItem", ItemBuilder.newItem(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        pattern = new InventoryPattern(config.getConfigurationSection("pattern").getValues(false));
    }

    public static ItemStack getRecipeIcon(Recipe recipe, RecipeItem ingredient) {
        Material material = Material.valueOf(config.getString("recipeItem.material", "$<material>").replace(MessageUtil.getReplacement("material"), recipe.getResults().getResultItem().getItemStack().getType().name()).toUpperCase());
        String itemName = Utils.getItemName(recipe.getResults().getResultItem().getItemStack());
        String name = ChatUT.hexString(config.getString("recipeItem.name", "&7$<name>").replace(MessageUtil.getReplacement("name"), itemName));
        List<String> lore = config.getStringList("recipeItem.lore");
        lore.replaceAll(s -> ChatUT.hexString(s.replace(MessageUtil.getReplacement("ingredient"), Utils.getItemName(ingredient.getItemStack()))
                .replace(MessageUtil.getReplacement("profession"), recipe.getTable().getInventoryName())
                .replace(MessageUtil.getReplacement("amount"), String.valueOf(ingredient.getAmount()))
                .replace(MessageUtil.getReplacement("name"), name)));
        return ItemBuilder.newItem(material).name(name).lore(lore).build();
    }
}
