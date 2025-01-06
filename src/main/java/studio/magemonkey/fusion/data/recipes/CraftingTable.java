package studio.magemonkey.fusion.data.recipes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityService;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Setter
public class CraftingTable implements ConfigurationSerializable {
    private String                          name;
    private String                          inventoryName;
    private InventoryPattern recipePattern;
    private InventoryPattern                catPattern;
    private ItemStack                       fillItem;
    private Map<String, Recipe>             recipes;
    private ItemType                        iconItem;
    private boolean                         useCategories = true;
    @Getter
    private LinkedHashMap<String, Category> categories    = new LinkedHashMap<>();

    //Mastery!
    private int masteryUnlock;
    private int masteryFee;

    private int maxLevel = -1;

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern recipePattern,
                         InventoryPattern catPattern,
                         boolean useCategories,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee,
                         Map<String, Recipe> recipes,
                         Map<String, Category> categories) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.recipePattern = recipePattern;
        this.catPattern = catPattern;
        this.useCategories = useCategories;
        this.recipes = recipes;
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
        this.categories = new LinkedHashMap<>(categories);
    }

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern recipePattern,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.recipePattern = recipePattern;
        this.catPattern = recipePattern;
        this.recipes = new LinkedHashMap<>(5);
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    @SuppressWarnings("unchecked")
    public CraftingTable(Map<String, Object> map) throws MissingProviderException, MissingItemException {
        long start = System.currentTimeMillis();


        this.recipes = new LinkedHashMap<>(5);
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.inventoryName = dw.getString("inventoryName");
        this.recipePattern = new InventoryPattern(dw.getSection("recipePattern"));
        this.catPattern =
                dw.getSection("categoryPattern") != null && dw.getSection("categoryPattern").containsKey("pattern")
                        ? new InventoryPattern(dw.getSection("categoryPattern"))
                        : recipePattern;
        this.masteryUnlock = dw.getInt("masteryUnlock");
        this.masteryFee = dw.getInt("masteryFee");
        this.maxLevel = dw.getInt("maxLevel", -1);
        this.useCategories = dw.getBoolean("useCategories", true);
        this.iconItem = CodexEngine.get()
                .getItemManager()
                .getItemType(dw.getString("icon"));
        if (dw.getSection("recipePattern.items.fillItem") != null)
            this.fillItem = new ItemBuilder(dw.getSection("recipePattern.items.fillItem")).build();
        else
//            this.fillItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            this.fillItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        Collection<Category> categoriesList = new ArrayList<>();
        dw.deserializeCollection(categoriesList, "categories", Category.class);
        categoriesList.stream()
                .filter(c -> c.getIconItem() != null)
                .forEach(c -> {
                    if (c.getPattern() == null)
                        c.setPattern(recipePattern);
                    categories.put(c.getName(), c);
                });

        List<Map<?, ?>> recipesSection = dw.getList("recipes", new ArrayList<>(2));
        long            longest        = 0;
        String          longestName    = null;
        for (Map<?, ?> recipeData : recipesSection) {
            long rStart = System.currentTimeMillis();
            try {
                Map<?, ?> results    = (Map<?, ?>) recipeData.get("results");
                String    itemResult = (String) results.get("item");
                if (itemResult.startsWith("DIVINITY_item_generator")) {
                    buildDivinityResultItem(recipeData, itemResult);
                    continue;
                }

                Recipe recipe = new Recipe(this, (Map<String, Object>) recipeData);
                this.recipes.put(recipe.getName(), recipe);

                if (recipeData.containsKey("category")
                        && recipeData.get("category") instanceof String categoryStr) {
                    Category category = categories.get(categoryStr);

                    if (category == null) {
                        continue;
                    }

                    category.getRecipes().add(recipe);
                }
            } catch (Exception e) {
                Fusion.getInstance()
                        .error("Exception when reading config, Invalid entry in config of " + this.name
                                + " crafting table. Value: " + recipeData);
                e.printStackTrace();
            } finally {
                long rEnd  = System.currentTimeMillis();
                long delta = rEnd - rStart;
                if (delta > longest) {
                    longest = delta;
                    longestName = (String) recipeData.get("name");
                }
            }
        }

        long end = System.currentTimeMillis();
        Fusion.getInstance()
                .getLogger()
                .info("Loaded " + this.recipes.size() + " recipes for " + this.name + " in " + (end - start) + "ms "
                        + "Longest recipe: " + longestName + " (" + longest + "ms)");
    }

    private static Pattern levelPattern    = Pattern.compile(".*~level:(\\d+).*");
    private static Pattern materialPattern = Pattern.compile(".*~material:(\\w+).*");

    private void buildDivinityResultItem(Map<?, ?> recipeData, String itemResult) {
        // Divinity format is: DIVINITY_<module>:<id>[~level:<level>][~material:<material>][:<amount>]
        int      level  = -1;
        int      amount = 1;
        ItemType type   = null;

        Matcher levelMatcher = levelPattern.matcher(itemResult);
        if (levelMatcher.matches()) {
            try {
                level = Integer.parseInt(levelMatcher.group(1));
            } catch (NumberFormatException ignored) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("Failed to get level for Divinity item " + itemResult + ". Using -1 instead.");
            }
            itemResult = itemResult.replace("~level:" + levelMatcher.group(1), "");
        }

        Matcher materialMatcher = materialPattern.matcher(itemResult);
        if (materialMatcher.matches()) {
            try {
                type = CodexEngine.get().getItemManager().getItemType(materialMatcher.group(1));
            } catch (MissingProviderException | MissingItemException ignored) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("Failed to get material item for Divinity item " + itemResult
                                + ". Using the item's configuration instead.");
            }
            itemResult = itemResult.replace("~material:" + materialMatcher.group(1), "");
        }

        // itemResult should be in the format of DIVINITY_<module>:<id>[:<amount>] at this point
        String[] itemArgs = itemResult.split(":", 3);
        if (itemArgs.length < 2) {
            Fusion.getInstance()
                    .error("Invalid entry in config of " + this.name
                            + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
            return;
        }

        String itemId = itemArgs[1];
        if (!DivinityService.isCached(itemId)) {
            if (!DivinityService.cache(itemId, null)) {
                Fusion.getInstance()
                        .error("Invalid entry in config of " + this.name
                                + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
                return;
            }
        }

        if (itemArgs.length >= 3) {
            amount = Integer.parseInt(itemArgs[2]);
        }

        ItemGenEntry entry = DivinityService.itemGenResults.get(itemId);
        if (entry == null) {
            Fusion.getInstance()
                    .error("Invalid entry in config of " + this.name
                            + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
            return;
        }
        Map<ItemType, Set<String>> names = entry.loadNames(type, level);

        Category category = null;
        if (recipeData.containsKey("category") && recipeData.get("category") instanceof String categoryStr) {
            category = categories.get(categoryStr);
        }

        String recipeName = (String) recipeData.get("name");
        int    i          = 0;
        for (Map.Entry<ItemType, Set<String>> nameEntry : names.entrySet()) {
            for (String name : nameEntry.getValue()) {
                DivinityRecipeMeta meta =
                        new DivinityRecipeMeta(recipeName, entry, level, amount, nameEntry.getKey(), name);
                Recipe recipe = new Recipe(this, (Map<String, Object>) recipeData, meta);
                recipe.setName(recipe.getName() + "::" + i);
                recipes.put(recipe.getName(), recipe);
                if (category != null) {
                    category.getRecipes().add(recipe);
                }
                i++;
                long end = System.currentTimeMillis();
            }
        }
    }

    public List<String> getCategoryList() {
        return new ArrayList<>(categories.keySet());
    }

    public Recipe getRecipe(String str) {
        return this.recipes.get(str);
    }

    public void addRecipe(Recipe recipe) {
        this.recipes.put(recipe.getName(), recipe);
    }

    public boolean getUseCategories() {
        return useCategories;
    }

    public Collection<Recipe> getRecipes(Collection<ItemStack> items, Player p) {
        if (items.isEmpty()) {
            return new ArrayList<>(this.recipes.values());
        }
        return this.recipes.values()
                .stream()
                .unordered()
                .filter(r -> r.isValid(items, p, this))
                .collect(Collectors.toList());
    }

    public Category getCategory(String name) {
        return this.categories.get(name);
    }

    public void updateCategoryOrder() {
        LinkedHashMap<String, Category> newCategories = new LinkedHashMap<>();
        // Compare the categories by their order and make a new map with the sorted categories
        this.categories.values().stream()
                .sorted(Comparator.comparingInt(Category::getOrder))
                .forEach(c -> newCategories.put(c.getName(), c));
        this.categories = newCategories;
    }

    public void moveEntry(Recipe recipe, int offset) {
        // Ensure the offset is either -1 (left) or 1 (right)
        if (offset != -1 && offset != 1) {
            throw new IllegalArgumentException("Offset must be -1 or 1");
        }

        List<Map.Entry<String, Recipe>> entries = new ArrayList<>(recipes.entrySet());
        int                             index   = -1;

        // Find the index of the current entry
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(recipe.getName())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return; // Key not found, do nothing
        }

        // Calculate the new index
        int newIndex = index + offset;

        // Check if the new index is within bounds
        if (newIndex < 0 || newIndex >= entries.size()) {
            return; // New index out of bounds, do nothing
        }

        // Remove and reinsert the entry at the new position
        Map.Entry<String, Recipe> entry = entries.remove(index);
        entries.add(newIndex, entry);

        // Clear the original map and reinsert the entries in the new order
        recipes.clear();
        for (Map.Entry<String, Recipe> e : entries) {
            recipes.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationBuilder.start(4)
                .append("name", this.name)
                .append("icon", this.iconItem.getNamespacedID())
                .append("recipePattern", this.recipePattern.serialize())
                .append("categoryPattern", this.catPattern != null ? this.catPattern.serialize() : null)
                .append("inventoryName", this.inventoryName)
                .append("masteryUnlock", this.masteryUnlock)
                .append("masteryFee", this.masteryFee)
                .append("maxLevel", this.maxLevel)
                .append("useCategories", useCategories)
                .append("recipes", this.recipes.values().stream().map(Recipe::serialize).collect(Collectors.toList()))
                .build();
    }

    public void save(Runnable runnable) {
        // Saving all changes to the file
        FileConfiguration   config = ProfessionsCfg.getCfgs().get(this.name);
        File                file   = ProfessionsCfg.getFiles().get(this.name);
        Map<String, Object> map    = this.serialize();

        Map<String, Object> recipePatterntemsMap    = (Map<String, Object>) map.get("recipePattern");
        Map<String, Object> catPatterntemsMap = (Map<String, Object>) map.get("categoryPattern");
        recipePatterntemsMap.remove("f");
        recipePatterntemsMap.remove("q");
        recipePatterntemsMap.remove("o");
        if (this.catPattern != null) {
            catPatterntemsMap.remove("f");
            catPatterntemsMap.remove("q");
            catPatterntemsMap.remove("o");
        }
        config.set("name", map.get("name"));
        config.set("inventoryName", map.get("inventoryName"));
        config.set("icon", map.get("icon"));
        config.set("recipePattern", recipePatterntemsMap);
        config.set("categoryPattern", catPatterntemsMap);
        config.set("masteryUnlock", map.get("masteryUnlock"));
        config.set("masteryFee", map.get("masteryFee"));
        config.set("maxLevel", map.get("maxLevel"));
        config.set("useCategories", map.get("useCategories"));
        config.set("recipes", map.get("recipes"));
        try {
            config.save(file);
            if (runnable != null)
                Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), runnable, 1L);
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
        }
    }

    // Static method to copy contents from one CraftingTable instance to another
    public static CraftingTable copy(CraftingTable source) {
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Recipe recipe : source.getRecipes().values()) {
            recipes.put(recipe.getName(), Recipe.copy(recipe));
        }

        Map<String, Category> categories = new LinkedHashMap<>();
        for (Category category : source.getCategories().values()) {
            categories.put(category.getName(), Category.copy(category));
        }
        return new CraftingTable(source.getName(),
                source.getInventoryName(),
                source.getIconItem(),
                InventoryPattern.copy(source.getRecipePattern()),
                InventoryPattern.copy(source.getCatPattern()),
                source.getUseCategories(),
                source.getFillItem(),
                source.getMasteryUnlock(),
                source.getMasteryFee(),
                recipes,
                categories);
    }

    // A method to cleanup 'pseudo' recipes created from the ItemGenerator Module of Divinity.
    // If not cleaned, the gui results in a OOM-Exception.
    public void cleanUpRecipesForEditor() {
        Map<String, Recipe> cleanedRecipes = new LinkedHashMap<>();
        Set<String>         uniqueRecipes  = new HashSet<>();
        for (Recipe recipe : recipes.values()) {
            if (recipe.getName().contains("::")) {
                String recipeName = recipe.getName().split("::")[0];
                if (uniqueRecipes.add(recipeName)) {
                    Recipe copiedRecipe = Recipe.copy(recipe);
                    copiedRecipe.setName(recipeName);
                    cleanedRecipes.put(recipeName, copiedRecipe);
                }
            } else {
                cleanedRecipes.put(recipe.getName(), recipe);
            }
        }
        this.recipes = cleanedRecipes;
    }
}
