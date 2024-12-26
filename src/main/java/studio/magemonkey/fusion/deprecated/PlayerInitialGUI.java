package studio.magemonkey.fusion.deprecated;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.util.LevelFunction;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

public class PlayerInitialGUI extends PlayerCustomGUI {
    private final CustomGUI              gui;
    private final Map<Integer, Category> slotMap = new HashMap<>();
    private       boolean                isBase  = false;

    private final Category masterCat;

    private PlayerInitialGUI(CustomGUI gui, Player player, Inventory inventory) {
        super(gui, player, inventory, null);
        masterCat = new Category("master");
        this.gui = gui;
    }

    public static PlayerInitialGUI open(CustomGUI gui, Player player) {
        gui.resetPattern();
        gui.map.remove(player);
        player.closeInventory();

        Inventory inv = null;
        try {
            inv = Bukkit.createInventory(player,
                    gui.slots.length,
                    ChatColor.translateAlternateColorCodes('&', gui.inventoryName));
            int                           k               = -1;
            HashMap<Character, ItemStack> items           = gui.pattern.getItems();
            PlayerInitialGUI              playerCustomGUI = new PlayerInitialGUI(gui, player, inv);
            playerCustomGUI.isBase = true;
            CraftingTable table = ProfessionsCfg.getTable(gui.name);
            Iterator<Category> categoryIterator = table.getCategories()
                    .values()
                    .stream()
                    .sorted(Comparator.comparingInt(Category::getOrder))
                    .iterator();

            gui.resetBlockedSlots(player, inv, 0, 0, table.getCategories().size(), 0,
                    new MessageData[]{
                            new MessageData("level", LevelFunction.getLevel(player, ProfessionsCfg.getTable(gui.name))),
                            new MessageData("gui", gui.getName()),
                            new MessageData("player", player.getName()),
                            new MessageData("bal", CodexEngine.get().getVault().getBalance(player))
                    }, table);
            for (String row : gui.pattern.getPattern()) {
                charLoop:
                for (char c : row.toCharArray()) {
                    k++;
//                    ItemStack item = ItemUtils.replaceText(items.get(c),
//                            new MessageData("level", LevelFunction.getLevel(player, Cfg.getTable(gui.name))),
//                            new MessageData("gui", gui.getName()),
//                            new MessageData("player", player.getName()));
//                    if (item != null) inv.setItem(k, item.clone());

                    //Slots
                    if (c == 'o' && categoryIterator.hasNext()) {
                        List<Recipe> recipes;
                        Category     category;
                        do {
                            category = categoryIterator.next();
                            recipes = new ArrayList<>(category.getRecipes());
//                            for (Recipe r : recipes) {
//                                System.out.println("Has permission (" + r.getName() + ")? " +
//                                        Utils.hasCraftingPermission(player, r.getName()) +
//                                        " -- Has levels? " + (r.getNeededLevels() < LevelFunction.getLevel(player, table) + 5));
//                            }
                            recipes.removeIf(r -> !Utils.hasCraftingPermission(player, r.getName()));
//                            recipes.removeIf(r -> r.getNeededLevels() > LevelFunction.getLevel(player, table) + 5);
//                            recipes.removeIf(r -> r.isMastery() && !MasteryManager.hasMastery(player, gui.name));
                            playerCustomGUI.masterCat.getRecipes().addAll(recipes);

                            if (recipes.isEmpty() && !categoryIterator.hasNext()) {
                                continue charLoop;
                            }
                        } while (recipes.isEmpty());

                        inv.setItem(k, category.getDisplayIcon());
                        playerCustomGUI.slotMap.put(k, category);
                    }
                }
            }

            for (int j = 0; j < inv.getSize(); j++) {
                if (inv.getItem(j) != null && inv.getItem(j).getType() != Material.AIR)
                    continue;

                inv.setItem(j, table.getFillItem());
            }

            if (table.getUseCategories()) {
                gui.open(player, playerCustomGUI);
                player.openInventory(inv);
                gui.map.put(player, playerCustomGUI);
            } else {
                player.closeInventory();
                playerCustomGUI.masterCat.hasPrevious(false);
                PlayerCustomGUI.open(gui, player, playerCustomGUI.masterCat);
            }
            return playerCustomGUI;
        } catch (Exception e) {
            if (inv != null) {
                inv.clear();
            }
            player.closeInventory();
            throw new RuntimeException("Exception was thrown on gui open for: " + player.getName(), e);
        }
    }

    @Override
    public void onClick(InventoryClickEvent e) {

        e.setCancelled(true);
        Category category = slotMap.get(e.getSlot());

//        if (this.gui.prevPage != -1 && e.getSlot() == this.gui.prevPage) {
//            BrowseGUI.open((Player) e.getWhoClicked());
//            return;
//        }

        if (category != null) {
            e.getWhoClicked().closeInventory();
            PlayerCustomGUI.open(gui, (Player) e.getWhoClicked(), category);
        }

        //Execute commands
        Character c = gui.getPattern().getSlot(e.getRawSlot());
        gui.executeCommands(c, e.getWhoClicked());

        //Close on click
        if (gui.getPattern().getCloseOnClickSlots().contains(c)) {
            e.getWhoClicked().closeInventory();
        }
    }
}
