package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.migrations.ProfessionMigration;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.util.Utils;

import java.io.File;
import java.util.*;

public class ProfessionsCfg {

    @Getter
    private static final Map<String, CraftingTable>         map    = new HashMap<>(4);
    @Getter
    private static final Map<String, ProfessionGuiRegistry> guiMap = new HashMap<>(4);
    @Getter
    private static final Map<String, FileConfiguration>     cfgs   = new HashMap<>(4);
    @Getter
    private static final Map<String, File>                  files  = new HashMap<>(4);

    public static void init() {
        map.clear();
        guiMap.clear();
        File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
        if (!professionFolder.exists()) {
            professionFolder.mkdirs();
        }
        if (professionFolder.listFiles() == null || Objects.requireNonNull(professionFolder.listFiles()).length == 0) {
            Fusion.getInstance().getLogger().warning("Professions folder is empty. Adding default professions.");
            YamlParser.loadOrExtract(Fusion.getInstance(), "professions/armor_smithing.yml");
            YamlParser.loadOrExtract(Fusion.getInstance(), "professions/weapon_smithing.yml");
        }

        loadProfessions(professionFolder);

        for (Map.Entry<String, CraftingTable> entry : map.entrySet()) {
            String key = entry.getKey();
            guiMap.put(key, new ProfessionGuiRegistry(key));
        }
    }

    public static boolean createNewProfession(String profession, String refProfession) {
        FileConfiguration cfg = new YamlConfiguration();
        try {
            if (refProfession != null && files.containsKey(refProfession)) {
                cfg = YamlConfiguration.loadConfiguration(files.get(refProfession));
                files.put(profession,
                        new File(Fusion.getInstance().getDataFolder() + File.separator + "professions",
                                profession + ".yml"));
                File file = files.get(profession);
                if (!file.exists()) {
                    file.createNewFile();
                }
                cfg.set("name", profession);
                cfg.save(file);
                Map<String, Object> _map = cfg.getValues(true);
                CraftingTable       ct   = new CraftingTable(_map);
                map.put(ct.getName(), ct);
                cfgs.put(profession, cfg);
                files.put(profession, file);
                return true;
            } else if (refProfession == null) {
                files.put(profession,
                        new File(Fusion.getInstance().getDataFolder() + File.separator + "professions",
                                profession + ".yml"));
                File file = files.get(profession);
                if (!file.exists()) {
                    file.createNewFile();
                }
                cfg.load(file);
                cfg.set("name", profession);
                addDefs(cfg);
                cfg.save(file);
                cfg.load(file);
                Map<String, Object> _map = cfg.getValues(true);
                CraftingTable       ct   = new CraftingTable(_map);
                map.put(ct.getName(), ct);
                files.put(profession, file);
                cfgs.put(profession, cfg);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
        }
        return false;
    }

    private static void loadProfessions(File root) {
        for (File file : Objects.requireNonNull(root.listFiles())) {
            if (file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    Bukkit.getConsoleSender().sendMessage("Loading " + file.getName());
                    cfg.load(file);
                    addDefs(cfg);

                    // Perform migrations
                    ProfessionMigration.migrate(cfg);

                    cfg.save(file);
                    cfg.load(file);
                    // Get the YAMLs whole content as a map
                    Map<String, Object> _map = cfg.getValues(true);
                    CraftingTable       ct   = new CraftingTable(_map);
                    map.put(ct.getName(), ct);
                    cfgs.put(ct.getName(), cfg);
                    files.put(ct.getName(), file);
                } catch (Exception e) {
                    e.printStackTrace();
                    Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
                }
            } else if (file.isDirectory()) {
                loadProfessions(file);
            }
        }
    }

    public static CraftingTable getTable(String str) {
        return map.get(str);
    }

    public static ProfessionGuiRegistry getGUI(String str) {
        if (str == null) return null;
        return guiMap.get(str);
    }

    private static void addDefs(FileConfiguration cfg) {
        if (!cfg.isSet("icon")) {
            cfg.set("icon", "STONE");
        }
        if (!cfg.isSet("inventoryName")) {
            cfg.set("inventoryName", "Template Profession");
        }
        if (!cfg.isSet("useCategories")) {
            cfg.set("useCategories", false);
        }
        if (!cfg.isSet("recipePattern.pattern")) {
            List<String> pattern =
                    new ArrayList<>(Arrays.asList("fffffffff", "fooooooof", "fooooooof", "ff-----ff", "f{<f0f>}f"));
            cfg.set("recipePattern.pattern", pattern);
        }
        if (!cfg.isSet("recipePattern.items.}")) {
            cfg.set("recipePattern.items.}.material", "LIME_DYE");
            cfg.set("recipePattern.items.}.amount", 1);
            cfg.set("recipePattern.items.}.durability", 0);
            cfg.set("recipePattern.items.}.unbreakable", false);
            cfg.set("recipePattern.items.}.name", "Next queued items");
            cfg.set("recipePattern.items.}.lore", List.of());
            cfg.set("recipePattern.items.}.flags", List.of());
            cfg.set("recipePattern.items.}.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.>")) {
            cfg.set("recipePattern.items.>.material", "GREEN_DYE");
            cfg.set("recipePattern.items.>.amount", 1);
            cfg.set("recipePattern.items.>.durability", 0);
            cfg.set("recipePattern.items.>.unbreakable", false);
            cfg.set("recipePattern.items.>.name", "Next recipes");
            cfg.set("recipePattern.items.>.lore", List.of());
            cfg.set("recipePattern.items.>.flags", List.of());
            cfg.set("recipePattern.items.>.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.<")) {
            cfg.set("recipePattern.items.<.material", "GREEN_DYE");
            cfg.set("recipePattern.items.<.amount", 1);
            cfg.set("recipePattern.items.<.durability", 0);
            cfg.set("recipePattern.items.<.unbreakable", false);
            cfg.set("recipePattern.items.<.name", "Previous recipes");
            cfg.set("recipePattern.items.<.lore", List.of());
            cfg.set("recipePattern.items.<.flags", List.of());
            cfg.set("recipePattern.items.<.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.{")) {
            cfg.set("recipePattern.items.{.material", "LIME_DYE");
            cfg.set("recipePattern.items.{.amount", 1);
            cfg.set("recipePattern.items.{.durability", 0);
            cfg.set("recipePattern.items.{.unbreakable", false);
            cfg.set("recipePattern.items.{.name", "Previous queued items");
            cfg.set("recipePattern.items.{.lore", List.of());
            cfg.set("recipePattern.items.{.flags", List.of());
            cfg.set("recipePattern.items.{.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.queue-items.-")) {
            cfg.set("recipePattern.items.queue-items.-.material", "GRAY_STAINED_GLASS_PANE");
            cfg.set("recipePattern.items.queue-items.-.amount", 1);
            cfg.set("recipePattern.items.queue-items.-.durability", 0);
            cfg.set("recipePattern.items.queue-items.-.unbreakable", false);
            cfg.set("recipePattern.items.queue-items.-.name", "&cQueue Slot");
            cfg.set("recipePattern.items.queue-items.-.lore", List.of("&7This slot is empty."));
            cfg.set("recipePattern.items.queue-items.-.flags", List.of());
            cfg.set("recipePattern.items.queue-items.-.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.queue-items.Unfinished")) {
            cfg.set("recipePattern.items.queue-items.Unfinished.material", "%material%");
            cfg.set("recipePattern.items.queue-items.Unfinished.amount", 1);
            cfg.set("recipePattern.items.queue-items.Unfinished.durability", 0);
            cfg.set("recipePattern.items.queue-items.Unfinished.unbreakable", false);
            cfg.set("recipePattern.items.queue-items.Unfinished.name", "&cQueue Slot");
            cfg.set("recipePattern.items.queue-items.Unfinished.lore",
                    List.of("&7&oThis item is in the crafting queue",
                            " ",
                            "&7Time left: &c%time%",
                            " ",
                            "&eClick to cancel"));
            cfg.set("recipePattern.items.queue-items.Unfinished.flags", List.of());
            cfg.set("recipePattern.items.queue-items.Unfinished.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.queue-items.Finished")) {
            cfg.set("recipePattern.items.queue-items.Finished.material", "%material%");
            cfg.set("recipePattern.items.queue-items.Finished.amount", 1);
            cfg.set("recipePattern.items.queue-items.Finished.durability", 0);
            cfg.set("recipePattern.items.queue-items.Finished.unbreakable", false);
            cfg.set("recipePattern.items.queue-items.Finished.name", "&cQueue Slot");
            cfg.set("recipePattern.items.queue-items.Finished.lore",
                    List.of("&7&oThis item is in the crafting queue",
                            " ",
                            "&7The item is &afinished&7!",
                            " ",
                            "&eClick to obtain"));
            cfg.set("recipePattern.items.queue-items.Finished.flags", List.of());
            cfg.set("recipePattern.items.queue-items.Finished.enchants", Map.of());
        }
        if (!cfg.isSet("recipePattern.items.fillItem")) {
            cfg.set("recipePattern.items.fillItem.material", "BLACK_STAINED_GLASS_PANE");
            cfg.set("recipePattern.items.fillItem.amount", 1);
            cfg.set("recipePattern.items.fillItem.durability", 0);
            cfg.set("recipePattern.items.fillItem.unbreakable", false);
            cfg.set("recipePattern.items.fillItem.name", " ");
            cfg.set("recipePattern.items.fillItem.lore", List.of());
            cfg.set("recipePattern.items.fillItem.flags", List.of());
            cfg.set("recipePattern.items.fillItem.enchants", Map.of());
        }

        // TODO correct serialization for catPattern without queue-items
        if (cfg.isSet("categoryPattern")) {
            if (!cfg.isSet("categoryPattern.items.queue-items.-")) {
                cfg.set("categoryPattern.items.queue-items.-",
                        cfg.getConfigurationSection("pattern.items.queue-items.-").getValues(false));
            }
            if (!cfg.isSet("categoryPattern.items.queue-items.Unfinished")) {
                cfg.set("categoryPattern.items.queue-items.Unfinished",
                        cfg.getConfigurationSection("pattern.items.queue-items.Unfinished").getValues(false));
            }
            if (!cfg.isSet("categoryPattern.items.queue-items.Finished")) {
                cfg.set("categoryPattern.items.queue-items.Finished",
                        cfg.getConfigurationSection("pattern.items.queue-items.Finished").getValues(false));
            }
        }

        if (!cfg.isSet("categories")) {
            cfg.set("categories", List.of(new Category("First Category").serialize()));
        }
        if (cfg.isSet("recipes")) {
            // Retrieve the list of recipes
            List<Map<?, ?>> recipesSection = cfg.getMapList("recipes");

            // Ensure recipesSection is not null
            if (recipesSection != null) {
                // Create a new list to hold the modified recipes
                List<Map<String, Object>> modifiedRecipes = new ArrayList<>();

                // Iterate and modify recipes
                for (Map<?, ?> recipeData : recipesSection) {
                    try {
                        // Create a mutable copy of the recipe data
                        Map<String, Object> mutableRecipeData = (Map<String, Object>) new LinkedHashMap<>(recipeData);

                        Map<String, Object> costs =
                                (Map<String, Object>) mutableRecipeData.getOrDefault("costs", new HashMap<>());
                        Map<String, Object> results =
                                (Map<String, Object>) mutableRecipeData.getOrDefault("results", new HashMap<>());
                        Map<String, Object> conditions =
                                (Map<String, Object>) mutableRecipeData.getOrDefault("conditions", new HashMap<>());


                        if (mutableRecipeData.containsKey("cooldown")) {
                            int craftingTime = (int) mutableRecipeData.getOrDefault("cooldown", 0);
                            mutableRecipeData.remove("cooldown");
                            mutableRecipeData.put("craftingTime", craftingTime);
                        }

                        // Costs
                        if (mutableRecipeData.containsKey("price")) {
                            costs.put("money", (mutableRecipeData.get("price") instanceof Number)
                                    ? ((Number) mutableRecipeData.get("price")).doubleValue() : 0.0);
                            mutableRecipeData.remove("price");
                        }
                        if (mutableRecipeData.containsKey("costs")
                                && mutableRecipeData.get("costs") instanceof Number) {
                            costs.put("exp", ((mutableRecipeData.get("costs") instanceof Number)
                                    ? ((Number) mutableRecipeData.get("costs")).doubleValue() : 0.0));
                        }
                        if (mutableRecipeData.containsKey("pattern")) {
                            List<String> pattern = (List<String>) mutableRecipeData.get("pattern");
                            mutableRecipeData.remove("pattern");
                            costs.put("items", pattern);
                        }

                        // Conditions
                        if (mutableRecipeData.containsKey("mastery")) {
                            boolean mastery = (boolean) mutableRecipeData.getOrDefault("mastery", false);
                            mutableRecipeData.remove("mastery");
                            conditions.put("mastery", mastery);
                        }
                        if (mutableRecipeData.containsKey("neededLevels")) {
                            int neededLevels = (int) mutableRecipeData.getOrDefault("neededLevels", 0);
                            mutableRecipeData.remove("neededLevels");
                            conditions.put("professionLevel", neededLevels);
                        }
                        if (mutableRecipeData.containsKey("rank")) {
                            String rank = (String) mutableRecipeData.getOrDefault("rank", null);
                            mutableRecipeData.remove("rank");
                            conditions.put("rank", rank);
                        }

                        // Results
                        if (mutableRecipeData.containsKey("result")) {
                            String item = mutableRecipeData.get("result").toString();
                            mutableRecipeData.remove("result");
                            results.put("item", item);
                        }
                        if (mutableRecipeData.containsKey("xpGain")) {
                            results.put("professionExp", ((mutableRecipeData.get("xpGain") instanceof Number)
                                    ? ((Number) mutableRecipeData.get("xpGain")).intValue() : 0));
                            mutableRecipeData.remove("xpGain");
                        }
                        if (mutableRecipeData.containsKey("commands")) {
                            List<String> commands = (List<String>) mutableRecipeData.get("commands");
                            mutableRecipeData.remove("commands");
                            results.put("commands", commands);
                        }


                        mutableRecipeData.put("results", results);
                        mutableRecipeData.put("conditions", conditions);
                        mutableRecipeData.put("costs", costs);
                        // Add the modified recipe to the new list
                        modifiedRecipes.add(mutableRecipeData);
                    } catch (Exception e) {
                        Fusion.getInstance()
                                .error("Exception when reading config, Invalid entry in config of " + recipeData
                                        + " crafting table. Value: " + recipeData);
                        e.printStackTrace();
                    }
                }

                // Set the modified recipes back to the config
                cfg.set("recipes", modifiedRecipes);
            } else {
                Fusion.getInstance().error("Recipes section is null in the config.");
            }
        } else {
            cfg.set("recipes", new ArrayList<>());
        }
    }

    public static boolean loadFrom(String key, Map<String, Object> map) {
        try {
            File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
            if (!professionFolder.exists()) {
                professionFolder.mkdirs();
            }

            File file = new File(professionFolder, key + ".yml");
            if (file.exists()) {
                Fusion.getInstance().getLogger().warning("Profession '" + key + "' was already migrated.");
                return true;
            }
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("name", key);
            cfg.set("icon", map.get("icon"));
            cfg.set("inventoryName", map.get("inventoryName"));
            cfg.set("useCategories", map.get("useCategories"));
            cfg.set("pattern", map.get("pattern"));
            cfg.set("categoryPattern", map.get("categoryPattern"));
            cfg.set("masteryUnlock", map.get("masteryUnlock"));
            cfg.set("masteryFee", map.get("masteryFee"));
            cfg.set("categories", map.get("categories"));
            cfg.set("recipes", map.get("recipes"));
            cfg.save(file);
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' migrated successfully.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
            return false;
        }
    }

    public static ItemStack getFillItem(String key) {
        String       name = cfgs.get(key).getString("pattern.items.fillItem.name", " ");
        List<String> lore = cfgs.get(key).getStringList("pattern.items.fillItem.lore");
        Material material = Material.getMaterial(cfgs.get(key)
                .getString("pattern.items.fillItem.material", "BLACK_STAINED_GLASS_PANE"));
        return ItemBuilder.newItem(material).name(name).lore(lore).build();
    }

    public static ItemStack getQueueSlot(String key) {
        FileConfiguration cfg = cfgs.get(key);
        if (!cfg.isSet("recipePattern.items.queue-items.-")) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("Profession '" + key + "' does not have a queue slot. Using default.");
            return ItemBuilder.newItem(Material.GRAY_STAINED_GLASS_PANE)
                    .name("&cQueue Slot")
                    .lore(List.of("&7This slot is empty."))
                    .build();
        }
        Material material =
                Material.getMaterial(cfg.getString("recipePattern.items.queue-items.-.material", "GRAY_STAINED_GLASS_PANE"));
        return ItemBuilder.newItem(material)
                .name(cfg.getString("recipePattern.items.queue-items.-.name", "&cQueue Slot"))
                .lore(cfg.getStringList("recipePattern.items.queue-items.-.lore"))
                .build();
    }

    public static ItemStack getQueueItem(String key, QueueItem item) {
        /* Fetch stored data to the queued item */
        //System.out.println("Fetching queued item for " + key + " with item " + item.getRecipe().getResult().getItemStack().getType());
        FileConfiguration cfg = cfgs.get(key);
        String path =
                item.isDone() ? "recipePattern.items.queue-items.Finished" : "recipePattern.items.queue-items.Unfinished";


        if (!cfg.isSet(path)) {
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' is missing '" + path + "'.");
            return null;
        }
        ItemStack result = item.getRecipe().getDivinityRecipeMeta() == null ? item.getRecipe()
                .getResults()
                .getResultItem()
                .getItemStack() : item.getRecipe().getDivinityRecipeMeta().getIcon();
        Material material = Material.getMaterial(cfg.getString(path + ".material", "STONE")
                .replace("%material%", result.getType().toString())
                .toUpperCase());
        List<String> lore = cfg.getStringList(path + ".lore");
        lore.replaceAll(s -> s.replace("%time%", Utils.getFormattedTime(item.getVisualRemainingItemTime())));
        return ItemBuilder.newItem(result).material(material).lore(lore).build();
    }

    public static void closeAll() {
        for (ProfessionGuiRegistry gui : guiMap.values()) {
            gui.closeAll();
        }
    }
}
