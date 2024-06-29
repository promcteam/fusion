package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.items.providers.VanillaProvider;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.legacy.item.ItemColors;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.gui.CustomGUI;
import studio.magemonkey.fusion.queue.QueueItem;

import java.io.File;
import java.util.*;

public class ProfessionsCfg {

    @Getter
    private static final Map<String, CraftingTable> map = new HashMap<>(4);
    @Getter
    private static final Map<String, CustomGUI> guiMap = new HashMap<>(4);
    @Getter
    private static final Map<String, FileConfiguration> cfgs = new HashMap<>(4);

    public static void init() {
        map.clear();
        guiMap.clear();
        File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
        if (!professionFolder.exists()) {
            professionFolder.mkdirs();
        }
        if (professionFolder.listFiles() == null) {
            Fusion.getInstance().getLogger().warning("There are no professions registered to load.");
            Fusion.getInstance().getLogger().warning("Initializing default profession 'craft'");
            HashMap<Character, ItemStack> items = new HashMap<>();
            items.put('0', ItemBuilder.newItem(Material.STONE).durability(ItemColors.BLACK).build());
            items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
            items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
            if (Cfg.craftingQueue) {
                items.put('-', ItemBuilder.newItem(Material.PAPER).name("%name%").lore(List.of("&7&oThis item is in the crafting queue", " ", " &7Time left: &c%time%", " ", "&eClick to cancel")).build());
                items.put('}', ItemBuilder.newItem(Material.BOOK).name("Next queued items").build());
                items.put('{', ItemBuilder.newItem(Material.BOOK).name("Previous queued items").build());
            }
            InventoryPattern ip =
                    new InventoryPattern(new String[]{"=========", "=========", "=========", "=========", Cfg.craftingQueue ? "{-------}" : "=========", "<0000000>"},
                            items);
            ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            CraftingTable b = new CraftingTable("craft",
                    "Craft inventory name",
                    new VanillaProvider.VanillaItemType(Material.PAPER),
                    ip,
                    item,
                    0,
                    0);
            List<Map<String, Object>> list = new ArrayList<>(3);
            list.add(b.serialize());
            loadFrom("craft", b.serialize());
            return;
        }
        for (File file : Objects.requireNonNull(professionFolder.listFiles())) {
            if (file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    cfg.load(file);
                    addCraftingQueueDefs(cfg);
                    cfg.save(file);
                    cfg.load(file);
                    // Get the YAMLs whole content as a map
                    Map<String, Object> _map = cfg.getValues(true);
                    CraftingTable ct = new CraftingTable(_map);
                    map.put(ct.getName(), ct);
                    cfgs.put(ct.getName(), cfg);
                } catch (Exception e) {
                    e.printStackTrace();
                    Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
                }
            }
        }

        for (Map.Entry<String, CraftingTable> entry : map.entrySet()) {
            String key = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }

    public static CraftingTable getTable(String str) {
        return map.get(str.toLowerCase().trim());
    }

    public static CustomGUI getGUI(String str) {
        if (str == null) return null;
        return guiMap.get(str.toLowerCase().trim());
    }

    private static void addCraftingQueueDefs(FileConfiguration cfg) {
        if (!cfg.isSet("queueSlot")) {
            cfg.set("queueSlot.material", "GRAY_STAINED_GLASS_PANE");
            cfg.set("queueSlot.name", "&cQueue Slot");
            cfg.set("queueSlot.lore", List.of("&7This slot is empty."));
        }
        if (!cfg.isSet("pattern.items.}")) {
            cfg.set("pattern.items.}.material", "BOOK");
            cfg.set("pattern.items.}.amount", 1);
            cfg.set("pattern.items.}.durability", 0);
            cfg.set("pattern.items.}.unbreakable", false);
            cfg.set("pattern.items.}.name", "Next queued items");
            cfg.set("pattern.items.}.lore", List.of());
            cfg.set("pattern.items.}.flags", List.of());
            cfg.set("pattern.items.}.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.{")) {
            cfg.set("pattern.items.{.material", "BOOK");
            cfg.set("pattern.items.{.amount", 1);
            cfg.set("pattern.items.{.durability", 0);
            cfg.set("pattern.items.{.unbreakable", false);
            cfg.set("pattern.items.{.name", "Previous queued items");
            cfg.set("pattern.items.{.lore", List.of());
            cfg.set("pattern.items.{.flags", List.of());
            cfg.set("pattern.items.{.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.-")) {
            cfg.set("pattern.items.-.material", "PAPER");
            cfg.set("pattern.items.-.amount", 1);
            cfg.set("pattern.items.-.durability", 0);
            cfg.set("pattern.items.-.unbreakable", false);
            cfg.set("pattern.items.-.name", "%name%");
            cfg.set("pattern.items.-.lore", List.of("&7&oThis item is in the crafting queue", " ", " &7Time left: &c%time%", " ", "&eClick to cancel"));
            cfg.set("pattern.items.-.flags", List.of());
            cfg.set("pattern.items.-.enchants", Map.of());
        }
    }

    public static ItemStack getQueueSlot(String key) {
        FileConfiguration cfg = cfgs.get(key);
        if (!cfg.isSet("queueSlot")) {
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' does not have a queue item. Using default.");
            return ItemBuilder.newItem(Material.GRAY_STAINED_GLASS_PANE).name("&cQueue Slot").lore(List.of("&7This slot is empty.")).build();
        }
        Material material = Material.getMaterial(cfg.getString("queueSlot.material", "GRAY_STAINED_GLASS_PANE"));
        return ItemBuilder.newItem(material)
                .name(cfg.getString("queueSlot.name", "&cQueue Slot"))
                .lore(cfg.getStringList("queueSlot.lore"))
                .build();
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
            cfg.set("masteryUnlock", map.get("masteryUnlock"));
            cfg.set("masteryFee", map.get("masteryFee"));
            cfg.set("fillItem", map.get("fillItem"));
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

    public static ItemStack getQueueItem(String key, QueueItem item) {
        /* Fetch stored data to the queued item */
        System.out.println("Fetching queued item for " + key + " with item " + item.getRecipe().getResult().getItemStack().getType());
        FileConfiguration cfg = cfgs.get(key);


        if (!cfg.isSet("pattern.items.-")) {
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' does not have a queue item.");
            return null;
        }
        ItemStack result = item.getRecipe().getResult().getItemStack();
        Material material = Material.getMaterial(cfg.getString("pattern.items.-.material", "STONE").replace("%material%", result.getType().toString()).toUpperCase());
        List<String> lore = cfg.getStringList("pattern.items.-.lore");
        lore.replaceAll(s -> s.replace("%time%", String.valueOf(item.getDifference())));
        return ItemBuilder.newItem(result).lore(lore).build();
    }
}
