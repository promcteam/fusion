package com.gotofinal.darkrise.crafting.gui;

import com.gotofinal.darkrise.crafting.Category;
import com.gotofinal.darkrise.crafting.CraftingTable;
import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import com.gotofinal.darkrise.crafting.LevelFunction;
import com.gotofinal.darkrise.crafting.MasteryManager;
import com.gotofinal.darkrise.crafting.Recipe;
import com.gotofinal.darkrise.crafting.Utils;
import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.spigot.core.utils.cmds.DelayedCommand;
import com.gotofinal.darkrise.spigot.core.utils.cmds.R;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PlayerInitialGUI extends PlayerCustomGUI {
    private final CustomGUI gui;
    private final Map<Integer, Category> slotMap = new HashMap<>();

    private PlayerInitialGUI(CustomGUI gui, Player player, Inventory inventory) {
        super(gui, player, inventory, null);
        this.gui = gui;
    }

    public static PlayerInitialGUI open(CustomGUI gui, Player player)
    {
        InventoryView iv = player.getOpenInventory();
        if ((iv != null) && (iv.getTopInventory() != null))
        {
            gui.map.remove(player);
            player.closeInventory();
        }

        Inventory inv = null;
        try
        {
            inv = Bukkit.createInventory(player, gui.slots.length, ChatColor.translateAlternateColorCodes('&', gui.inventoryName));
            int k = - 1;
            Char2ObjectMap<ItemStack> items = gui.pattern.getItems();
            PlayerInitialGUI playerCustomGUI = new PlayerInitialGUI(gui, player, inv);
            CraftingTable table = Cfg.getTable(gui.name);
            Iterator<Category> categoryIterator = table.getCategories()
                    .values()
                    .stream()
                    .sorted(Comparator.comparingInt(Category::getOrder))
                    .iterator();

            for (String row : gui.pattern.getPattern())
            {
                charLoop: for (char c : row.toCharArray())
                {
                    k++;
                    ItemStack item = items.get(c);
                    if (item != null)
                    {
                        inv.setItem(k, item.clone());
                    }

                    //Slots
                    if (c == 'o' && categoryIterator.hasNext())
                    {
                        List<Recipe> recipes;
                        Category category;
                        do {
                            category = categoryIterator.next();
                            recipes = new ArrayList<>(category.getRecipes());
                            recipes.removeIf(r -> !Utils.hasCraftingPermission(player, r.getName()));
                            recipes.removeIf(r -> r.getNeededLevels() > LevelFunction.getLevel(player) + 5);
                            recipes.removeIf(r -> !MasteryManager.hasMastery(player, gui.name));

                            if(recipes.isEmpty() && !categoryIterator.hasNext()) {
                                continue charLoop;
                            }

                        } while(recipes.isEmpty());

                        inv.setItem(k, category.getIconItem().getItem());
                        playerCustomGUI.slotMap.put(k, category);
                    }
                }
            }

            gui.open(player, playerCustomGUI);
            player.openInventory(inv);
            gui.map.put(player, playerCustomGUI);
            return playerCustomGUI;
        }
        catch (Exception e)
        {
            if (inv != null)
            {
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

        if (category != null)
        {
            e.getWhoClicked().closeInventory();
            PlayerCustomGUI.open(gui, (Player) e.getWhoClicked(), category);
        }

        //Execute commands
        Character c = gui.getPattern().getSlot(e.getRawSlot());
        Collection<DelayedCommand> patternCommands = gui.getPattern().getCommands(c);
        if (patternCommands != null && ! patternCommands.isEmpty())
        {
            DelayedCommand.invoke(DarkRiseCrafting.getInstance(), e.getWhoClicked(), patternCommands,
                    R.r("{crafting}", this.gui.getName()),
                    R.r("{inventoryName}", this.gui.getInventoryName()));
        }
    }
}
